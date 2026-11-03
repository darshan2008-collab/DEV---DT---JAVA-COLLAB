# System Architecture & Technical Specification

## Project Title
**AI-Driven Smart Ambulance Navigation with Real-Time Traffic Analysis**  
*Academic Project: B.Tech AI & Data Science, M. Kumarasamy College of Engineering, Karur.*  
*Prepared for Smart India Hackathon.*

---

## 1. High-Level Architecture

```
Camera Video / Replay Footage
             │
             ▼
┌───────────────────────────────────────┐
│     Python Vision Service (FastAPI)    │
│  - Ultralytics YOLOv8                 │
│  - ByteTrack Multi-Object Tracking    │
│  - Density, Speed & Queue Estimation   │
└──────────────────┬────────────────────┘
                   │ HTTP POST /api/observations
                   ▼
┌────────────────────────────────────────────────────────┐
│             Java Backend (Spring Boot 3)               │
│  - Single Source of Truth for Routes, Trips & Decisions│
│  - Preprocessing (Median Smoothing, Gap Interpolation) │
│  - Route Optimizer (Dijkstra + A* Search Engine)       │
│  - Decision Engine (1.0 min Hysteresis, Stale Fallback)│
│  - PostgreSQL / H2 In-Memory Database                  │
│  - REST API (Springdoc OpenAPI) & WebSocket (/ws/live) │
└──────────────┬───────────────────────────┬─────────────┘
               │                           │ MQTT QoS 1
               ▼                           ▼
┌─────────────────────────────┐   ┌───────────────────────────────┐
│   Python Streamlit Multi-   │   │ Eclipse Mosquitto MQTT Broker │
│        Page Dashboards      │   └───────────────┬───────────────┘
│  - Driver Glanceable View   │                   │
│  - Dispatcher Console       │                   ▼
│  - Hospital Trauma Bay      │   ┌───────────────────────────────┐
│  - Signal / Police View     │   │   ESP32 Miniature Junction    │
│  - Traffic Monitor & Vision │   │  - Safe Phase State Machine   │
│  - Evaluation & Datasets    │   │  - Yellow -> All-Red -> Green │
└─────────────────────────────┘   │  - Physical Override Button   │
                                  └───────────────────────────────┘
```

---

## 2. Core Architectural Principles

1. **Java Backend as Single Source of Truth:**
   The Python dashboards never compute routes independently. The Java Spring Boot engine executes all Dijkstra and A* graph calculations, applies hysteresis thresholds, validates trip state transitions, and serves authoritative telemetry.
2. **Deterministic Data Separation:**
   All simulated metrics, assumed speeds, and synthetic observations are clearly distinguished from live camera detections. Simulated numbers demonstrate algorithmic logic without fabricating physical measurements.
3. **Driver Autonomy & Invariant Safety:**
   The driver remains in full control at all times. Turn recommendations require explicit driver confirmation (**Accept** or **Reject**). Nothing forces a vehicle turn.
4. **Fail-Safe on Degraded Data:**
   If traffic camera telemetry becomes stale (> 120 seconds) or masked, the system does not assume a clear road. It falls back to the distance baseline planner and flags the ETA as **UNCERTAIN**.
5. **Fail-Safe Signal Sequence:**
   The traffic signal firmware enforces a strict state machine: `Yellow Clearance (3s)` ➔ `All-Red Clearance (2s)` ➔ `Green Priority (8s)`. Conflicting directions are strictly prevented from simultaneously showing green.
