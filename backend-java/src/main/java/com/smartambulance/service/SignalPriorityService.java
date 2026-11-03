package com.smartambulance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartambulance.domain.SignalEvent;
import com.smartambulance.domain.SignalState;
import com.smartambulance.mqtt.MqttPriorityPublisher;
import com.smartambulance.repository.SignalEventRepository;
import com.smartambulance.websocket.LiveEvent;
import com.smartambulance.websocket.LiveEventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SignalPriorityService {

    private static final Logger log = LoggerFactory.getLogger(SignalPriorityService.class);

    @Autowired
    private SignalEventRepository signalEventRepository;

    @Autowired
    private MqttPriorityPublisher mqttPublisher;

    @Autowired
    private LiveEventBroadcaster liveEventBroadcaster;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory junction state tracking
    private final Map<String, SignalState> junctionStates = new ConcurrentHashMap<>();
    private final Map<String, String> currentActiveDirection = new ConcurrentHashMap<>();

    public SignalPriorityService() {
        // Initialize default states for demo junctions
        junctionStates.put("J1", SignalState.NORMAL_CYCLE);
        junctionStates.put("J2", SignalState.NORMAL_CYCLE);
        junctionStates.put("J3", SignalState.NORMAL_CYCLE);
        junctionStates.put("H", SignalState.NORMAL_CYCLE);

        currentActiveDirection.put("J1", "EAST_WEST");
        currentActiveDirection.put("J2", "EAST_WEST");
        currentActiveDirection.put("J3", "EAST_WEST");
        currentActiveDirection.put("H", "EAST_WEST");
    }

    public SignalState getJunctionState(String junctionId) {
        return junctionStates.getOrDefault(junctionId, SignalState.NORMAL_CYCLE);
    }

    public Map<String, Object> getFullJunctionStatus(String junctionId) {
        Map<String, Object> res = new HashMap<>();
        res.put("junction_id", junctionId);
        res.put("state", junctionStates.getOrDefault(junctionId, SignalState.NORMAL_CYCLE));
        res.put("active_direction", currentActiveDirection.getOrDefault(junctionId, "EAST_WEST"));
        res.put("timestamp", Instant.now().toString());
        return res;
    }

    @Transactional
    public SignalEvent requestPriority(String junctionId, String direction, String ambulanceId, String tripId) {
        log.info("Signal priority requested for junction {} direction {} for ambulance {}", junctionId, direction, ambulanceId);

        // Record initial event
        SignalEvent event = new SignalEvent(junctionId, direction, ambulanceId, tripId, SignalState.REQUESTED, true);
        SignalEvent saved = signalEventRepository.save(event);

        // Publish to MQTT
        Map<String, Object> payload = new HashMap<>();
        payload.put("junction_id", junctionId);
        payload.put("direction", direction);
        payload.put("ambulance_id", ambulanceId);
        payload.put("trip_id", tripId);
        payload.put("timestamp", Instant.now().toString());
        payload.put("expiry_seconds", 30);

        try {
            String json = objectMapper.writeValueAsString(payload);
            mqttPublisher.publish("junction/" + junctionId + "/priority/request", json);
        } catch (Exception e) {
            log.warn("Failed to serialize MQTT priority request: {}", e.getMessage());
        }

        // Execute safe phase transition sequence asynchronously
        executeSafePhaseSequence(junctionId, direction, ambulanceId, tripId);

        return saved;
    }

    @org.springframework.beans.factory.annotation.Value("${smartambulance.signal.fast-mode:true}")
    private boolean fastMode;

    @Async
    public void executeSafePhaseSequence(String junctionId, String direction, String ambulanceId, String tripId) {
        try {
            long yellowMs = fastMode ? 100 : 3000;
            long redMs = fastMode ? 50 : 2000;
            long greenMs = fastMode ? 200 : 8000;

            // Step 1: Conflicting direction (East-West) gets YELLOW clearance
            junctionStates.put(junctionId, SignalState.YELLOW_CLEARANCE);
            broadcastSignalState(junctionId, SignalState.YELLOW_CLEARANCE, direction, ambulanceId, tripId, "Cross-traffic yellow clearance phase active (3s)");
            Thread.sleep(yellowMs);

            // Step 2: ALL_RED clearance (minimum clearance time: never two conflicting greens!)
            junctionStates.put(junctionId, SignalState.ALL_RED);
            broadcastSignalState(junctionId, SignalState.ALL_RED, direction, ambulanceId, tripId, "Safe intersection all-red clearance phase active (2s)");
            Thread.sleep(redMs);

            // Step 3: GREEN_PRIORITY granted to ambulance direction
            junctionStates.put(junctionId, SignalState.GREEN_PRIORITY);
            currentActiveDirection.put(junctionId, direction);
            broadcastSignalState(junctionId, SignalState.GREEN_PRIORITY, direction, ambulanceId, tripId, "GREEN PRIORITY GRANTED to " + direction + " for ambulance " + ambulanceId);
            Thread.sleep(greenMs);

            // Step 4: Resume NORMAL_CYCLE safely
            junctionStates.put(junctionId, SignalState.NORMAL_CYCLE);
            currentActiveDirection.put(junctionId, "EAST_WEST");
            broadcastSignalState(junctionId, SignalState.NORMAL_CYCLE, direction, ambulanceId, tripId, "Ambulance cleared intersection. Normal cycle resumed.");

        } catch (InterruptedException e) {
            log.warn("Signal priority sequence interrupted for junction {}", junctionId);
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public void manualOverride(String junctionId) {
        junctionStates.put(junctionId, SignalState.OVERRIDDEN);
        currentActiveDirection.put(junctionId, "NORMAL_CYCLE");

        log.info("Manual override triggered for junction {}", junctionId);

        mqttPublisher.publish("junction/" + junctionId + "/override", "{\"action\":\"OVERRIDE\",\"timestamp\":\"" + Instant.now() + "\"}");

        broadcastSignalState(junctionId, SignalState.OVERRIDDEN, "MANUAL", "OPERATOR", null, "Manual override active. Junction returned to standard fixed cycle.");

        // Return to normal cycle immediately
        junctionStates.put(junctionId, SignalState.NORMAL_CYCLE);
    }

    private void broadcastSignalState(String junctionId, SignalState state, String direction, String ambulanceId, String tripId, String reason) {
        // Save history
        SignalEvent event = new SignalEvent(junctionId, direction, ambulanceId != null ? ambulanceId : "NONE", tripId, state, true);
        signalEventRepository.save(event);

        // Broadcast to WebSocket clients
        liveEventBroadcaster.broadcast(new LiveEvent(
                "SIGNAL_STATE",
                tripId,
                junctionId,
                state.name(),
                null,
                null,
                reason,
                0L,
                "SIGNAL_CONTROLLER"
        ));
    }
}
