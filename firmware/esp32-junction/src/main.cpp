/*
 * =========================================================================
 * Smart Ambulance Navigation System - ESP32 Signal & RGB LED Controller
 * Dual Mode: Access Point (AP) + Station (Hotspot Client)
 * =========================================================================
 */

#include <WiFi.h>
#include <WebServer.h>

// --- Wi-Fi Configuration ---
// 1. Mobile Hotspot Details (Station Mode)
const char* STA_SSID = "DARSHAN K";
const char* STA_PASS = "12345678";

// 2. ESP32's Own Wi-Fi Hotspot (SoftAP Mode - ALWAYS VISIBLE!)
const char* AP_SSID  = "ESP32-AMBULANCE-SIGNAL";
const char* AP_PASS  = "12345678";

// --- Hardware Pins ---
#define PIN_RED    18
#define PIN_GREEN  19
#define PIN_BLUE   23
#define PIN_ONBOARD_LED 2

WebServer server(80);

// State tracking
String currentColor = "GREEN";
bool sirenActive = false;
unsigned long lastSirenToggle = 0;
bool sirenFlip = false;

// Function to set RGB LED colors (Common Cathode: HIGH = ON, LOW = OFF)
void setRGB(int r, int g, int b) {
  analogWrite(PIN_RED,   r);
  analogWrite(PIN_GREEN, g);
  analogWrite(PIN_BLUE,  b);
}

void applyColor(String color) {
  sirenActive = false;
  currentColor = color;
  
  if (color == "RED") {
    setRGB(255, 0, 0);
  } else if (color == "YELLOW") {
    setRGB(255, 120, 0); // Red + Green = Yellow
  } else if (color == "GREEN") {
    setRGB(0, 255, 0);
  } else if (color == "BLUE") {
    setRGB(0, 0, 255);
  } else if (color == "SIREN") {
    sirenActive = true;
  } else { // OFF
    setRGB(0, 0, 0);
    currentColor = "OFF";
  }
}

// Web Server Handlers
void handleRoot() {
  String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>";
  html += "<title>ESP32 Ambulance Signal Head</title>";
  html += "<style>body{font-family:sans-serif; text-align:center; background:#0F172A; color:#F8FAFC; padding:20px;}";
  html += "h1{color:#38BDF8; font-size:22px;}";
  html += ".btn{display:block; width:85%; max-width:320px; margin:12px auto; padding:15px; font-size:18px; font-weight:bold; border-radius:10px; border:none; text-decoration:none; color:white;}";
  html += ".btn-red{background:#EF4444;} .btn-yellow{background:#F59E0B; color:black;} .btn-green{background:#10B981;} .btn-blue{background:#0284C7;} .btn-siren{background:#EC4899;} .btn-off{background:#475569;}";
  html += ".status{margin:20px auto; padding:14px; background:#1E293B; border-radius:8px; width:85%; max-width:320px; border:1px solid #334155;}";
  html += "</style></head><body>";
  html += "<h1>ESP32 Signal Head Controller</h1>";
  html += "<div class='status'>Active Status: <strong style='color:#38BDF8;'>" + currentColor + "</strong></div>";
  html += "<a class='btn btn-red' href='/red'>RED LIGHT (STOP)</a>";
  html += "<a class='btn btn-yellow' href='/yellow'>YELLOW LIGHT (PREEMPT)</a>";
  html += "<a class='btn btn-green' href='/green'>GREEN LIGHT (CLEAR)</a>";
  html += "<a class='btn btn-blue' href='/blue'>BLUE LIGHT</a>";
  html += "<a class='btn btn-siren' href='/siren'>AMBULANCE SIREN FLASH</a>";
  html += "<a class='btn btn-off' href='/off'>TURN OFF ALL LIGHTS</a>";
  html += "<p style='font-size:12px; color:#94A3B8; margin-top:25px;'>Smart Ambulance Corridor Priority System</p>";
  html += "</body></html>";
  server.send(200, "text/html", html);
}

void handleRed() {
  applyColor("RED");
  server.sendHeader("Location", "/");
  server.send(303);
}

void handleYellow() {
  applyColor("YELLOW");
  server.sendHeader("Location", "/");
  server.send(303);
}

