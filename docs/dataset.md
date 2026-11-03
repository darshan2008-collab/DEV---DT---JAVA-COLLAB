# Traffic Dataset & Preprocessing Specification

## 1. Internal Observation Schema

All inputs—whether generated from the traffic simulator, uploaded CSV datasets, or extracted by the YOLO+ByteTrack vision service—conform to this unified schema:

| Field | Type | Description | Validation Rule |
|---|---|---|---|
| `segment_id` | String | Unique identifier (e.g., A1, A2, B1, B2, C1, C2) | Must exist in road network |
| `timestamp` | ISO-8601 | Observation timestamp in UTC | Required |
| `vehicle_count` | Integer | Total tracked vehicles currently in segment region | >= 0 |
| `density_veh_per_km` | Float | Estimated density in vehicles per kilometer | >= 0.0 |
| `speed_kmph` | Float | Average tracked speed in kilometers per hour | >= 0.0 |
| `queue_m` | Float | Length of stopped or slow vehicle queue in meters | >= 0.0 |
| `confidence` | Float | AI model detection confidence score | Range: [0.0, 1.0] |
| `data_source` | String | Origin identifier | SIMULATION, CAMERA_LIVE, CAMERA_REPLAY, CSV |

---

## 2. Preprocessing & Quality Assurance Pipeline

1. **Duplicate Rejection:**
   Identical `(segment_id, timestamp)` tuples are discarded immediately to prevent double-counting.
2. **Confidence Masking:**
   Frames with confidence `< 0.5` are flagged as masked. Poor video quality or occluded cameras are **never** interpreted as a clear road.
3. **Outlier Filtering:**
   Speeds that are negative or exceed `1.5 × reference_typical_speed` are masked as measurement anomalies.
4. **Gap Interpolation:**
   Temporal gaps of `<= 120 seconds` are bridged using the most recent valid observation.
5. **Trailing Median Smoothing:**
   Speeds are smoothed using a **trailing 5-sample median window**, eliminating single-frame spikes and detector noise.
6. **Congestion Classification:**
   Speed ratio $R = \frac{v_{\text{smoothed}}}{v_{\text{typical}}}$ is classified as:
   - $R \ge 0.80$: **LOW** (Free flow)
   - $0.60 \le R < 0.80$: **MEDIUM** (Moderate traffic)
   - $0.35 \le R < 0.60$: **HIGH** (Congestion / Queuing)
   - $R < 0.35$: **CRITICAL** (Severe gridlock)

---

## 3. Synthetic Benchmark Dataset

A reference synthetic dataset is generated in `data/samples/synthetic_traffic_observations.csv`:
- 1,452 observations spanning 2 hours across all 6 segments.
- Reproduces normal conditions for the first 60 minutes, followed by a simulated queue on **B2** (speed drops to 12 km/h) from minute 60 to 90.
- Injects realistic noise, occasional low confidence, outlier spikes, and duplicate samples to rigorously test preprocessing pipelines.
