"""
API Client Helper for Streamlit Dashboards
Connects to Spring Boot Backend and FastAPI Vision Service
"""

import os
import requests
import streamlit as st

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080/api")
VISION_URL  = os.getenv("VISION_URL",  "http://localhost:8000")


@st.cache_data(ttl=2)
def get_backend_health():
    try:
        r = requests.get(f"{BACKEND_URL}/health", timeout=0.8)
        return r.json() if r.status_code == 200 else None
    except Exception:
        return None


@st.cache_data(ttl=2)
def get_traffic_current():
    try:
        r = requests.get(f"{BACKEND_URL}/traffic/current", timeout=1.0)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


@st.cache_data(ttl=15)
def get_roads():
    try:
        r = requests.get(f"{BACKEND_URL}/roads", timeout=1.0)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


@st.cache_data(ttl=5)
def get_ambulances():
    try:
        r = requests.get(f"{BACKEND_URL}/ambulances", timeout=1.0)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


@st.cache_data(ttl=15)
def get_hospitals():
    try:
        r = requests.get(f"{BACKEND_URL}/hospitals", timeout=1.0)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


@st.cache_data(ttl=2)
def get_trips():
    try:
        r = requests.get(f"{BACKEND_URL}/trips", timeout=1.0)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def get_trip(trip_id: str):
    try:
        r = requests.get(f"{BACKEND_URL}/trips/{trip_id}", timeout=1.0)
        return r.json() if r.status_code == 200 else None
    except Exception:
        return None


