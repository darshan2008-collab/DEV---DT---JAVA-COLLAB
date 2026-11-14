/*
 * =========================================================================
 * Smart Ambulance Navigation System - ESP32 Miniature Traffic Controller
 * Hardware:
 *   1. 3-LED Traffic Light Module (R, Y, G, GND)
 *   2. I2C 0.96" OLED Display (SSD1306) via U8G2 Library
 *   3. JLG1295-5 Electromagnetic Buzzer (Audio Feedback & Ambulance Siren)
 * =========================================================================
 * 
 * HARDWARE CONNECTIONS:
 * 
 * 1. 3-LED TRAFFIC LIGHT MODULE (Printed: G, Y, R, GND):
 *    [GND] Pin -> ESP32 GND
 *    [ R ] Pin -> ESP32 GPIO 18 (Red LED)
 *    [ Y ] Pin -> ESP32 GPIO 19 (Yellow LED)
 *    [ G ] Pin -> ESP32 GPIO 23 (Green LED)
 *    (330 Ohm resistors are already integrated on the PCB module!)
 * 
 * 2. JLG1295-5 BUZZER (Marked with '+' on side):
 *    [(+)] Pin (Long lead / near '+' mark) -> ESP32 GPIO 25
 *    [(-)] Pin (Short lead)               -> ESP32 GND
 * 
 * 3. 0.96" I2C OLED DISPLAY (SSD1306):
 *    [VCC] Pin -> ESP32 3.3V
 *    [GND] Pin -> ESP32 GND
 *    [SCL] Pin -> ESP32 GPIO 22
 *    [SDA] Pin -> ESP32 GPIO 21
 * =========================================================================
 */

#include <WiFi.h>
#include <WebServer.h>
#include <Wire.h>
#include <U8g2lib.h>

// U8G2 Constructor for 0.96" I2C SSD1306 128x64 OLED (Full Buffer, Fast & Crisp)
U8G2_SSD1306_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, /* reset=*/ U8X8_PIN_NONE, /* clock=*/ 22, /* data=*/ 21);

// --- Wi-Fi Configuration ---
const char* STA_SSID = "DARSHAN K";
const char* STA_PASS = "12345678";

const char* AP_SSID  = "ESP32-AMBULANCE-SIGNAL";
const char* AP_PASS  = "12345678";

// --- Hardware Pin Definitions ---
#define PIN_RED        18
#define PIN_YELLOW     19
#define PIN_GREEN      23
#define PIN_BUZZER     25
#define PIN_ONBOARD_LED 2

WebServer server(80);

// State tracking
String currentSignal = "GREEN";
bool sirenActive = false;
unsigned long lastSirenToggle = 0;
bool sirenFlip = false;
String staIpStr = "Connecting...";

// Quick tactile confirmation beep (No default argument to prevent Arduino IDE preprocessor errors)
void beepFeedback(int durationMs) {
  digitalWrite(PIN_BUZZER, HIGH);
  delay(durationMs);
  digitalWrite(PIN_BUZZER, LOW);
}

void beepFeedback() {
  beepFeedback(70);
}

// Update OLED Screen using U8G2
void updateOled(String signalName) {
  u8g2.clearBuffer();

  // Top Banner / Header (Inverted background box)
  u8g2.setDrawColor(1);
  u8g2.drawRBox(0, 0, 128, 14, 2);
  u8g2.setDrawColor(0); // White text on dark box
  u8g2.setFont(u8g2_font_6x10_tf);
  u8g2.drawStr(10, 10, "SMART AMBULANCE J2");

  // Restore normal drawing color
  u8g2.setDrawColor(1);

  // Line 2: Signal State
  u8g2.setFont(u8g2_font_7x14B_tf);
  if (signalName == "RED") {
    u8g2.drawStr(0, 29, "TRAFFIC: [RED] STOP");
  } else if (signalName == "YELLOW") {
    u8g2.drawStr(0, 29, "TRAFFIC: [YEL] CLEAR");
  } else if (signalName == "GREEN") {
    u8g2.drawStr(0, 29, "TRAFFIC: [GRN] GO-AMB");
  } else if (signalName == "SIREN") {
    u8g2.drawStr(0, 29, "SIREN: [WEE-WOO !]");
  } else {
    u8g2.drawStr(0, 29, "TRAFFIC: [ALL OFF]");
  }

  // Divider line
  u8g2.drawHLine(0, 33, 128);

  // Line 3 & 4: Network IP Info
  u8g2.setFont(u8g2_font_5x8_tf);
  u8g2.drawStr(0, 44, "AP IP : 192.168.4.1");
  
  String staLine = "STA IP: " + staIpStr;
  u8g2.drawStr(0, 53, staLine.c_str());

  // Line 5: District
  u8g2.setFont(u8g2_font_4x6_tf);
  u8g2.drawStr(0, 62, "KARUR MEDICAL CORRIDOR | BUZZER ON");

  u8g2.sendBuffer();
}

