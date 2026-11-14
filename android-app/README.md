# Smart Ambulance Controller - Android Native Mobile App

This is a complete, native Android Studio project developed for the **AI-Driven Smart Ambulance Navigation System** (M. Kumarasamy College of Engineering, Karur - B.Tech AI & Data Science).

---

## Features
1. **Direct ESP32 Wi-Fi Communication:**
   - Works over ESP32 SoftAP Hotspot (`192.168.4.1`) or local mobile hotspot IP.
   - Zero cloud lag (instant sub-millisecond execution).
2. **Physical 4-Pin RGB LED Signal Head Control:**
   - **RED:** Enforces cross-traffic stop.
   - **YELLOW:** Caution & 3-second clearance wave.
   - **GREEN:** Emergency ambulance priority corridor green.
   - **BLUE:** Standby / diagnostic telemetry.
   - **SIREN FLASH:** Alternate Red & Blue high-intensity flashing.
   - **TURN OFF:** Standby power saving.
3. **Emergency Corridor Preemption Protocol:**
   - One-tap button executes safe phase transitions (Yellow Clearance -> Ambulance Green).
4. **Haptic Feedback & Real-Time Telemetry Terminal:**
   - Provides physical vibration feedback on touch.
   - Displays live HTTP request/response codes and latency.

---

## How to Build the APK in Google Android Studio

1. **Open the Project:**
   - Open **Google Android Studio**.
   - Click **File -> Open...**
   - Navigate to: `D:\save from yematharma raja\android-app` and click **OK**.
   - Allow Gradle to sync (usually takes 1-2 minutes on first run).

2. **Generate the APK:**
   - From the top menu bar, click:
     **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
   - Android Studio will compile the app and show a popup at the bottom right:
     `APK(s) generated successfully for 1 module: 'app'. Locate`.

3. **Locate & Install the APK:**
   - Click **Locate** (or find it at `android-app/app/build/outputs/apk/debug/app-debug.apk`).
   - Transfer this `app-debug.apk` to your Android mobile phone (via USB, Bluetooth, or Google Drive / WhatsApp).
   - Tap on the APK file on your mobile phone and click **Install**.

---

## How to Use the App with ESP32 Hardware

1. Power on your **ESP32** board.
2. On your Android phone, go to **Wi-Fi Settings** and connect to:
   - **Wi-Fi SSID:** `ESP32-AMBULANCE-SIGNAL`
   - **Password:** `12345678`
3. Open the **Smart Ambulance** app on your phone.
4. Tap **Ping / Connect** (defaults to `192.168.4.1`). The status turns **Online (HTTP 200 OK)**!
5. Tap **RED**, **YELLOW**, **GREEN**, **BLUE**, or **SIREN** — your physical breadboard RGB LED will change color immediately!
