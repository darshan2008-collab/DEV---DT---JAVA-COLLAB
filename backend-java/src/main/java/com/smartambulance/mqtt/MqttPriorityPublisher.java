package com.smartambulance.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class MqttPriorityPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttPriorityPublisher.class);

    @Value("${smartambulance.mqtt.enabled:true}")
    private boolean enabled;

    @Value("${smartambulance.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${smartambulance.mqtt.client-id:smart-ambulance-backend}")
    private String clientId;

    private MqttClient client;
    private boolean isConnected = false;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("MQTT publishing is disabled in configuration.");
            return;
        }
        try {
            client = new MqttClient(brokerUrl, clientId + "-" + System.currentTimeMillis());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(3);
            options.setKeepAliveInterval(30);

            client.connect(options);
            isConnected = true;
            log.info("Connected to MQTT broker at {}", brokerUrl);
        } catch (Exception e) {
            log.warn("Could not connect to MQTT broker ({}) - using HTTP / internal fallback: {}", brokerUrl, e.getMessage());
            isConnected = false;
        }
    }

    public boolean publish(String topic, String payload) {
        if (!isConnected || client == null || !client.isConnected()) {
            log.debug("MQTT not connected. Message for topic {} logged via fallback: {}", topic, payload);
            return false;
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            client.publish(topic, message);
            log.info("Published to MQTT topic {}: {}", topic, payload);
            return true;
        } catch (Exception e) {
            log.warn("Failed to publish to MQTT topic {}: {}", topic, e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        return isConnected && client != null && client.isConnected();
    }

    @PreDestroy
    public void cleanup() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                client.close();
            } catch (Exception e) {
                log.warn("Error disconnecting MQTT client: {}", e.getMessage());
            }
        }
    }
}
