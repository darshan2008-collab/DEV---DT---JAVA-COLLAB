# ESP32 Miniature Traffic Junction Wiring & Hardware Specification

## 1. Prototype Overview

The hardware demonstration prototype models a 4-way urban intersection controlled by an ESP32 microcontroller with Wi-Fi and MQTT connectivity.

- **North-South (NS) Direction:** Ambulance primary travel corridor.
- **East-West (EW) Direction:** Cross-traffic corridor.
- **Manual Override Button:** Immediate fail-safe interrupt button on GPIO 34.

---

## 2. Pinout Configuration

| Component | ESP32 GPIO Pin | Function | Resistor |
|---|---|---|---|
| **NS RED LED** | GPIO 18 | North-South Stop Signal | 220 Ω to GND |
| **NS YELLOW LED** | GPIO 19 | North-South Clearance Signal | 220 Ω to GND |
| **NS GREEN LED** | GPIO 21 | North-South Priority Signal | 220 Ω to GND |
| **EW RED LED** | GPIO 22 | East-West Stop Signal | 220 Ω to GND |
| **EW YELLOW LED** | GPIO 23 | East-West Clearance Signal | 220 Ω to GND |
| **EW GREEN LED** | GPIO 25 | East-West Normal Cycle Signal | 220 Ω to GND |
| **OVERRIDE BUTTON** | GPIO 34 | Emergency Manual Override | Internal Pull-Up (Active LOW) |

---

## 3. Safe State Transition Sequence

```
[Normal Cycle: EW Green, NS Red]
               │
               │ Ambulance priority request received over MQTT
               ▼
[Yellow Clearance (3s)]
  - EW: YELLOW
  - NS: RED
               │
               ▼
[All-Red Clearance (2s)]
  - EW: RED
  - NS: RED (CRITICAL INVARIANT: Never two greens simultaneously!)
               │
               ▼
[Green Priority (8s)]
  - EW: RED
  - NS: GREEN (Ambulance passes intersection)
               │
               ▼
[Normal Alternating Cycle Resumed]
```

---

## 4. Software Signal Simulator

For evaluations where physical ESP32 hardware is not wired, run the software simulator:
```bash
python simulator/signal_simulator.py
```
This Python daemon connects to the Mosquitto broker, listens to `junction/{id}/priority/request`, and executes the identical safe phase state machine with real-time state broadcasts.
