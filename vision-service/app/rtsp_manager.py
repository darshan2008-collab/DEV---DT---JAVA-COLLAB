"""
RTSP IP Camera Stream Manager
Manages persistent background threads that read RTSP streams, run YOLO + ByteTrack,
and publish detection telemetry to the Spring Boot backend.
"""

import threading
import time
import logging
import os
import cv2
import numpy as np
import requests
from datetime import datetime, timezone
from typing import Dict, Any, Optional

logger = logging.getLogger("rtsp-manager")

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080/api")
METERS_PER_PIXEL_DEFAULT = float(os.getenv("METERS_PER_PIXEL", "0.05"))
COCO_VEHICLE_CLASSES = {2: "car", 3: "motorcycle", 5: "bus", 7: "truck"}


class RTSPStreamState:
    """Holds live state for one registered RTSP camera stream."""

    def __init__(self, stream_id: str, rtsp_url: str, segment_id: str,
                 fps_sample: int = 2, label: str = "", enabled: bool = True):
        self.stream_id = stream_id
        self.rtsp_url = rtsp_url
        self.segment_id = segment_id
        self.fps_sample = max(1, fps_sample)
        self.label = label
        self.enabled = enabled

        # Latest annotated frame (numpy array) for MJPEG serving
        self.latest_frame: Optional[np.ndarray] = None
        self.latest_frame_lock = threading.Lock()

        # Latest metrics
        self.metrics: Dict[str, Any] = {
            "stream_id": stream_id,
            "segment_id": segment_id,
            "label": label,
            "status": "STARTING",
            "vehicle_count": 0,
            "density_veh_per_km": 0.0,
            "speed_kmph": 0.0,
            "queue_m": 0.0,
            "confidence": 0.0,
            "last_updated": None,
            "frames_processed": 0,
            "reconnect_count": 0,
            "error": None,
        }
        self.metrics_lock = threading.Lock()

        self._thread: Optional[threading.Thread] = None
        self._stop_event = threading.Event()

    def start(self, model, model_loaded: bool):
        """Start the background RTSP reader thread."""
        self._stop_event.clear()
        self._thread = threading.Thread(
            target=self._worker,
            args=(model, model_loaded),
            daemon=True,
            name=f"rtsp-{self.stream_id}"
        )
        self._thread.start()
        logger.info("RTSP worker started for stream %s → %s", self.stream_id, self.rtsp_url)

    def stop(self):
        """Signal the worker to stop and wait for it."""
        self.enabled = False
        self._stop_event.set()
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=10)
        logger.info("RTSP worker stopped for stream %s", self.stream_id)

    def get_latest_jpeg(self) -> Optional[bytes]:
        """Return the latest annotated frame as JPEG bytes, or None."""
        with self.latest_frame_lock:
            if self.latest_frame is None:
                return None
            _, jpeg = cv2.imencode(".jpg", self.latest_frame,
                                   [cv2.IMWRITE_JPEG_QUALITY, 70])
            return jpeg.tobytes()

    def get_metrics(self) -> Dict[str, Any]:
        with self.metrics_lock:
            return dict(self.metrics)

    def _update_metrics(self, **kwargs):
        with self.metrics_lock:
            self.metrics.update(kwargs)
            self.metrics["last_updated"] = datetime.now(timezone.utc).isoformat()

    def _worker(self, model, model_loaded: bool):
        """Main RTSP reading + detection loop."""
        reconnect_count = 0
        frame_idx = 0

        while not self._stop_event.is_set() and self.enabled:
            cap = None
            try:
                logger.info("[%s] Connecting to RTSP: %s", self.stream_id, self.rtsp_url)
                cap = cv2.VideoCapture(self.rtsp_url)
                cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)  # Minimize latency

                if not cap.isOpened():
                    raise ConnectionError(f"Cannot open RTSP stream: {self.rtsp_url}")

                self._update_metrics(status="STREAMING", error=None,
                                     reconnect_count=reconnect_count)
                consecutive_failures = 0

                while not self._stop_event.is_set() and self.enabled:
                    ret, frame = cap.read()
                    if not ret:
                        consecutive_failures += 1
                        if consecutive_failures > 30:
                            logger.warning("[%s] Too many read failures, reconnecting.", self.stream_id)
                            break
                        time.sleep(0.1)
                        continue

                    consecutive_failures = 0
                    frame_idx += 1

                    # Only process every Nth frame to control load
                    if frame_idx % self.fps_sample != 0:
                        # Still update latest_frame for smooth MJPEG
                        with self.latest_frame_lock:
                            self.latest_frame = frame.copy()
                        continue

                    annotated_frame, metrics = self._run_detection(
                        frame, model, model_loaded, frame_idx
                    )

                    with self.latest_frame_lock:
                        self.latest_frame = annotated_frame

                    self._update_metrics(
                        frames_processed=frame_idx,
                        **metrics
                    )

                    # Post observation to Java backend every 5 processed frames
                    if frame_idx % (self.fps_sample * 5) == 0:
                        self._post_observation(metrics)

            except Exception as e:
                logger.error("[%s] RTSP worker error: %s", self.stream_id, e)
                self._update_metrics(status="RECONNECTING", error=str(e))
                reconnect_count += 1
            finally:
                if cap:
                    cap.release()

            if not self._stop_event.is_set() and self.enabled:
                # Back-off reconnect: 5s initially, max 30s
                wait = min(5 * reconnect_count, 30)
                logger.info("[%s] Reconnecting in %ds (attempt %d)...",
                            self.stream_id, wait, reconnect_count)
                self._stop_event.wait(timeout=wait)

        self._update_metrics(status="STOPPED")
        logger.info("[%s] RTSP worker exited.", self.stream_id)

    def _run_detection(self, frame: np.ndarray, model, model_loaded: bool,
                       frame_idx: int) -> tuple:
        """Run YOLO detection on a single frame and return annotated frame + metrics."""
        h, w = frame.shape[:2]
        annotated = frame.copy()
        class_counts = {"car": 0, "bus": 0, "truck": 0, "motorcycle": 0}
        total_vehicles = 0

        if model_loaded and model is not None:
            try:
                results = model.track(annotated, persist=True,
                                      tracker="bytetrack.yaml", verbose=False)
                for r in results:
                    if r.boxes is None:
                        continue
                    boxes = r.boxes
                    ids = r.boxes.id.int().cpu().tolist() if r.boxes.id is not None else []
                    for i, box in enumerate(boxes):
                        cls_id = int(box.cls[0].item())
                        if cls_id not in COCO_VEHICLE_CLASSES:
                            continue
                        cls_name = COCO_VEHICLE_CLASSES[cls_id]
                        conf = float(box.conf[0].item())
                        if conf < 0.4:
                            continue
                        class_counts[cls_name] += 1
                        total_vehicles += 1
                        xyxy = box.xyxy[0].tolist()
                        track_id = ids[i] if i < len(ids) else "?"
                        # Draw bounding box
                        color = {"car": (0, 200, 50), "bus": (255, 140, 0),
                                 "truck": (200, 0, 50), "motorcycle": (50, 50, 255)}[cls_name]
                        cv2.rectangle(annotated,
                                      (int(xyxy[0]), int(xyxy[1])),
                                      (int(xyxy[2]), int(xyxy[3])), color, 2)
                        label = f"{cls_name}#{track_id} {conf:.2f}"
                        cv2.putText(annotated, label,
                                    (int(xyxy[0]), int(xyxy[1]) - 5),
                                    cv2.FONT_HERSHEY_SIMPLEX, 0.45, color, 1)
            except Exception as e:
                logger.warning("[%s] Detection error on frame %d: %s",
                               self.stream_id, frame_idx, e)
        else:
            # Simulation/replay mode
            total_vehicles = 8 + (frame_idx % 5)
            class_counts = {"car": int(total_vehicles * 0.6), "bus": 1,
                            "truck": 1, "motorcycle": max(0, total_vehicles - int(total_vehicles * 0.6) - 2)}
            cv2.putText(annotated, "[RTSP SIMULATION MODE]", (20, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)

        # Draw overlay: stream ID, vehicle count, timestamp
        overlay = f"[{self.stream_id}] Seg:{self.segment_id} | Vehicles:{total_vehicles} | Frame:{frame_idx}"
        cv2.putText(annotated, overlay, (10, h - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1, cv2.LINE_AA)
        cv2.putText(annotated, datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    (w - 220, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1)

        # Compute metrics
        road_len_km = 4.0
        density = round(total_vehicles / road_len_km, 2)
        # Rough speed estimate: inverse of density, clamped 10–80 km/h
        speed = max(10.0, min(80.0, round(60.0 / max(density, 0.5), 1)))
        queue = round(total_vehicles * 7.5, 1) if total_vehicles > 8 else 0.0
        confidence = 0.88 if model_loaded else 0.70

        return annotated, {
            "vehicle_count": total_vehicles,
            "class_counts": class_counts,
            "density_veh_per_km": density,
            "speed_kmph": speed,
            "queue_m": queue,
            "confidence": confidence,
            "status": "STREAMING",
        }

    def _post_observation(self, metrics: Dict[str, Any]):
        """POST detection telemetry to Java backend /api/observations."""
        payload = {
            "segment_id": self.segment_id,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "vehicle_count": metrics.get("vehicle_count", 0),
            "density_veh_per_km": metrics.get("density_veh_per_km", 0.0),
            "speed_kmph": metrics.get("speed_kmph", 0.0),
            "queue_m": metrics.get("queue_m", 0.0),
            "confidence": metrics.get("confidence", 0.7),
            "data_source": "CAMERA_LIVE_RTSP",
        }
        try:
            r = requests.post(f"{BACKEND_URL}/observations", json=payload, timeout=4)
            if r.status_code not in (200, 201):
                logger.debug("[%s] Backend observation POST returned %d",
                             self.stream_id, r.status_code)
        except Exception as e:
            logger.debug("[%s] Could not POST observation: %s", self.stream_id, e)


class RTSPManager:
    """Singleton registry of all active RTSP stream workers."""

    def __init__(self):
        self._streams: Dict[str, RTSPStreamState] = {}
        self._lock = threading.Lock()
        self._model = None
        self._model_loaded = False

    def set_model(self, model, model_loaded: bool):
        """Inject the shared YOLO model reference."""
        self._model = model
        self._model_loaded = model_loaded

    def register(self, stream_id: str, rtsp_url: str, segment_id: str,
                 fps_sample: int = 2, label: str = "", enabled: bool = True) -> RTSPStreamState:
        """Register and immediately start an RTSP stream worker."""
        with self._lock:
            if stream_id in self._streams:
                # Stop old worker if re-registering
                self._streams[stream_id].stop()
            state = RTSPStreamState(
                stream_id=stream_id,
                rtsp_url=rtsp_url,
                segment_id=segment_id,
                fps_sample=fps_sample,
                label=label,
                enabled=enabled,
            )
            self._streams[stream_id] = state
        state.start(self._model, self._model_loaded)
        logger.info("Registered RTSP stream: %s", stream_id)
        return state

    def unregister(self, stream_id: str) -> bool:
        """Stop and remove a stream."""
        with self._lock:
            if stream_id not in self._streams:
                return False
            state = self._streams.pop(stream_id)
        state.stop()
        return True

    def get(self, stream_id: str) -> Optional[RTSPStreamState]:
        with self._lock:
            return self._streams.get(stream_id)

    def list_all(self) -> list:
        with self._lock:
            return [s.get_metrics() for s in self._streams.values()]

    def stop_all(self):
        with self._lock:
            states = list(self._streams.values())
        for s in states:
            s.stop()
        with self._lock:
            self._streams.clear()


# Module-level singleton
rtsp_manager = RTSPManager()
