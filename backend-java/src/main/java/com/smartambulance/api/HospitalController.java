package com.smartambulance.api;

import com.smartambulance.domain.Hospital;
import com.smartambulance.repository.HospitalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    @Autowired
    private HospitalRepository hospitalRepository;

    @GetMapping
    public ResponseEntity<List<Hospital>> getAllHospitals() {
        return ResponseEntity.ok(hospitalRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Hospital> createHospital(@RequestBody Hospital hospital) {
        return ResponseEntity.ok(hospitalRepository.save(hospital));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Hospital> patchHospital(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        Hospital h = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        if (updates.containsKey("available_beds")) {
            h.setAvailableBeds(((Number) updates.get("available_beds")).intValue());
        }
        if (updates.containsKey("notes")) {
            h.setNotes((String) updates.get("notes"));
        }
        return ResponseEntity.ok(hospitalRepository.save(h));
    }
}
