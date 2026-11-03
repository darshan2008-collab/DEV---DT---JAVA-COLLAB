# REST API & Messaging Specification

## Base URLs
- **Java Spring Boot Backend:** `http://localhost:8080/api`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Schema:** `http://localhost:8080/v3/api-docs`
- **Python Vision Service:** `http://localhost:8000`
- **WebSocket Live Stream:** `ws://localhost:8080/ws/live`

---

## 1. Authentication Endpoints

### `POST /api/auth/login`
Authenticates users and issues JWT Bearer tokens.
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```
**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "role": "ADMIN",
  "full_name": "System Administrator"
}
```

### `GET /api/auth/me`
Returns current authenticated user profile.

---

## 2. Traffic Observation Endpoints

### `POST /api/observations`
Submits real-time or simulated traffic observations from cameras or simulators.
```bash
curl -X POST http://localhost:8080/api/observations \
  -H "Content-Type: application/json" \
  -d '{
    "segment_id": "B2",
    "timestamp": "2026-11-02T10:15:30Z",
    "vehicle_count": 18,
    "density_veh_per_km": 62.0,
    "speed_kmph": 12.0,
    "queue_m": 140.0,
    "confidence": 0.84,
    "data_source": "SIMULATION"
  }'
```
**Response (200 OK):**
```json
{
  "accepted": true,
  "duplicate": false,
  "masked": false,
  "segment_id": "B2",
  "smoothed_speed_kmph": 12.0,
  "travel_time_minutes": 20.0,
  "congestion_level": "HIGH",
  "message": "Processed successfully"
}
```

### `GET /api/traffic/current`
Returns current smoothed speed, travel time, and congestion levels for all network segments.

### `GET /api/traffic/history?segment=B2`
Returns chronological observations for historical analysis.

---

## 3. Dynamic Routing Endpoints

### `POST /api/routes/calculate`
Calculates candidate paths and ranks them by lowest estimated travel time.
```bash
curl -X POST http://localhost:8080/api/routes/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "S",
    "destination": "H",
    "algorithm": "DIJKSTRA",
    "include_signal_delay": false
  }'
```

---

## 4. Emergency Trips Lifecycle

- `GET /api/trips` - Lists all trips
- `POST /api/trips` - Creates a new trip (`{"ambulance_id": "AMB-01", "hospital_id": "H-01", "origin": "S", "destination": "H"}`)
- `POST /api/trips/{id}/start` - Activates mission and begins telemetry tracking
- `POST /api/trips/{id}/pause` - Temporarily halts mission progression
- `POST /api/trips/{id}/resume` - Resumes active mission
- `POST /api/trips/{id}/complete` - Records arrival at hospital trauma bay
- `POST /api/trips/{id}/cancel` - Cancels active mission
- `POST /api/trips/{id}/recommendations/{rid}/accept` - Driver accepts route recommendation
- `POST /api/trips/{id}/recommendations/{rid}/reject` - Driver rejects route recommendation

---

## 5. Signal Priority & Traffic Police

- `POST /api/signals/{junctionId}/priority` - Requests priority for approaching ambulance
- `GET /api/signals/{junctionId}/state` - Returns current safe phase state and active green direction
- `POST /api/signals/{junctionId}/override` - Immediate manual override reverting to normal cycle

---

## 6. Simulation & Evaluation

- `POST /api/simulation/start | pause | reset`
- `POST /api/simulation/scenario` (`NORMAL`, `QUEUE_ON_B2`, `SUDDEN_CONGESTION`, `FEED_LOST`, `ROAD_CLOSED`, `RECOVERY`)
- `GET /api/evaluation/summary` - Returns comparative metrics between dynamic optimizer and distance baseline
- `GET /api/evaluation/export.csv` - Downloads evaluation metrics as CSV
