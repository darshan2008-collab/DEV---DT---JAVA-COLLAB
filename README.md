# AI-Driven Smart Ambulance Navigation with Real-Time Traffic Analysis

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python 3.11+](https://img.shields.io/badge/Python-3.11%2B-yellow.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-teal.svg)](https://fastapi.tiangolo.com/)
[![Streamlit](https://img.shields.io/badge/Streamlit-1.38%2B-red.svg)](https://streamlit.io/)
[![YOLOv8 + ByteTrack](https://img.shields.io/badge/YOLO-v8%20%2B%20ByteTrack-blueviolet.svg)](https://github.com/ultralytics/ultralytics)
[![ESP32](https://img.shields.io/badge/ESP32-Arduino%20Framework-orange.svg)](https://www.espressif.com/)

> **Academic Project Submission**  
> **Department of Artificial Intelligence & Data Science**  
> **M. Kumarasamy College of Engineering, Karur, Tamil Nadu, India**  
> Prepared for the **Smart India Hackathon (SIH)**.

---

> [!WARNING]
> ### PROTOTYPE NOTICE & CLINICAL / TRAFFIC DISCLAIMER
> This software is an **academic demonstration prototype**. It is **NOT** approved for real emergency medical dispatch, real-world traffic signal preemption, or clinical patient triage. Evaluations and benchmark figures reflect **simulations on assumed illustrative speeds**, not measured real-world traffic or patient outcome improvements. Real-world traffic signal preemption requires municipal traffic authority authorization and rigorous fail-safe certifications.

---

## 1. Problem Statement & Objectives

### Problem
Emergency ambulances lose critical golden-hour minutes navigating congested urban road corridors. Conventional navigation applications rely on general map data and aggregate probe telemetry. They lack camera-based visibility into real-time junction queues, cannot preempt traffic signals along the route, and fail to provide trauma hospitals with accurate, dynamic arrival times. Furthermore, routes determined at dispatch frequently become congested after the trip has commenced.

### Objectives
1. **Estimate Segment Congestion from Video:** Analyze traffic-camera feeds using OpenCV and Ultralytics YOLO with ByteTrack tracking to determine vehicle count, density (veh/km), smoothed speed, and queue length.
2. **Optimize by Travel Time, Not Distance:** Select routes with the lowest estimated travel time rather than shortest geographic distance.
3. **Dynamic Re-Routing with Hysteresis:** Continuously evaluate traffic changes and recalculate routes using Dijkstra and A* search engines, requiring a configurable hysteresis margin (**1.0 min**) to prevent route oscillation.
4. **Driver Autonomy & Safe Fallback:** Present re-routing suggestions for explicit driver **Acceptance** or **Rejection**. If camera data becomes stale (**> 120s**), fail safe to the distance-based baseline with an **"ETA Uncertain"** warning instead of interpreting missing data as a clear road.
5. **Miniature ESP32 Signal Priority:** Demonstrate junction signal priority over MQTT with strict collision-prevention phase sequences (Yellow ➔ All-Red ➔ Green Priority).

### Out of Scope for this Minimum Viable Product (Future Scope)
- Machine learning traffic forecasting (e.g., XGBoost / LSTM).
- Automatic accident detection (stopped vehicles are treated as queues; accident detection is deferred).
- Reinforcement learning routing agents.
- LoRa / 4G cellular hardware connectivity (Wi-Fi and MQTT are used).
- Multi-junction and multi-ambulance network coordination.

---

## 2. Technology Stack

| Component | Technology | Role |
|---|---|---|
| **Core Backend & Optimizer** | **Java 21, Spring Boot 3.4.2**, Maven | Single source of truth: Dijkstra & A* routing, decision engine, trip lifecycle, Flyway migrations |
| **Persistence** | PostgreSQL 16 (Docker) / H2 In-Memory (Local) | Stores roads, observations, trips, route updates, recommendations, and signal events |
| **Real-Time Telemetry** | Spring WebSocket (`/ws/live`) + Polling Fallback | Broadcasts live route changes, vehicle movements, and hospital ETA updates |
| **Signal Messaging** | Eclipse Mosquitto (MQTT) + Eclipse Paho | Publishes and coordinates signal priority requests |
| **Vision Service** | **Python 3.11**, FastAPI, OpenCV, Ultralytics YOLOv8, ByteTrack | Extracts vehicle densities, smoothed speeds, and queue lengths from video feeds |
| **Dashboards** | **Python Streamlit** (Multi-Page), Plotly, Folium | Glanceable driver view, dispatcher console, hospital trauma view, signal police monitor |
| **Hardware Prototype** | ESP32 (Arduino / PlatformIO), 6 LEDs, Button | Miniature junction controller enforcing safe phase transitions |

---

## 3. Road Network & Scenario Specification

The system models the Karur demo road network consisting of a source dispatch point `S`, intermediate junctions `J1`, `J2`, `J3`, and hospital trauma centre `H`:

```
               [J1] (A1: 3km, 30 km/h) ─────── (A2: 3km, 15 km/h) ───────┐
              /                                                            │
             /                                                             ▼
[S: Dispatch] ─── [J2] (B1: 4km, 40 km/h) ─────── (B2: 4km, 40 km/h) ─── [H: Hospital]
             \                                                             ▲
              \                                                            │
               [J3] (C1: 5km, 40 km/h) ─────── (C2: 5km, 40 km/h) ───────┘
```

### Exact Benchmark Numbers (Section 5)

| Route | Segments | Distance | Initial ETA | ETA After B2 Queue | Outcome |
|---|---|---|---|---|---|
| **Route A** | A1 + A2 | **6.0 km** | 18.0 min | 18.0 min | **Shortest by distance.** Chosen by distance-only baseline. |
| **Route B** | B1 + B2 | 8.0 km | **12.0 min** | 26.0 min (Queue on B2: 12 km/h) | **Fastest initially.** Selected first by dynamic optimizer. |
| **Route C** | C1 + C2 | 10.0 km | 15.0 min | **15.0 min** | **Fastest after queue.** Selected upon dynamic re-routing. |

- **Initial State:** Route B is selected (12.0 min) despite being 2 km longer than Route A (18.0 min).
- **Congestion Event:** When a queue forms on B2 (speed drops from 40 to 12 km/h), B's ETA increases to 26.0 min. The dynamic optimizer identifies Route C (15.0 min), which offers an 11-minute advantage (> 1.0 min hysteresis margin), and prompts the driver to switch.

---

## 4. End-to-End Section 14 Demonstration

The dispatcher dashboard includes a dedicated **"🚨 RUN EMERGENCY NAVIGATION DEMO"** button that executes the complete Section 14 sequence without requiring external routing services:

1. **Ambulance & Hospital Selection:** Available ambulance `AMB-01` and receiving hospital `H-01` are selected.
2. **Dispatch & Candidate Calculation:** Candidate routes A, B, and C are calculated. **Route B (12 min)** is recommended and assigned.
3. **Navigation Start:** Ambulance journey starts, following route geometry on the interactive Folium map.
4. **Signal Preemption:** Priority request is published to Junction `J2`. The signal controller initiates the safe clearance sequence (Yellow ➔ All-Red ➔ Green Priority).
5. **Traffic Injection:** Scenario `QUEUE_ON_B2` triggers. Segment B2 speed drops to 12 km/h, vehicle count rises to 18, and queue reaches 140m.
6. **Dynamic Recalculation:** Route B rises to 26 min. The optimizer identifies **Route C (15 min)** as the new optimal path.
7. **Driver Autonomy:** A recommendation is presented to the driver cockpit with reasons. Upon clicking **Accept**, a `RouteUpdate` is persisted, a `ROUTE_CHANGED` event is broadcast, and the map updates.
8. **Hospital Alert:** The receiving emergency room view updates with the revised ETA.
9. **Stale Data Verification (Optional):** Triggering `FEED_LOST` causes the system to timeout after 120s, flag the ETA as **UNCERTAIN**, and fall back to the distance baseline.
10. **Mission Completion:** The ambulance reaches the emergency trauma bay, recording arrival timestamp and updating metrics.

---

## 5. Quick Start Instructions

### Prerequisites
- **Java 21** (JDK 21)
- **Apache Maven 3.9+**
- **Python 3.11+**
- **Docker & Docker Compose** (Optional, for full containerized stack)

---

### Option A: Running with Docker Compose (Recommended for Production Stack)

1. Clone or navigate to the project directory:
   ```bash
   cd "d:/save from yematharma raja"
   ```
2. Start all services using Docker Compose:
   ```bash
   docker compose up --build
   ```
3. Open the applications in your browser:
   - **Streamlit Dashboards:** [http://localhost:8501](http://localhost:8501)
   - **Spring Boot REST API / Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **FastAPI Vision Service:** [http://localhost:8000/docs](http://localhost:8000/docs)
   - **Mosquitto MQTT Broker:** `localhost:1883`

---

### Option B: Running Locally without Docker (H2 In-Memory Profile)

#### 1. Start the Java Spring Boot Backend
```bash
cd backend-java
mvn spring-boot:run
```
*(The backend defaults to the `local` profile with an in-memory H2 database, automatically executing Flyway migrations and synthetic seeds).*

#### 2. Start the FastAPI Vision Service
```bash
cd vision-service
pip install -r requirements.txt
uvicorn app.main:app --port 8000 --reload
```

#### 3. Start the Streamlit Dashboards
```bash
cd dashboard
pip install -r requirements.txt
streamlit run app.py --server.port 8501
```

#### 4. Start the Software Signal Simulator (Optional, mirrors ESP32)
```bash
python simulator/signal_simulator.py
```

---

## 6. Testing & Quality Assurance

### Java Automated Tests (JUnit 5 + Spring Boot Test)
The test suite covers all 7 documented scenarios, preprocessing rules, algorithm agreement, and security invariants:
```bash
cd backend-java
mvn test
```

**Documented Scenario Test Matrix:**
- `Scenario 1:` Both routes clear ➔ Select Route B (12 min).
- `Scenario 2:` Queue on B2 ➔ Select Route C (15 min).
- `Scenario 3:` Sudden traffic increase ➔ Dynamic route recalculation.
- `Scenario 4:` Camera feed unavailable (>120s) ➔ Fallback to baseline Route A with ETA uncertain.
- `Scenario 5:` Minor traffic fluctuation ➔ Hysteresis margin prevents route oscillation.
- `Scenario 6:` Signal priority request ➔ Safe phase state machine (never two conflicting greens).
- `Scenario 7:` Destination reached ➔ Trip completed and arrival recorded.
- `Algorithm Agreement:` Dijkstra and A* produce identical costs.
- `Closed Road Exclusion:` Segment B1 marked closed is excluded from routes.
- `Preprocessing Pipeline:` Duplicate rejection, confidence masking (<0.5), speed outlier masking (>1.5x reference), and 5-sample median smoothing.

### Python Automated Tests (pytest)
```bash
python -m pytest vision-service/tests/test_vision_and_simulator.py
```
Covers schema validation, outlier filtering, dataset profiling, and the safe-phase signal invariant.

---

## 7. Project Structure

```
smart-ambulance/
|-- backend-java/            # Spring Boot 3, Java 21, Maven
|   |-- src/main/java/com/smartambulance/
|   |   |-- api/             # REST Controllers, DTOs, Global Exception Handler
|   |   |-- config/          # JWT Security, CORS, Swagger OpenAPI Config
|   |   |-- decision/        # Hysteresis & Stale-Data Decision Engine
|   |   |-- domain/          # JPA Entities (Ambulances, Roads, Trips, Signals)
|   |   |-- mqtt/            # Eclipse Paho MQTT Priority Publisher
|   |   |-- repository/      # Spring Data JPA Repositories
|   |   |-- routing/         # Dijkstra, A*, Baseline Planners, RoadGraph
|   |   |-- service/         # Preprocessing, Trip Lifecycle, Simulation, Evaluation
|   |   |-- websocket/       # /ws/live Event Broadcaster
|   |-- src/main/resources/
|   |   |-- application.yml  # Local (H2) and Docker (PostgreSQL) profiles
|   |   |-- db/migration/    # Flyway V1 schema & V2 demo seed migrations
|   |-- src/test/java/       # Comprehensive JUnit 5 scenario test suite
|   |-- pom.xml
|-- vision-service/          # Python 3.11, FastAPI, OpenCV, YOLOv8, ByteTrack
|   |-- app/main.py          # Vision endpoints (/detect/image, /detect/video)
|   |-- tests/               # pytest test suite
|   |-- requirements.txt
|-- dashboard/               # Python Streamlit Multi-Page Application
|   |-- app.py               # Main overview and system health dashboard
|   |-- pages/
|   |   |-- 1_🚑_Driver_View.py
|   |   |-- 2_📋_Dispatcher_View.py
|   |   |-- 3_🏥_Hospital_View.py
|   |   |-- 4_🚦_Signal_Police_View.py
|   |   |-- 5_📊_Traffic_Monitor.py
|   |   |-- 6_📷_Camera_Analysis.py
|   |   |-- 7_📈_Evaluation.py
|   |   |-- 8_📁_Dataset_Explorer.py
|   |   |-- 9_⚙️_Settings.py
|   |-- components/          # API client and Folium map rendering utilities
|-- simulator/               # Traffic producer & Software signal simulator
|-- firmware/esp32-junction/ # ESP32 C++ firmware (PlatformIO / Arduino)
|-- data/samples/            # 2-hour synthetic benchmark observation dataset
|-- scripts/                 # Dataset generator and detection evaluation scripts
|-- docs/                    # Architecture, API, Dataset, Evaluation, and Wiring specs
|-- docker-compose.yml       # Production Docker composition
|-- .env.example             # Environment variable placeholders
|-- README.md                # System documentation
```

---

## 8. Role-Based Access Control

The backend enforces role permissions across 4 user personas:
- `ADMIN` (`admin` / `admin123`): Global system settings, database inspection, algorithm tuning.
- `AMBULANCE_OPERATOR` (`driver` / `driver123`): Driver cockpit, route viewing, recommendation accept/reject.
- `HOSPITAL_STAFF` (`hospital` / `hospital123`): Receiving trauma bay inflow monitor, live ETA tracking.
- `TRAFFIC_POLICE` (`police` / `police123`): Junction status, safe phase monitoring, manual override.

---

## 9. Hardware Signal Priority Wiring (ESP32)

Refer to [docs/wiring.md](docs/wiring.md) for full schematics and breadboard layouts:
- **North-South (Ambulance):** Red = GPIO 18, Yellow = GPIO 19, Green = GPIO 21.
- **East-West (Cross-Traffic):** Red = GPIO 22, Yellow = GPIO 23, Green = GPIO 25.
- **Manual Override Button:** GPIO 34 (Active LOW with internal pull-up).

---

## 10. Authors & Acknowledgements
- **Institution:** M. Kumarasamy College of Engineering, Karur - Department of AI & Data Science.
- **Event:** Smart India Hackathon (SIH).