def create_trip(ambulance_id: str, hospital_id: str, origin: str = "S", destination: str = "H"):
    payload = {
        "ambulance_id": ambulance_id,
        "hospital_id": hospital_id,
        "origin": origin,
        "destination": destination
    }
    r = requests.post(f"{BACKEND_URL}/trips", json=payload, timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


def trip_action(trip_id: str, action: str):
    r = requests.post(f"{BACKEND_URL}/trips/{trip_id}/{action}", timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


def get_trip_recommendations(trip_id: str):
    try:
        r = requests.get(f"{BACKEND_URL}/trips/{trip_id}/recommendations", timeout=1.0)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def accept_recommendation(trip_id: str, recommendation_id: str):
    r = requests.post(f"{BACKEND_URL}/trips/{trip_id}/recommendations/{recommendation_id}/accept", timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


def reject_recommendation(trip_id: str, recommendation_id: str):
    r = requests.post(f"{BACKEND_URL}/trips/{trip_id}/recommendations/{recommendation_id}/reject", timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


def get_trip_route_updates(trip_id: str):
    try:
        r = requests.get(f"{BACKEND_URL}/trips/{trip_id}/route-updates", timeout=1.0)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def calculate_routes(origin: str, destination: str, algorithm: str = "DIJKSTRA", include_signal_delay: bool = False):
    payload = {
        "origin": origin,
        "destination": destination,
        "algorithm": algorithm,
        "include_signal_delay": include_signal_delay
    }
    r = requests.post(f"{BACKEND_URL}/routes/calculate", json=payload, timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


def set_simulation_scenario(scenario: str):
    r = requests.post(f"{BACKEND_URL}/simulation/scenario", json={"scenario": scenario}, timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


def simulation_control(action: str):
    r = requests.post(f"{BACKEND_URL}/simulation/{action}", timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


@st.cache_data(ttl=3)
def get_simulation_status():
    try:
        r = requests.get(f"{BACKEND_URL}/simulation/status", timeout=0.8)
        return r.json() if r.status_code == 200 else {"running": True, "active_scenario": "NORMAL"}
    except Exception:
        return {"running": True, "active_scenario": "NORMAL"}


def request_signal_priority(junction_id: str, direction: str = "NORTH_SOUTH", ambulance_id: str = "AMB-01", trip_id: str = ""):
    payload = {
        "direction": direction,
        "ambulance_id": ambulance_id,
        "trip_id": trip_id
    }
    r = requests.post(f"{BACKEND_URL}/signals/{junction_id}/priority", json=payload, timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


def get_signal_state(junction_id: str):
    try:
        r = requests.get(f"{BACKEND_URL}/signals/{junction_id}/state", timeout=0.8)
        return r.json() if r.status_code == 200 else {"state": "NORMAL_CYCLE", "active_direction": "EAST_WEST"}
    except Exception:
        return {"state": "NORMAL_CYCLE", "active_direction": "EAST_WEST"}


def signal_override(junction_id: str):
    r = requests.post(f"{BACKEND_URL}/signals/{junction_id}/override", timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


@st.cache_data(ttl=10)
def get_evaluation_summary():
    try:
        r = requests.get(f"{BACKEND_URL}/evaluation/summary", timeout=1.0)
        return r.json() if r.status_code == 200 else {}
    except Exception:
        return {}


@st.cache_data(ttl=2)
def get_live_events(limit: int = 20):
    try:
        r = requests.get(f"{BACKEND_URL}/traffic/events?limit={limit}", timeout=0.8)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


@st.cache_data(ttl=2)
def get_vision_health():
    try:
        r = requests.get(f"{VISION_URL}/health", timeout=0.8)
        return r.json() if r.status_code == 200 else None
    except Exception:
        return None


# ─── NEW: RTSP Camera API Functions ───────────────────────────────────────────

def get_cameras():
    """Fetch all registered cameras from Java backend."""
    try:
        r = requests.get(f"{BACKEND_URL}/cameras", timeout=1.0)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def register_camera_backend(stream_id: str, rtsp_url: str, segment_id: str,
                             label: str = "", fps_sample: int = 2,
                             location_lat: float = None, location_lng: float = None):
    """Register a new camera in the Java backend camera registry."""
    payload = {
        "streamId": stream_id,
        "rtspUrl": rtsp_url,
        "segmentId": segment_id,
        "label": label,
        "fpsSample": fps_sample,
        "enabled": True,
    }
    if location_lat is not None:
        payload["locationLat"] = location_lat
    if location_lng is not None:
        payload["locationLng"] = location_lng
    try:
        r = requests.post(f"{BACKEND_URL}/cameras", json=payload, timeout=5)
        return r.json()
    except Exception as e:
        return {"error": str(e)}


def register_camera_vision(stream_id: str, rtsp_url: str, segment_id: str,
                            label: str = "", fps_sample: int = 2):
    """Register and start an RTSP stream worker in the vision-service."""
    payload = {
        "stream_id": stream_id,
        "rtsp_url": rtsp_url,
        "segment_id": segment_id,
        "label": label,
        "fps_sample": fps_sample,
        "enabled": True,
    }
    try:
        r = requests.post(f"{VISION_URL}/streams/register", json=payload, timeout=5)
        return r.json()
    except Exception as e:
        return {"error": str(e)}


def test_camera_vision(stream_id: str, rtsp_url: str, segment_id: str,
                       label: str = "", fps_sample: int = 2):
    """Probe an IP camera and require one readable frame before registration."""
    payload = {
        "stream_id": stream_id,
        "rtsp_url": rtsp_url,
        "segment_id": segment_id,
        "label": label,
        "fps_sample": fps_sample,
        "enabled": True,
    }
    try:
        r = requests.post(f"{VISION_URL}/streams/test", json=payload, timeout=12)
        return r.json() if r.content else {"status_code": r.status_code}
    except Exception as e:
        return {"error": str(e)}


def connect_camera(stream_id: str, rtsp_url: str, segment_id: str,
                   label: str = "", fps_sample: int = 2,
                   location_lat: float = None, location_lng: float = None,
                   force_register: bool = False):
    """Verify, persist, and start one IP camera as a single dashboard action."""
    if not force_register:
        probe = test_camera_vision(stream_id, rtsp_url, segment_id, label, fps_sample)
        if probe.get("status") != "READY":
            # Return probe error with flag indicating it can be queued
            return {
                "error": probe.get("detail", probe.get("error", "Camera connection failed.")),
                "can_retry": True
            }

    backend = register_camera_backend(
        stream_id, rtsp_url, segment_id, label, fps_sample, location_lat, location_lng
    )
    if "error" in backend:
        return backend

    vision = register_camera_vision(stream_id, rtsp_url, segment_id, label, fps_sample)
    if "error" in vision:
        delete_camera_backend(stream_id)
        return vision

    return {"status": "CONNECTED", "backend": backend, "vision": vision}


@st.cache_data(ttl=2)
def get_vision_streams():
    """List all active RTSP streams from vision-service."""
    try:
        r = requests.get(f"{VISION_URL}/streams", timeout=1.0)
        return r.json() if r.status_code == 200 else {"total": 0, "streams": []}
    except Exception:
        return {"total": 0, "streams": []}


def get_stream_metrics(stream_id: str):
    """Get current detection metrics for one RTSP stream."""
    try:
        r = requests.get(f"{VISION_URL}/streams/{stream_id}/metrics", timeout=0.8)
        return r.json() if r.status_code == 200 else {}
    except Exception:
        return {}


def stop_camera_vision(stream_id: str):
    """Stop an RTSP stream worker in vision-service."""
    try:
        r = requests.delete(f"{VISION_URL}/streams/{stream_id}", timeout=5)
        return r.json() if r.status_code == 200 else {"error": r.text}
    except Exception as e:
        return {"error": str(e)}


def toggle_camera_backend(stream_id: str):
    """Enable/disable a camera in the Java backend."""
    try:
        r = requests.put(f"{BACKEND_URL}/cameras/{stream_id}/toggle", timeout=5)
        return r.json() if r.status_code == 200 else {"error": r.text}
    except Exception as e:
        return {"error": str(e)}


def delete_camera_backend(stream_id: str):
    """Remove a camera from the Java backend registry."""
    try:
        r = requests.delete(f"{BACKEND_URL}/cameras/{stream_id}", timeout=5)
        return {"success": True} if r.status_code == 204 else {"error": r.text}
    except Exception as e:
        return {"error": str(e)}


def mjpeg_url(stream_id: str) -> str:
    """Return the MJPEG live stream URL for embedding in HTML."""
    return f"{VISION_URL}/streams/{stream_id}/live"


def snapshot_url(stream_id: str) -> str:
    """Return the snapshot URL for a stream."""
    return f"{VISION_URL}/streams/{stream_id}/snapshot"