void handleGreen() {
  applyColor("GREEN");
  server.sendHeader("Location", "/");
  server.send(303);
}

void handleBlue() {
  applyColor("BLUE");
  server.sendHeader("Location", "/");
  server.send(303);
}

void handleSiren() {
  applyColor("SIREN");
  server.sendHeader("Location", "/");
  server.send(303);
}

void handleOff() {
  applyColor("OFF");
  server.sendHeader("Location", "/");
  server.send(303);
}

void handleStatus() {
  String json = "{\"status\":\"online\",\"color\":\"" + currentColor + "\",\"ip\":\"" + WiFi.localIP().toString() + "\",\"ap_ip\":\"" + WiFi.softAPIP().toString() + "\"}";
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", json);
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n\n========================================");
  Serial.println("  ESP32 SMART AMBULANCE SIGNAL HEAD");
  Serial.println("========================================");

  // Configure Pins
  pinMode(PIN_RED, OUTPUT);
  pinMode(PIN_GREEN, OUTPUT);
  pinMode(PIN_BLUE, OUTPUT);
  pinMode(PIN_ONBOARD_LED, OUTPUT);
  
  // Test sequence: Blink onboard LED and cycle RGB colors
  digitalWrite(PIN_ONBOARD_LED, HIGH);
  setRGB(255, 0, 0); delay(400);
  setRGB(0, 255, 0); delay(400);
  setRGB(0, 0, 255); delay(400);
  setRGB(0, 0, 0);   delay(200);

  // Set Wi-Fi Mode to DUAL (AP + STA)
  WiFi.mode(WIFI_AP_STA);

  // 1. Start ESP32's OWN Wi-Fi Hotspot (Visible to all phones & laptops!)
  Serial.println("[Wi-Fi] Starting ESP32 Hotspot (AP)...");
  WiFi.softAP(AP_SSID, AP_PASS);
  Serial.print("[Wi-Fi] Broadcast SSID: ");
  Serial.println(AP_SSID);
  Serial.print("[Wi-Fi] AP IP Address:  http://");
  Serial.println(WiFi.softAPIP());

  // 2. Connect to Mobile Hotspot 'DARSHAN K'
  Serial.print("[Wi-Fi] Connecting to Mobile Hotspot '");
  Serial.print(STA_SSID);
  Serial.println("' (Must be 2.4 GHz)...");
  
  WiFi.begin(STA_SSID, STA_PASS);

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 15) {
    delay(500);
    Serial.print(".");
    digitalWrite(PIN_ONBOARD_LED, !digitalRead(PIN_ONBOARD_LED));
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[Wi-Fi] Connected to Mobile Hotspot successfully!");
    Serial.print("[Wi-Fi] Mobile Hotspot IP: http://");
    Serial.println(WiFi.localIP());
    digitalWrite(PIN_ONBOARD_LED, HIGH);
  } else {
    Serial.println("\n[Wi-Fi] Mobile Hotspot not reached. Continuing in Standalone AP Mode.");
    Serial.println("[Wi-Fi] Tip: Ensure Mobile Hotspot 'AP Band' is set to 2.4 GHz!");
    digitalWrite(PIN_ONBOARD_LED, LOW);
  }

  // Set default initial light: GREEN
  applyColor("GREEN");

  // Web Server Endpoints
  server.on("/", handleRoot);
  server.on("/red", handleRed);
  server.on("/yellow", handleYellow);
  server.on("/green", handleGreen);
  server.on("/blue", handleBlue);
  server.on("/siren", handleSiren);
  server.on("/off", handleOff);
  server.on("/status", handleStatus);

  server.begin();
  Serial.println("[HTTP] Web Server Started!");
  Serial.println("========================================\n");
}

void loop() {
  server.handleClient();

  // Emergency Siren Animation (Red & Blue Alternate Flashing)
  if (sirenActive) {
    unsigned long currentMillis = millis();
    if (currentMillis - lastSirenToggle >= 120) {
      lastSirenToggle = currentMillis;
      sirenFlip = !sirenFlip;
      if (sirenFlip) {
        setRGB(255, 0, 0); // RED
      } else {
        setRGB(0, 0, 255); // BLUE
      }
    }
  }
}
