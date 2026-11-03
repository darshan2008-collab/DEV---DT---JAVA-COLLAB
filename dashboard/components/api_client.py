"""
API Client Helper for Streamlit Dashboards
Connects to Spring Boot Backend and FastAPI Vision Service
"""

import os
import requests
import streamlit as st

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080/api")
VISION_URL = os.getenv("VISION_URL", "http://localhost:8000")


def get_backend_health():
    try:
        r = requests.get(f"{BACKEND_URL}/health", timeout=2)
        return r.json() if r.status_code == 200 else None
    except Exception:
        return None


def get_traffic_current():
    try:
        r = requests.get(f"{BACKEND_URL}/traffic/current", timeout=3)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def get_roads():
    try:
        r = requests.get(f"{BACKEND_URL}/roads", timeout=3)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def get_ambulances():
    try:
        r = requests.get(f"{BACKEND_URL}/ambulances", timeout=3)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def get_hospitals():
    try:
        r = requests.get(f"{BACKEND_URL}/hospitals", timeout=3)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def get_trips():
    try:
        r = requests.get(f"{BACKEND_URL}/trips", timeout=3)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def get_trip(trip_id: str):
    try:
        r = requests.get(f"{BACKEND_URL}/trips/{trip_id}", timeout=3)
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
        r = requests.get(f"{BACKEND_URL}/trips/{trip_id}/recommendations", timeout=3)
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
        r = requests.get(f"{BACKEND_URL}/trips/{trip_id}/route-updates", timeout=3)
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


def get_simulation_status():
    try:
        r = requests.get(f"{BACKEND_URL}/simulation/status", timeout=2)
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
        r = requests.get(f"{BACKEND_URL}/signals/{junction_id}/state", timeout=2)
        return r.json() if r.status_code == 200 else {"state": "NORMAL_CYCLE", "active_direction": "EAST_WEST"}
    except Exception:
        return {"state": "NORMAL_CYCLE", "active_direction": "EAST_WEST"}


def signal_override(junction_id: str):
    r = requests.post(f"{BACKEND_URL}/signals/{junction_id}/override", timeout=5)
    return r.json() if r.status_code == 200 else {"error": r.text}


def get_evaluation_summary():
    try:
        r = requests.get(f"{BACKEND_URL}/evaluation/summary", timeout=3)
        return r.json() if r.status_code == 200 else {}
    except Exception:
        return {}


def get_live_events(limit: int = 20):
    try:
        r = requests.get(f"{BACKEND_URL}/traffic/events?limit={limit}", timeout=2)
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []


def get_vision_health():
    try:
        r = requests.get(f"{VISION_URL}/health", timeout=2)
        return r.json() if r.status_code == 200 else None
    except Exception:
        return None
