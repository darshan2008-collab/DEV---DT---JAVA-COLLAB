"""
AI-Driven Smart Ambulance Navigation - Synthetic Traffic Dataset Generator
Generates 2 hours of synthetic 30s observations (1,440 records across 6 segments)
Includes QUEUE_ON_B2 between minutes 60 and 90, noise, speed spikes, and duplicate samples.
"""

import os
import random
import pandas as pd
from datetime import datetime, timezone, timedelta

SEGMENTS = [
    {"id": "A1", "typical": 30.0, "distance": 3.0},
    {"id": "A2", "typical": 15.0, "distance": 3.0},
    {"id": "B1", "typical": 40.0, "distance": 4.0},
    {"id": "B2", "typical": 40.0, "distance": 4.0},
    {"id": "C1", "typical": 40.0, "distance": 5.0},
    {"id": "C2", "typical": 40.0, "distance": 5.0}
]

def generate_dataset(output_path="data/samples/synthetic_traffic_observations.csv"):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    random.seed(42)

    start_time = datetime(2026, 11, 2, 8, 0, 0, tzinfo=timezone.utc)
    records = []

    # 240 intervals of 30 seconds = 120 minutes (2 hours)
    for step in range(240):
        current_time = start_time + timedelta(seconds=step * 30)
        minute = step * 0.5

        for seg in SEGMENTS:
            sid = seg["id"]
            typical = seg["typical"]

            # Scenario logic: Queue on B2 between minutes 60 and 90
            is_b2_queue = (sid == "B2" and 60.0 <= minute <= 90.0)

            if is_b2_queue:
                speed = 12.0 + (random.random() - 0.5) * 1.5
                queue_m = 140.0 + (random.random() - 0.5) * 20.0
                vehicle_count = random.randint(16, 22)
                density = round(vehicle_count / seg["distance"], 1)
                confidence = round(0.82 + random.random() * 0.12, 2)
            else:
                speed = typical + (random.random() - 0.5) * 2.0
                queue_m = 5.0 if sid != "A2" else 60.0
                vehicle_count = random.randint(8, 14)
                density = round(vehicle_count / seg["distance"], 1)
                confidence = round(0.88 + random.random() * 0.10, 2)

            # Inject occasional speed spike (1% chance)
            if random.random() < 0.01:
                speed = typical * 1.8  # Will be masked by preprocessing

            # Inject occasional low confidence (1% chance)
            if random.random() < 0.01:
                confidence = 0.35  # Will be masked by preprocessing

            rec = {
                "segment_id": sid,
                "timestamp": current_time.isoformat(),
                "vehicle_count": vehicle_count,
                "density_veh_per_km": density,
                "speed_kmph": round(max(5.0, speed), 1),
                "queue_m": round(max(0.0, queue_m), 1),
                "confidence": confidence,
                "data_source": "SIMULATION"
            }
            records.append(rec)

            # Inject occasional duplicate record (1% chance)
            if random.random() < 0.01:
                records.append(rec.copy())

    df = pd.DataFrame(records)
    df.to_csv(output_path, index=False)
    print(f"Generated {len(df)} synthetic traffic observation records in {output_path}")
    return df

if __name__ == "__main__":
    generate_dataset()
