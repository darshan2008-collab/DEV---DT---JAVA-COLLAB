package com.smartambulance.api;

import com.smartambulance.api.dto.TripCreateRequestDto;
import com.smartambulance.decision.RouteDecisionEngine;
import com.smartambulance.domain.EmergencyTrip;
import com.smartambulance.domain.Recommendation;
import com.smartambulance.domain.RouteUpdate;
import com.smartambulance.repository.EmergencyTripRepository;
import com.smartambulance.repository.RecommendationRepository;
import com.smartambulance.repository.RouteUpdateRepository;
import com.smartambulance.service.TripService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    @Autowired
    private TripService tripService;

    @Autowired
    private EmergencyTripRepository tripRepository;

    @Autowired
    private RouteDecisionEngine decisionEngine;

    @Autowired
    private RouteUpdateRepository routeUpdateRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @GetMapping
    public ResponseEntity<List<EmergencyTrip>> getAllTrips() {
        return ResponseEntity.ok(tripRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping
    public ResponseEntity<EmergencyTrip> createTrip(@Valid @RequestBody TripCreateRequestDto request) {
        EmergencyTrip trip = tripService.createTrip(
                request.getAmbulanceId(),
                request.getHospitalId(),
                request.getOrigin(),
                request.getDestination()
        );
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyTrip> getTripById(@PathVariable String id) {
        return tripRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<EmergencyTrip> startTrip(@PathVariable String id) {
        return ResponseEntity.ok(tripService.startTrip(id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<EmergencyTrip> pauseTrip(@PathVariable String id) {
        return ResponseEntity.ok(tripService.pauseTrip(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<EmergencyTrip> resumeTrip(@PathVariable String id) {
        return ResponseEntity.ok(tripService.resumeTrip(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<EmergencyTrip> completeTrip(@PathVariable String id) {
        return ResponseEntity.ok(tripService.completeTrip(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<EmergencyTrip> cancelTrip(@PathVariable String id) {
        return ResponseEntity.ok(tripService.cancelTrip(id));
    }

    @PostMapping("/{id}/recommendations/{rid}/accept")
    public ResponseEntity<Map<String, String>> acceptRecommendation(@PathVariable String id, @PathVariable String rid) {
        decisionEngine.acceptRecommendation(id, rid);
        return ResponseEntity.ok(Map.of("status", "ACCEPTED", "message", "Route recommendation accepted by driver. Trip rerouted successfully."));
    }

    @PostMapping("/{id}/recommendations/{rid}/reject")
    public ResponseEntity<Map<String, String>> rejectRecommendation(@PathVariable String id, @PathVariable String rid) {
        decisionEngine.rejectRecommendation(id, rid);
        return ResponseEntity.ok(Map.of("status", "REJECTED", "message", "Route recommendation rejected by driver. Continuing on current route."));
    }

    @GetMapping("/{id}/route-updates")
    public ResponseEntity<List<RouteUpdate>> getTripRouteUpdates(@PathVariable String id) {
        return ResponseEntity.ok(routeUpdateRepository.findByTripIdOrderByTimestampDesc(id));
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<Recommendation>> getTripRecommendations(@PathVariable String id) {
        return ResponseEntity.ok(recommendationRepository.findByTripIdOrderByCreatedAtDesc(id));
    }
}
