package com.smartambulance.api;

import com.smartambulance.api.dto.ScenarioRequestDto;
import com.smartambulance.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startSimulation() {
        simulationService.startSimulation();
        return ResponseEntity.ok(Map.of("status", "RUNNING", "message", "Simulation started"));
    }

    @PostMapping("/pause")
    public ResponseEntity<Map<String, String>> pauseSimulation() {
        simulationService.pauseSimulation();
        return ResponseEntity.ok(Map.of("status", "PAUSED", "message", "Simulation paused"));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetSimulation() {
        simulationService.resetSimulation();
        return ResponseEntity.ok(Map.of("status", "RESET", "message", "Simulation reset to NORMAL baseline state"));
    }

    @PostMapping("/scenario")
    public ResponseEntity<Map<String, String>> setScenario(@Valid @RequestBody ScenarioRequestDto request) {
        simulationService.setScenario(request.getScenario());
        return ResponseEntity.ok(Map.of(
                "status", "UPDATED",
                "active_scenario", simulationService.getActiveScenario(),
                "message", "Active simulation scenario updated to " + simulationService.getActiveScenario()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "running", simulationService.isRunning(),
                "active_scenario", simulationService.getActiveScenario(),
                "mode", "SIMULATION_MODE (Assumed speeds for demonstration)"
        ));
    }
}
