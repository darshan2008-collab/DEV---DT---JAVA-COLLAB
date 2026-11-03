"""
AI-Driven Smart Ambulance Navigation - Traffic Observation Producer
Sends structured observations to Spring Boot Backend /api/observations
"""

import sys
import time
import random
import requests
from datetime import datetime, timezone

BACKEND_URL = "http://localhost:8080/api"
SEGMENTS = ["A1", "A2", "B1", "B2", "C1", "C2"]


def send_scenario_batch(scenario="NORMAL"):
    print(f"[*] Sending observation batch for scenario: {scenario}")
    now = datetime.now(timezone.utc).isoformat()

    for seg in SEGMENTS:
        speed = 40.0
        vehicle_count = 10
        density = 20.0
        queue_m = 10.0
        confidence = 0.92

        if scenario == "NORMAL":
            if seg == "A1": speed = 30.0; vehicle_count = 14; density = 28.0
            elif seg == "A2": speed = 15.0; vehicle_count = 22; density = 44.0; queue_m = 60.0
            elif seg in ["B1", "B2", "C1", "C2"]: speed = 40.0; vehicle_count = 10
        elif scenario == "QUEUE_ON_B2":
            if seg == "B2":
                speed = 12.0
                vehicle_count = 18
                density = 62.0
                queue_m = 140.0
            elif seg == "A1": speed = 30.0
            elif seg == "A2": speed = 15.0
            else: speed = 40.0
        elif scenario == "SUDDEN_CONGESTION":
            speed = 10.0 if "B" in seg else 35.0
            queue_m = 120.0

        payload = {
            "segment_id": seg,
            "timestamp": now,
            "vehicle_count": vehicle_count,
            "density_veh_per_km": density,
            "speed_kmph": speed,
            "queue_m": queue_m,
            "confidence": confidence,
            "data_source": "SIMULATION"
        }

        try:
            r = requests.post(f"{BACKEND_URL}/observations", json=payload, timeout=3)
            print(f" -> {seg}: speed={speed} km/h (status={r.status_code})")
        except Exception as e:
            print(f" -> Failed to send {seg}: {e}")


if __name__ == "__main__":
    scenario = sys.argv[1] if len(sys.argv) > 1 else "NORMAL"
    send_scenario_batch(scenario)
