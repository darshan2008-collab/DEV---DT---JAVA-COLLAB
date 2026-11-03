package com.smartambulance.api;

import com.smartambulance.domain.Ambulance;
import com.smartambulance.repository.AmbulanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ambulances")
public class AmbulanceController {

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @GetMapping
    public ResponseEntity<List<Ambulance>> getAllAmbulances() {
        return ResponseEntity.ok(ambulanceRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Ambulance> createAmbulance(@RequestBody Ambulance ambulance) {
        ambulance.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(ambulanceRepository.save(ambulance));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Ambulance> patchAmbulance(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        Ambulance amb = ambulanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found: " + id));

        if (updates.containsKey("current_lat")) {
            amb.setCurrentLat(((Number) updates.get("current_lat")).doubleValue());
        }
        if (updates.containsKey("current_lon")) {
            amb.setCurrentLon(((Number) updates.get("current_lon")).doubleValue());
        }
        if (updates.containsKey("speed_kmph")) {
            amb.setSpeedKmph(((Number) updates.get("speed_kmph")).doubleValue());
        }
        amb.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(ambulanceRepository.save(amb));
    }
}
