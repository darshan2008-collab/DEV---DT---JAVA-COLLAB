/*
 * =========================================================================
 * Smart Ambulance Navigation System - ESP32 Signal & RGB LED Controller
 * with I2C 0.96" OLED Display (SSD1306) using U8G2 Library
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

// --- Hardware Pins ---
#define PIN_RED    18
#define PIN_GREEN  19
#define PIN_BLUE   23
#define PIN_ONBOARD_LED 2

bool isCommonAnode = false;

WebServer server(80);

String currentColor = "GREEN";
bool sirenActive = false;
unsigned long lastSirenToggle = 0;
bool sirenFlip = false;
String staIpStr = "Connecting...";

void updateOled(String statusText) {
  u8g2.clearBuffer();

  u8g2.setDrawColor(1);
  u8g2.drawRBox(0, 0, 128, 14, 2);
  u8g2.setDrawColor(0);
  u8g2.setFont(u8g2_font_6x10_tf);
  u8g2.drawStr(12, 10, "SMART AMBULANCE");

  u8g2.setDrawColor(1);

  u8g2.setFont(u8g2_font_7x14B_tf);
  if (statusText == "RED") {
    u8g2.drawStr(0, 29, "SIG: [RED] STOP");
  } else if (statusText == "YELLOW") {
    u8g2.drawStr(0, 29, "SIG: [YEL] CLEAR");
  } else if (statusText == "GREEN") {
    u8g2.drawStr(0, 29, "SIG: [GRN] WAVE");
  } else if (statusText == "BLUE") {
    u8g2.drawStr(0, 29, "SIG: [BLU] READY");
  } else if (statusText == "SIREN") {
    u8g2.drawStr(0, 29, "SIG: [SIREN] !!!");
  } else {
    u8g2.drawStr(0, 29, "SIG: [OFF]");
  }

  u8g2.drawHLine(0, 33, 128);

  u8g2.setFont(u8g2_font_5x8_tf);
  u8g2.drawStr(0, 44, "AP IP : 192.168.4.1");
  
  String staLine = "STA IP: " + staIpStr;
  u8g2.drawStr(0, 53, staLine.c_str());

  u8g2.setFont(u8g2_font_4x6_tf);
  u8g2.drawStr(0, 62, "J2 NODE | 100% PREEMPTION ACTIVE");

  u8g2.sendBuffer();
}

void setRGB(bool r, bool g, bool b) {
  if (isCommonAnode) {
    digitalWrite(PIN_RED,   r ? LOW : HIGH);
    digitalWrite(PIN_GREEN, g ? LOW : HIGH);
    digitalWrite(PIN_BLUE,  b ? LOW : HIGH);
  } else {
    digitalWrite(PIN_RED,   r ? HIGH : LOW);
    digitalWrite(PIN_GREEN, g ? HIGH : LOW);
    digitalWrite(PIN_BLUE,  b ? HIGH : LOW);
  }
}

void applyColor(String color) {
  color.toUpperCase();
  sirenActive = false;
  currentColor = color;
  
  if (color == "RED") {
    setRGB(true, false, false);
  } else if (color == "YELLOW") {
    setRGB(true, true, false);
  } else if (color == "GREEN") {
    setRGB(false, true, false);
  } else if (color == "BLUE") {
    setRGB(false, false, true);
  } else if (color == "SIREN") {
    sirenActive = true;
  } else {
    setRGB(false, false, false);
    currentColor = "OFF";
  }

  updateOled(currentColor);
}

void handleRoot() {
  String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>";
  html += "<title>ESP32 Ambulance Signal Head</title>";
  html += "<style>body{font-family:sans-serif; text-align:center; background:#0F172A; color:#F8FAFC; padding:20px;}";
  html += "h1{color:#38BDF8; font-size:22px;}";
  html += ".btn{display:block; width:85%; max-width:320px; margin:10px auto; padding:14px; font-size:16px; font-weight:bold; border-radius:10px; border:none; text-decoration:none; color:white;}";
  html += ".btn-red{background:#EF4444;} .btn-yellow{background:#F59E0B; color:black;} .btn-green{background:#10B981;} .btn-blue{background:#0284C7;} .btn-siren{background:#EC4899;} .btn-off{background:#475569;}";
  html += ".status{margin:16px auto; padding:12px; background:#1E293B; border-radius:8px; width:85%; max-width:320px; border:1px solid #334155;}";
  html += ".toggle-box{margin:10px auto; padding:8px; font-size:12px; color:#94A3B8;}";
  html += "</style></head><body>";
  html += "<h1>ESP32 Signal & OLED Controller</h1>";
  html += "<div class='status'>Active Light: <strong style='color:#38BDF8;'>" + currentColor + "</strong><br>";
  html += "<small style='color:#94A3B8;'>LED Mode: " + String(isCommonAnode ? "Common Anode (3.3V)" : "Common Cathode (GND)") + "</small></div>";
  html += "<a class='btn btn-red' href='/red'>RED LIGHT (STOP)</a>";
  html += "<a class='btn btn-yellow' href='/yellow'>YELLOW LIGHT (PREEMPT)</a>";
  html += "<a class='btn btn-green' href='/green'>GREEN LIGHT (CLEAR)</a>";
  html += "<a class='btn btn-blue' href='/blue'>BLUE LIGHT</a>";
  html += "<a class='btn btn-siren' href='/siren'>AMBULANCE SIREN FLASH</a>";
  html += "<a class='btn btn-off' href='/off'>TURN OFF ALL LIGHTS</a>";
  html += "<div class='toggle-box'><a style='color:#38BDF8;' href='/toggle_mode'>Switch LED Mode (Cathode/Anode)</a></div>";
  html += "<p style='font-size:12px; color:#94A3B8; margin-top:20px;'>Karur Medical District | Preemption Node</p>";
  html += "</body></html>";
  server.send(200, "text/html", html);
}

void handleSet() {
  if (server.hasArg("color")) {
    String c = server.arg("color");
    applyColor(c);
    server.sendHeader("Access-Control-Allow-Origin", "*");
    server.send(200, "text/plain", "OK: " + currentColor);
  } else {
    server.send(400, "text/plain", "Missing color parameter");
  }
}

void handleRed()    { applyColor("RED");    server.sendHeader("Location", "/"); server.send(303); }
void handleYellow() { applyColor("YELLOW"); server.sendHeader("Location", "/"); server.send(303); }
void handleGreen()  { applyColor("GREEN");  server.sendHeader("Location", "/"); server.send(303); }
void handleBlue()   { applyColor("BLUE");   server.sendHeader("Location", "/"); server.send(303); }
void handleSiren()  { applyColor("SIREN");  server.sendHeader("Location", "/"); server.send(303); }
void handleOff()    { applyColor("OFF");    server.sendHeader("Location", "/"); server.send(303); }

void handleToggleMode() {
  isCommonAnode = !isCommonAnode;
  applyColor(currentColor);
  server.sendHeader("Location", "/");
  server.send(303);
}

void handleStatus() {
  String json = "{\"status\":\"online\",\"color\":\"" + currentColor + "\",\"ip\":\"" + WiFi.localIP().toString() + "\",\"ap_ip\":\"" + WiFi.softAPIP().toString() + "\",\"led_mode\":\"" + String(isCommonAnode ? "ANODE" : "CATHODE") + "\"}";
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", json);
}

void setup() {
  Serial.begin(115200);
  delay(500);

  u8g2.begin();
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_7x14B_tf);
  u8g2.drawStr(10, 24, "SMART AMBULANCE");
  u8g2.setFont(u8g2_font_6x10_tf);
  u8g2.drawStr(16, 44, "Booting ESP32...");
  u8g2.drawStr(22, 58, "OLED (U8G2)");
  u8g2.sendBuffer();

  pinMode(PIN_RED, OUTPUT);
  pinMode(PIN_GREEN, OUTPUT);
  pinMode(PIN_BLUE, OUTPUT);
  pinMode(PIN_ONBOARD_LED, OUTPUT);
  
  digitalWrite(PIN_ONBOARD_LED, HIGH);
  setRGB(true, false, false); delay(400);
  setRGB(false, true, false); delay(400);
  setRGB(false, false, true); delay(400);
  setRGB(false, false, false);delay(200);

  WiFi.mode(WIFI_AP_STA);
  WiFi.softAP(AP_SSID, AP_PASS);

  WiFi.begin(STA_SSID, STA_PASS);
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 12) {
    delay(400);
    digitalWrite(PIN_ONBOARD_LED, !digitalRead(PIN_ONBOARD_LED));
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    staIpStr = WiFi.localIP().toString();
    digitalWrite(PIN_ONBOARD_LED, HIGH);
  } else {
    staIpStr = "Offline (AP only)";
    digitalWrite(PIN_ONBOARD_LED, LOW);
  }

  applyColor("GREEN");

  server.on("/", handleRoot);
  server.on("/set", handleSet);
  server.on("/red", handleRed);
  server.on("/yellow", handleYellow);
  server.on("/green", handleGreen);
  server.on("/blue", handleBlue);
  server.on("/siren", handleSiren);
  server.on("/off", handleOff);
  server.on("/toggle_mode", handleToggleMode);
  server.on("/status", handleStatus);

  server.begin();
}

void loop() {
  server.handleClient();

  if (sirenActive) {
    unsigned long currentMillis = millis();
    if (currentMillis - lastSirenToggle >= 120) {
      lastSirenToggle = currentMillis;
      sirenFlip = !sirenFlip;
      if (sirenFlip) {
        setRGB(true, false, false);
      } else {
        setRGB(false, false, true);
      }
    }
  }
}
