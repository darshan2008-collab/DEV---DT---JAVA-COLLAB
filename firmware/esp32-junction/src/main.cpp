/*
 * AI-Driven Smart Ambulance Navigation - ESP32 Miniature Junction Firmware
 * Hardware Signal Priority Controller
 * M. Kumarasamy College of Engineering, Karur - B.Tech AI & Data Science
 *
 * Safe Phase State Machine:
 * 1. Conflicting direction (EW) Yellow (3s)
 * 2. All-Red Clearance (2s) - CRITICAL: Never two greens simultaneously!
 * 3. Green Priority for Ambulance Direction (NS) (8s)
 * 4. Normal Fixed-Time Alternating Cycle
 *
 * Manual Override Button on GPIO 34 instantly cancels priority.
 */

#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// Wi-Fi and MQTT Configuration
const char* WIFI_SSID = "SMART_AMBULANCE_WIFI";
const char* WIFI_PASS = "emergency_priority_2026";
const char* MQTT_SERVER = "192.168.1.100";
const int   MQTT_PORT   = 1883;
const char* JUNCTION_ID = "J2";

// Pin Assignments
// North-South (Ambulance Approach)
#define NS_RED_PIN    18
#define NS_YELLOW_PIN 19
#define NS_GREEN_PIN  21

// East-West (Cross Traffic)
#define EW_RED_PIN    22
#define EW_YELLOW_PIN 23
#define EW_GREEN_PIN  25

// Manual Override Button (Active LOW with internal pull-up)
#define OVERRIDE_BTN_PIN 34

WiFiClient espClient;
PubSubClient client(espClient);

enum SignalPhase {
  NORMAL_CYCLE_EW_GREEN,
  NORMAL_CYCLE_NS_GREEN,
  PRIORITY_YELLOW_CLEARANCE,
  PRIORITY_ALL_RED,
  PRIORITY_GREEN,
  MANUAL_OVERRIDE
};

SignalPhase currentPhase = NORMAL_CYCLE_EW_GREEN;
unsigned long lastPhaseChange = 0;
bool priorityRequested = false;
String priorityAmbulanceId = "";

void setLeds(bool nsR, bool nsY, bool nsG, bool ewR, bool ewY, bool ewG) {
  digitalWrite(NS_RED_PIN,    nsR ? HIGH : LOW);
  digitalWrite(NS_YELLOW_PIN, nsY ? HIGH : LOW);
  digitalWrite(NS_GREEN_PIN,  nsG ? HIGH : LOW);
  digitalWrite(EW_RED_PIN,    ewR ? HIGH : LOW);
  digitalWrite(EW_YELLOW_PIN, ewY ? HIGH : LOW);
  digitalWrite(EW_GREEN_PIN,  ewG ? HIGH : LOW);
}

void publishState(const char* stateName, const char* reason) {
  StaticJsonDocument<200> doc;
  doc["junction_id"] = JUNCTION_ID;
  doc["state"] = stateName;
  doc["reason"] = reason;
  doc["timestamp"] = millis();

  char buffer[256];
  serializeJson(doc, buffer);
  client.publish("junction/J2/state", buffer);
}

void onMqttMessage(char* topic, byte* message, unsigned int length) {
  String msgStr;
  for (unsigned int i = 0; i < length; i++) {
    msgStr += (char)message[i];
  }

  if (String(topic).endsWith("/priority/request")) {
    StaticJsonDocument<256> doc;
    DeserializationError error = deserializeJson(doc, msgStr);
    if (!error) {
      priorityAmbulanceId = doc["ambulance_id"] | "AMB-01";
      priorityRequested = true;

      // Publish ACK
      StaticJsonDocument<128> ack;
      ack["junction_id"] = JUNCTION_ID;
      ack["status"] = "ACCEPTED";
      char ackBuf[128];
      serializeJson(ack, ackBuf);
      client.publish("junction/J2/ack", ackBuf);

      // Transition to Yellow clearance phase
      currentPhase = PRIORITY_YELLOW_CLEARANCE;
      lastPhaseChange = millis();
      setLeds(true, false, false, false, true, false); // NS Red, EW Yellow
      publishState("YELLOW_CLEARANCE", "Cross-traffic yellow clearance for ambulance arrival");
    }
  } else if (String(topic).endsWith("/override")) {
    currentPhase = MANUAL_OVERRIDE;
    publishState("OVERRIDDEN", "Manual override activated from MQTT");
  }
}