// Control the 3 Traffic Light LEDs directly (HIGH = ON, LOW = OFF)
void setTrafficLights(bool red, bool yellow, bool green) {
  digitalWrite(PIN_RED,    red    ? HIGH : LOW);
  digitalWrite(PIN_YELLOW, yellow ? HIGH : LOW);
  digitalWrite(PIN_GREEN,  green  ? HIGH : LOW);
}

void applySignal(String signal) {
  signal.toUpperCase();
  sirenActive = false;
  currentSignal = signal;

  // Immediately silence any ongoing siren tone
  noTone(PIN_BUZZER);
  digitalWrite(PIN_BUZZER, LOW);

  if (signal == "RED") {
    setTrafficLights(true, false, false);
    beepFeedback(80); // 1 short confirmation beep
  } else if (signal == "YELLOW") {
    setTrafficLights(false, true, false);
    beepFeedback(60);
  } else if (signal == "GREEN") {
    setTrafficLights(false, false, true);
    // Double beep for priority green confirmation
    beepFeedback(50);
    delay(40);
    beepFeedback(50);
  } else if (signal == "SIREN") {
    sirenActive = true;
  } else { // OFF
    setTrafficLights(false, false, false);
    currentSignal = "OFF";
    beepFeedback(100);
  }

  updateOled(currentSignal);
}

// Web Server Handlers (Instant HTTP 200, Zero-Lag Execution)
void handleRoot() {
  String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>";
  html += "<title>Traffic Signal & Siren Controller</title>";
  html += "<style>body{font-family:sans-serif; text-align:center; background:#0F172A; color:#F8FAFC; padding:20px;}";
  html += "h1{color:#38BDF8; font-size:20px;}";
  html += ".btn{display:block; width:85%; max-width:320px; margin:10px auto; padding:14px; font-size:16px; font-weight:bold; border-radius:10px; border:none; text-decoration:none; color:white; cursor:pointer;}";
  html += ".btn-red{background:#EF4444;} .btn-yellow{background:#F59E0B; color:black;} .btn-green{background:#10B981;} .btn-siren{background:#EC4899;} .btn-off{background:#475569;}";
  html += ".status{margin:16px auto; padding:12px; background:#1E293B; border-radius:8px; width:85%; max-width:320px; border:1px solid #334155;}";
  html += "</style><script>";
  html += "function sendCmd(cmd){fetch('/' + cmd).then(r=>location.reload());}";
  html += "</script></head><body>";
  html += "<h1>ESP32 Signal &amp; Siren Controller</h1>";
  html += "<div class='status'>Active Signal: <strong style='color:#38BDF8;'>" + currentSignal + "</strong><br>";
  html += "<small style='color:#94A3B8;'>Hardware: 3-LED Module + JLG1295 Buzzer + OLED</small></div>";
  html += "<button class='btn btn-red' onclick=\"sendCmd('red')\">RED LIGHT (STOP TRAFFIC)</button>";
  html += "<button class='btn btn-yellow' onclick=\"sendCmd('yellow')\">YELLOW LIGHT (CLEARANCE)</button>";
  html += "<button class='btn btn-green' onclick=\"sendCmd('green')\">GREEN LIGHT (AMBULANCE GO)</button>";
  html += "<button class='btn btn-siren' onclick=\"sendCmd('siren')\">AMBULANCE SIREN (WEE-WOO AUDIO)</button>";
  html += "<button class='btn btn-off' onclick=\"sendCmd('off')\">TURN OFF ALL LIGHTS</button>";
  html += "<p style='font-size:12px; color:#94A3B8; margin-top:20px;'>Karur Medical District | Corridor Node J2</p>";
  html += "</body></html>";
  server.send(200, "text/html", html);
}

// Unified /set handler (supports /set?color=RED, /set?color=GREEN, etc.)
void handleSet() {
  if (server.hasArg("color")) {
    String c = server.arg("color");
    applySignal(c);
    server.sendHeader("Access-Control-Allow-Origin", "*");
    server.send(200, "text/plain", "OK: " + currentSignal);
  } else {
    server.send(400, "text/plain", "Missing color parameter");
  }
}

