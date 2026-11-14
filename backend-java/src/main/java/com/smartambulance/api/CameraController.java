package com.smartambulance.api;

import com.smartambulance.api.dto.CameraStreamDto;
import com.smartambulance.domain.CameraStream;
import com.smartambulance.repository.CameraStreamRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cameras")
@Tag(name = "Camera Registry", description = "RTSP IP Camera registration and management")
public class CameraController {

    private final CameraStreamRepository repo;

    public CameraController(CameraStreamRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @Operation(summary = "List all registered IP cameras")
    public List<CameraStream> listCameras() {
        return repo.findAll();
    }

    @GetMapping("/{streamId}")
    @Operation(summary = "Get camera by stream ID")
    public CameraStream getCamera(@PathVariable String streamId) {
        return repo.findByStreamId(streamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Camera stream not found: " + streamId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new RTSP IP camera")
    public CameraStream registerCamera(@Valid @RequestBody CameraStreamDto dto) {
        if (repo.existsByStreamId(dto.getStreamId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Camera stream ID already exists: " + dto.getStreamId());
        }
        CameraStream cam = new CameraStream();
        cam.setStreamId(dto.getStreamId());
        cam.setRtspUrl(dto.getRtspUrl());
        cam.setSegmentId(dto.getSegmentId());
        cam.setLocationLat(dto.getLocationLat());
        cam.setLocationLng(dto.getLocationLng());
        cam.setLabel(dto.getLabel());
        cam.setFpsSample(dto.getFpsSample() != null ? dto.getFpsSample() : 2);
        cam.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        return repo.save(cam);
    }

    @PutMapping("/{streamId}")
    @Operation(summary = "Update camera configuration")
    public CameraStream updateCamera(@PathVariable String streamId,
                                     @Valid @RequestBody CameraStreamDto dto) {
        CameraStream cam = repo.findByStreamId(streamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Camera stream not found: " + streamId));
        if (dto.getRtspUrl() != null) cam.setRtspUrl(dto.getRtspUrl());
        if (dto.getSegmentId() != null) cam.setSegmentId(dto.getSegmentId());
        if (dto.getLocationLat() != null) cam.setLocationLat(dto.getLocationLat());
        if (dto.getLocationLng() != null) cam.setLocationLng(dto.getLocationLng());
        if (dto.getLabel() != null) cam.setLabel(dto.getLabel());
        if (dto.getFpsSample() != null) cam.setFpsSample(dto.getFpsSample());
        if (dto.getEnabled() != null) cam.setEnabled(dto.getEnabled());
        return repo.save(cam);
    }

    @PutMapping("/{streamId}/toggle")
    @Operation(summary = "Enable or disable a camera stream")
    public ResponseEntity<Map<String, Object>> toggleCamera(@PathVariable String streamId) {
        CameraStream cam = repo.findByStreamId(streamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Camera stream not found: " + streamId));
        cam.setEnabled(!cam.getEnabled());
        repo.save(cam);
        return ResponseEntity.ok(Map.of(
                "stream_id", streamId,
                "enabled", cam.getEnabled(),
                "message", "Camera " + (cam.getEnabled() ? "enabled" : "disabled")
        ));
    }

    @PutMapping("/{streamId}/heartbeat")
    @Operation(summary = "Update last_seen_at timestamp (called by vision-service)")
    public ResponseEntity<Map<String, Object>> heartbeat(@PathVariable String streamId) {
        CameraStream cam = repo.findByStreamId(streamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Camera stream not found: " + streamId));
        cam.setLastSeenAt(OffsetDateTime.now());
        repo.save(cam);
        return ResponseEntity.ok(Map.of(
                "stream_id", streamId,
                "last_seen_at", cam.getLastSeenAt().toString()
        ));
    }

    @DeleteMapping("/{streamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a camera from registry")
    public void deleteCamera(@PathVariable String streamId) {
        CameraStream cam = repo.findByStreamId(streamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Camera stream not found: " + streamId));
        repo.delete(cam);
    }

    @GetMapping("/segment/{segmentId}")
    @Operation(summary = "Get cameras monitoring a specific road segment")
    public List<CameraStream> getCamerasBySegment(@PathVariable String segmentId) {
        return repo.findBySegmentId(segmentId);
    }
}
