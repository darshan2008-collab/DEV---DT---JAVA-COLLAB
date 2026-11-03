package com.smartambulance.api;

import com.smartambulance.api.dto.SignalPriorityRequestDto;
import com.smartambulance.domain.SignalEvent;
import com.smartambulance.repository.SignalEventRepository;
import com.smartambulance.service.SignalPriorityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signals")
public class SignalController {

    @Autowired
    private SignalPriorityService signalPriorityService;

    @Autowired
    private SignalEventRepository signalEventRepository;

    @PostMapping("/{junctionId}/priority")
    public ResponseEntity<SignalEvent> requestPriority(
            @PathVariable String junctionId,
            @Valid @RequestBody SignalPriorityRequestDto request) {

        SignalEvent event = signalPriorityService.requestPriority(
                junctionId,
                request.getDirection(),
                request.getAmbulanceId(),
                request.getTripId()
        );
        return ResponseEntity.ok(event);
    }

    @GetMapping("/{junctionId}/state")
    public ResponseEntity<Map<String, Object>> getJunctionState(@PathVariable String junctionId) {
        return ResponseEntity.ok(signalPriorityService.getFullJunctionStatus(junctionId));
    }

    @PostMapping("/{junctionId}/override")
    public ResponseEntity<Map<String, String>> manualOverride(@PathVariable String junctionId) {
        signalPriorityService.manualOverride(junctionId);
        return ResponseEntity.ok(Map.of(
                "status", "OVERRIDDEN",
                "junction_id", junctionId,
                "message", "Manual override triggered. Junction returned to normal fixed cycle immediately."
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SignalEvent>> getSignalHistory() {
        return ResponseEntity.ok(signalEventRepository.findAllByOrderByCreatedAtDesc());
    }
}
