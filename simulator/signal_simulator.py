"""
AI-Driven Smart Ambulance Navigation - Software Traffic Signal Simulator
Replicates the ESP32 Miniature Traffic Junction Firmware
Topics:
  - Subscribes: junction/{id}/priority/request, junction/{id}/override
  - Publishes: junction/{id}/state, junction/{id}/ack
Safe Phase Sequence:
  Conflicting direction (EW) Yellow -> All-Red Clearance -> Priority Green (NS) -> Normal Cycle
  Never displays green to conflicting directions!
"""

import json
import time
import logging
from datetime import datetime, timezone
import paho.mqtt.client as mqtt

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] [SIGNAL-SIM] %(message)s")
logger = logging.getLogger("SignalSimulator")

MQTT_BROKER = "localhost"
MQTT_PORT = 1883
JUNCTIONS = ["J1", "J2", "J3", "H"]


class JunctionController:
    def __init__(self, junction_id: str):
        self.junction_id = junction_id
        self.state = "NORMAL_CYCLE"  # NORMAL_CYCLE, YELLOW_CLEARANCE, ALL_RED, GREEN_PRIORITY, OVERRIDDEN
        self.active_direction = "EAST_WEST"
        self.ambulance_direction = "NORTH_SOUTH"
        self.in_priority_phase = False

    def handle_priority_request(self, payload: dict, client: mqtt.Client):
        logger.info("[%s] Received priority request: %s", self.junction_id, payload)
        ambulance_id = payload.get("ambulance_id", "AMB-01")
        req_direction = payload.get("direction", "NORTH_SOUTH")
        trip_id = payload.get("trip_id", "")

        # Acknowledge request
        ack_payload = {
            "junction_id": self.junction_id,
            "status": "ACCEPTED",
            "message": f"Priority sequence initiated for {ambulance_id}",
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        client.publish(f"junction/{self.junction_id}/ack", json.dumps(ack_payload))

        # Safe Phase Sequence:
        # Phase 1: Conflicting direction (East-West) gets Yellow clearance
        self.state = "YELLOW_CLEARANCE"
        self.publish_state(client, "Cross-traffic yellow clearance active (3s)")
        time.sleep(3)

        # Phase 2: All-Red clearance (CRITICAL SAFETY RULE: Never green to two conflicting directions!)
        self.state = "ALL_RED"
        self.publish_state(client, "All-red intersection clearance active (2s)")
        time.sleep(2)

        # Phase 3: Green Priority granted to ambulance direction
        self.state = "GREEN_PRIORITY"
        self.active_direction = req_direction
        self.publish_state(client, f"GREEN PRIORITY active for {ambulance_id} ({req_direction})")
        time.sleep(6)  # Hold time for ambulance passage

        # Phase 4: Resume normal cycling
        self.state = "NORMAL_CYCLE"
        self.active_direction = "EAST_WEST"
        self.publish_state(client, "Ambulance cleared junction. Normal cycling resumed.")

    def manual_override(self, client: mqtt.Client):
        logger.info("[%s] MANUAL OVERRIDE TRIGGERED!", self.junction_id)
        self.state = "NORMAL_CYCLE"
        self.active_direction = "EAST_WEST"
        self.publish_state(client, "Manual override executed. Signal immediately returned to standard fixed cycle.")

    def publish_state(self, client: mqtt.Client, reason: str):
        payload = {
            "junction_id": self.junction_id,
            "state": self.state,
            "active_green_direction": self.active_direction,
            "reason": reason,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        client.publish(f"junction/{self.junction_id}/state", json.dumps(payload))
        logger.info("[%s] State -> %s (%s)", self.junction_id, self.state, reason)


junction_controllers = {jid: JunctionController(jid) for jid in JUNCTIONS}


def on_connect(client, userdata, flags, rc):
    if rc == 0:
        logger.info("Connected to MQTT broker at %s:%d", MQTT_BROKER, MQTT_PORT)
        for jid in JUNCTIONS:
            client.subscribe(f"junction/{jid}/priority/request")
            client.subscribe(f"junction/{jid}/override")
            # Publish initial normal state
            junction_controllers[jid].publish_state(client, "Initial system start")
    else:
        logger.warning("Failed to connect to MQTT broker, return code %d", rc)


def on_message(client, userdata, msg):
    try:
        topic = msg.topic
        payload = json.loads(msg.payload.decode())
        parts = topic.split("/")
        junction_id = parts[1]
        action = parts[2]

        if junction_id in junction_controllers:
            ctrl = junction_controllers[junction_id]
            if action == "priority":
                ctrl.handle_priority_request(payload, client)
            elif action == "override":
                ctrl.manual_override(client)
    except Exception as e:
        logger.error("Error processing MQTT message: %s", e)


def run_simulator():
    client = mqtt.Client(client_id="smart-ambulance-software-signal-sim")
    client.on_connect = on_connect
    client.on_message = on_message

    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
        logger.info("Signal simulator running. Listening for priority requests on junction/#...")
        client.loop_forever()
    except Exception as e:
        logger.warning("MQTT broker unavailable (%s). Software simulator idle.", e)
        while True:
            time.sleep(5)


if __name__ == "__main__":
    run_simulator()