void reconnectMqtt() {
  while (!client.connected()) {
    if (client.connect("ESP32_Junction_J2")) {
      client.subscribe("junction/J2/priority/request");
      client.subscribe("junction/J2/override");
      publishState("NORMAL_CYCLE", "Junction online and operational");
    } else {
      delay(2000);
    }
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(NS_RED_PIN, OUTPUT);
  pinMode(NS_YELLOW_PIN, OUTPUT);
  pinMode(NS_GREEN_PIN, OUTPUT);
  pinMode(EW_RED_PIN, OUTPUT);
  pinMode(EW_YELLOW_PIN, OUTPUT);
  pinMode(EW_GREEN_PIN, OUTPUT);
  pinMode(OVERRIDE_BTN_PIN, INPUT_PULLUP);

  // Initial State: Safe All-Red for 1s
  setLeds(true, false, false, true, false, false);
  delay(1000);

  WiFi.begin(WIFI_SSID, WIFI_PASS);
  client.setServer(MQTT_SERVER, MQTT_PORT);
  client.setCallback(onMqttMessage);

  lastPhaseChange = millis();
}

void loop() {
  if (WiFi.status() == WL_CONNECTED && !client.connected()) {
    reconnectMqtt();
  }
  client.loop();

  // Check physical manual override button
  if (digitalRead(OVERRIDE_BTN_PIN) == LOW) {
    currentPhase = MANUAL_OVERRIDE;
  }

  unsigned long now = millis();

  switch (currentPhase) {
    case PRIORITY_YELLOW_CLEARANCE:
      if (now - lastPhaseChange >= 3000) { // 3s yellow clearance
        currentPhase = PRIORITY_ALL_RED;
        lastPhaseChange = now;
        setLeds(true, false, false, true, false, false); // ALL RED
        publishState("ALL_RED", "All-red safe intersection clearance");
      }
      break;

    case PRIORITY_ALL_RED:
      if (now - lastPhaseChange >= 2000) { // 2s all-red clearance
        currentPhase = PRIORITY_GREEN;
        lastPhaseChange = now;
        setLeds(false, false, true, true, false, false); // NS Green, EW Red
        publishState("GREEN_PRIORITY", "Green priority granted to ambulance direction");
      }
      break;

    case PRIORITY_GREEN:
      if (now - lastPhaseChange >= 8000) { // 8s priority green hold
        priorityRequested = false;
        currentPhase = NORMAL_CYCLE_EW_GREEN;
        lastPhaseChange = now;
        setLeds(true, false, false, false, false, true); // EW Green, NS Red
        publishState("NORMAL_CYCLE", "Priority complete. Resuming normal cycling.");
      }
      break;

    case MANUAL_OVERRIDE:
      // Return to normal fixed cycle immediately
      priorityRequested = false;
      currentPhase = NORMAL_CYCLE_EW_GREEN;
      lastPhaseChange = now;
      setLeds(true, false, false, false, false, true);
      publishState("NORMAL_CYCLE", "Manual override reset junction to normal cycle");
      delay(500); // debounce
      break;

    case NORMAL_CYCLE_EW_GREEN:
      if (!priorityRequested && now - lastPhaseChange >= 10000) { // 10s EW green
        currentPhase = NORMAL_CYCLE_NS_GREEN;
        lastPhaseChange = now;
        setLeds(false, false, true, true, false, false); // NS Green, EW Red
      }
      break;

    case NORMAL_CYCLE_NS_GREEN:
      if (!priorityRequested && now - lastPhaseChange >= 10000) { // 10s NS green
        currentPhase = NORMAL_CYCLE_EW_GREEN;
        lastPhaseChange = now;
        setLeds(true, false, false, false, false, true); // EW Green, NS Red
      }
      break;
  }
}
