package com.smartambulance.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Smart Ambulance Navigation Backend");
        health.put("version", "1.0.0");
        health.put("active_profile", activeProfile);
        health.put("timestamp", Instant.now().toString());
        health.put("mode", "Academic Prototype (M. Kumarasamy College of Engineering / Smart India Hackathon)");
        return ResponseEntity.ok(health);
    }
}
