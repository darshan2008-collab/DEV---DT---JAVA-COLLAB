package com.smartambulance.api;

import com.smartambulance.api.dto.RoadPatchDto;
import com.smartambulance.domain.RoadSegment;
import com.smartambulance.repository.RoadSegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @GetMapping
    public ResponseEntity<List<RoadSegment>> getAllRoads() {
        return ResponseEntity.ok(roadSegmentRepository.findAll());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RoadSegment> patchRoad(@PathVariable String id, @RequestBody RoadPatchDto patchDto) {
        RoadSegment road = roadSegmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Road segment not found: " + id));

        if (patchDto.getIsClosed() != null) {
            road.setIsClosed(patchDto.getIsClosed());
        }
        if (patchDto.getTypicalSpeedKmph() != null) {
            road.setTypicalSpeedKmph(patchDto.getTypicalSpeedKmph());
        }
        road.setUpdatedAt(Instant.now());
        RoadSegment saved = roadSegmentRepository.save(road);
        return ResponseEntity.ok(saved);
    }
}
