package com.smartambulance.api;

import com.smartambulance.api.dto.RouteCalculationRequestDto;
import com.smartambulance.domain.RoadSegment;
import com.smartambulance.domain.RouteEntity;
import com.smartambulance.repository.RoadSegmentRepository;
import com.smartambulance.repository.RouteRepository;
import com.smartambulance.routing.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private DijkstraRouteOptimizer dijkstraOptimizer;

    @Autowired
    private AStarRouteOptimizer aStarOptimizer;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private RouteRepository routeRepository;

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateRoutes(@Valid @RequestBody RouteCalculationRequestDto request) {
        List<RoadSegment> segments = roadSegmentRepository.findAll();
        RoadGraph graph = RoadGraph.fromSegments(segments);

        boolean useAStar = "A_STAR".equalsIgnoreCase(request.getAlgorithm());
        RouteOptimizer optimizer = useAStar ? aStarOptimizer : dijkstraOptimizer;

        boolean includeSignalDelay = Boolean.TRUE.equals(request.getIncludeSignalDelay());
        double signalDelay = request.getSignalDelayMinutes() != null ? request.getSignalDelayMinutes() : 0.25;

        List<RouteCandidate> candidates = optimizer.calculateAllRoutes(
                graph, request.getOrigin(), request.getDestination(), includeSignalDelay, signalDelay);

        Map<String, Object> response = new HashMap<>();
        response.put("algorithm", useAStar ? "A_STAR" : "DIJKSTRA");
        response.put("origin", request.getOrigin());
        response.put("destination", request.getDestination());
        response.put("candidates", candidates);

        if (!candidates.isEmpty()) {
            RouteCandidate recommended = candidates.get(0);
            response.put("recommended_route", recommended.getRouteName());
            response.put("recommended_eta_minutes", recommended.getEstimatedTimeMinutes());
            response.put("recommended_distance_km", recommended.getTotalDistanceKm());

            // Build human-readable comparison reason
            if (candidates.size() > 1) {
                RouteCandidate alt = candidates.get(1);
                String reason = String.format("%s is recommended (ETA %.1f min). Faster than %s (ETA %.1f min).",
                        recommended.getRouteName(), recommended.getEstimatedTimeMinutes(),
                        alt.getRouteName(), alt.getEstimatedTimeMinutes());
                response.put("reason", reason);
            } else {
                response.put("reason", recommended.getRouteName() + " is the only navigable route (ETA " + recommended.getEstimatedTimeMinutes() + " min).");
            }
        } else {
            response.put("recommended_route", null);
            response.put("reason", "No viable route found between " + request.getOrigin() + " and " + request.getDestination());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteEntity> getRouteById(@PathVariable String id) {
        return routeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
