# ESP32 Miniature Traffic Junction Wiring & Hardware Specification

## 1. Prototype Overview

The hardware demonstration prototype models an urban emergency preemption intersection controlled by an ESP32 microcontroller with Wi-Fi and HTTP REST connectivity.

- **3-LED Traffic Light Module:** Red, Yellow, Green signal heads with built-in 330Ω resistors.
- **I2C 0.96" OLED Display (SSD1306):** Displays junction telemetry, live signal state, and Wi-Fi IP.
- **JLG1295-5 Electromagnetic Buzzer:** Tactile audio feedback on light switch and emergency two-tone "Wee-Woo" siren.

---

## 2. Complete Pinout Configuration

| Component | Pin Label | ESP32 GPIO Pin | Function / Description |
|---|---|---|---|
| **Traffic Light Module** | **GND** | **GND** | Ground (Common Cathode) |
| **Traffic Light Module** | **R** | **GPIO 18** | Red Signal (Stop Cross Traffic) |
| **Traffic Light Module** | **Y** | **GPIO 19** | Yellow Signal (Preemption Clearance) |
| **Traffic Light Module** | **G** | **GPIO 23** | Green Signal (Ambulance Priority Wave) |
| **JLG1295-5 Buzzer** | **(+)** (Long Pin / '+' mark) | **GPIO 25** | Audio output / Siren PWM |
| **JLG1295-5 Buzzer** | **(-)** (Short Pin) | **GND** | Ground |
| **0.96" OLED Display** | **VCC** | **3.3V** | Power |
| **0.96" OLED Display** | **GND** | **GND** | Ground |
| **0.96" OLED Display** | **SCL** | **GPIO 22** | I2C Clock Pin |
| **0.96" OLED Display** | **SDA** | **GPIO 21** | I2C Data Pin |

---

## 3. Audio & Visual Behaviors

1. **Light Change Feedback:**
   - **RED:** 1 short confirmation beep (80ms).
   - **YELLOW:** 1 short beep (60ms).
   - **GREEN:** Double beep (50ms - 40ms pause - 50ms) indicating corridor clear priority.
   - **OFF:** 1 long beep (100ms) indicating power standby.
2. **Emergency Siren Mode:**
   - Continuous alternating two-tone siren:
     - High tone: **960 Hz** (synchronized with RED LED flash).
     - Low tone: **680 Hz** (synchronized with GREEN LED flash).
   - Produces the authentic ambulance "Wee-Woo" emergency siren sound!