// Direct Instant 200 Handlers (Fast Sub-millisecond Execution)
void handleRed()    { applySignal("RED");    server.sendHeader("Access-Control-Allow-Origin", "*"); server.send(200, "text/plain", "OK: RED"); }
void handleYellow() { applySignal("YELLOW"); server.sendHeader("Access-Control-Allow-Origin", "*"); server.send(200, "text/plain", "OK: YELLOW"); }
void handleGreen()  { applySignal("GREEN");  server.sendHeader("Access-Control-Allow-Origin", "*"); server.send(200, "text/plain", "OK: GREEN"); }
void handleBlue()   { applySignal("GREEN");  server.sendHeader("Access-Control-Allow-Origin", "*"); server.send(200, "text/plain", "OK: GREEN"); }
void handleSiren()  { applySignal("SIREN");  server.sendHeader("Access-Control-Allow-Origin", "*"); server.send(200, "text/plain", "OK: SIREN"); }
void handleOff()    { applySignal("OFF");    server.sendHeader("Access-Control-Allow-Origin", "*"); server.send(200, "text/plain", "OK: OFF"); }

void handleStatus() {
  String json = "{\"status\":\"online\",\"signal\":\"" + currentSignal + "\",\"ip\":\"" + WiFi.localIP().toString() + "\",\"ap_ip\":\"" + WiFi.softAPIP().toString() + "\",\"module\":\"3LED_BUZZER_OLED\"}";
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", json);
}

void setup() {
  Serial.begin(115200);
  delay(300);

  // 1. Initialize U8G2 OLED Display
  u8g2.begin();
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_7x14B_tf);
  u8g2.drawStr(6, 24, "TRAFFIC & SIREN J2");
  u8g2.setFont(u8g2_font_6x10_tf);
  u8g2.drawStr(14, 44, "Booting ESP32...");
  u8g2.drawStr(8, 58, "R-Y-G + Buzzer Active");
  u8g2.sendBuffer();

  // 2. Configure Hardware Pins as OUTPUT
  pinMode(PIN_RED, OUTPUT);
  pinMode(PIN_YELLOW, OUTPUT);
  pinMode(PIN_GREEN, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_ONBOARD_LED, OUTPUT);

  // Ensure buzzer starts silent
  noTone(PIN_BUZZER);
  digitalWrite(PIN_BUZZER, LOW);

  // 3. Hardware Self-Test: Cycle Red -> Yellow -> Green with short beeps
  digitalWrite(PIN_ONBOARD_LED, HIGH);
  setTrafficLights(true, false, false);  beepFeedback(40); delay(300); // Red
  setTrafficLights(false, true, false);  beepFeedback(40); delay(300); // Yellow
  setTrafficLights(false, false, true);  beepFeedback(60); delay(300); // Green
  setTrafficLights(false, false, false); delay(150); // Off

  // 4. Wi-Fi Dual Mode: AP + STA
  WiFi.mode(WIFI_AP_STA);
  WiFi.softAP(AP_SSID, AP_PASS);
  Serial.println("[Wi-Fi] SoftAP Started: " + String(AP_SSID));

  WiFi.begin(STA_SSID, STA_PASS);
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 10) {
    delay(300);
    digitalWrite(PIN_ONBOARD_LED, !digitalRead(PIN_ONBOARD_LED));
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    staIpStr = WiFi.localIP().toString();
    Serial.println("[Wi-Fi] Connected! STA IP: " + staIpStr);
    digitalWrite(PIN_ONBOARD_LED, HIGH);
  } else {
    staIpStr = "Offline (AP only)";
    Serial.println("[Wi-Fi] Hotspot not found. Running in standalone AP mode.");
    digitalWrite(PIN_ONBOARD_LED, LOW);
  }

  // 5. Initial State: GREEN (Ambulance Wave)
  applySignal("GREEN");

  // 6. Register Web Server Endpoints
  server.enableCORS(true);
  server.on("/", handleRoot);
  server.on("/set", handleSet);
  server.on("/red", handleRed);
  server.on("/yellow", handleYellow);
  server.on("/green", handleGreen);
  server.on("/blue", handleBlue);
  server.on("/siren", handleSiren);
  server.on("/off", handleOff);
  server.on("/status", handleStatus);

  server.begin();
  Serial.println("[HTTP] Server ready on http://192.168.4.1");
}

void loop() {
  server.handleClient();

  // Emergency Siren Mode: Authentic Two-Tone "Wee-Woo" (ட்வைன் ட்வைன்) Sound + Alternate Flashing
  if (sirenActive) {
    unsigned long currentMillis = millis();
    if (currentMillis - lastSirenToggle >= 260) {
      lastSirenToggle = currentMillis;
      sirenFlip = !sirenFlip;
      if (sirenFlip) {
        setTrafficLights(true, false, false); // RED LED ON
        tone(PIN_BUZZER, 960);                // High Tone (~960 Hz) "WEE"
      } else {
        setTrafficLights(false, false, true); // GREEN LED ON
        tone(PIN_BUZZER, 680);                // Low Tone (~680 Hz) "WOO"
      }
    }
  }
}
