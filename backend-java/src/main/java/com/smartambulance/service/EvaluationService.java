package com.smartambulance.service;

import com.smartambulance.domain.RoadSegment;
import com.smartambulance.domain.RouteUpdate;
import com.smartambulance.repository.RoadSegmentRepository;
import com.smartambulance.repository.RouteUpdateRepository;
import com.smartambulance.routing.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class EvaluationService {

    @Autowired
    private DijkstraRouteOptimizer dijkstraOptimizer;

    @Autowired
    private AStarRouteOptimizer aStarOptimizer;

    @Autowired
    private DistanceBaselinePlanner baselinePlanner;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private RouteUpdateRepository routeUpdateRepository;

    public Map<String, Object> getEvaluationSummary() {
        List<RoadSegment> segments = roadSegmentRepository.findAll();
        RoadGraph graph = RoadGraph.fromSegments(segments);

        long startTime = System.nanoTime();
        RouteCandidate dynamicBest = dijkstraOptimizer.findBestRoute(graph, "S", "H", false, 0.0);
        long dijkstraTimeNs = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        RouteCandidate aStarBest = aStarOptimizer.findBestRoute(graph, "S", "H", false, 0.0);
        long aStarTimeNs = System.nanoTime() - startTime;

        RouteCandidate baselineBest = baselinePlanner.findShortestDistanceRoute(graph, "S", "H");

        List<RouteUpdate> updates = routeUpdateRepository.findAllByOrderByTimestampDesc();

        double totalTimeSavedMinutes = 0.0;
        for (RouteUpdate u : updates) {
            if (u.getTimeDifferenceMinutes() > 0) {
                totalTimeSavedMinutes += u.getTimeDifferenceMinutes();
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("timestamp", Instant.now().toString());
        summary.put("data_mode", "SIMULATION (assumed illustrative speeds, not field measurements)");
        summary.put("dynamic_route", dynamicBest != null ? dynamicBest.getRouteName() : "N/A");
        summary.put("dynamic_eta_minutes", dynamicBest != null ? dynamicBest.getEstimatedTimeMinutes() : 0.0);
        summary.put("dynamic_distance_km", dynamicBest != null ? dynamicBest.getTotalDistanceKm() : 0.0);
        summary.put("baseline_route", baselineBest != null ? baselineBest.getRouteName() : "N/A");
        summary.put("baseline_eta_minutes", baselineBest != null ? baselineBest.getEstimatedTimeMinutes() : 0.0);
        summary.put("baseline_distance_km", baselineBest != null ? baselineBest.getTotalDistanceKm() : 0.0);
        summary.put("time_advantage_minutes", (baselineBest != null && dynamicBest != null) ?
                Math.round((baselineBest.getEstimatedTimeMinutes() - dynamicBest.getEstimatedTimeMinutes()) * 10.0) / 10.0 : 0.0);
        summary.put("total_rerouting_events", updates.size());
        summary.put("total_simulated_time_saved_minutes", Math.round(totalTimeSavedMinutes * 10.0) / 10.0);
        summary.put("dijkstra_calculation_latency_ms", Math.round(dijkstraTimeNs / 10000.0) / 100.0);
        summary.put("astar_calculation_latency_ms", Math.round(aStarTimeNs / 10000.0) / 100.0);
        summary.put("algorithm_agreement", (dynamicBest != null && aStarBest != null &&
                dynamicBest.getEstimatedTimeMinutes() == aStarBest.getEstimatedTimeMinutes()));
        summary.put("disclaimer", "Academic prototype demonstration. Results are illustrative simulations and do not claim real-world response time reduction.");

        return summary;
    }

    public String generateEvaluationCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("metric,value,unit,data_type,notes\n");

        Map<String, Object> summary = getEvaluationSummary();
        csv.append("dynamic_route,").append(summary.get("dynamic_route")).append(",-,SIMULATED,Recommended dynamic route\n");
        csv.append("dynamic_eta,").append(summary.get("dynamic_eta_minutes")).append(",minutes,SIMULATED,Estimated travel time\n");
        csv.append("baseline_route,").append(summary.get("baseline_route")).append(",-,SIMULATED,Shortest distance baseline\n");
        csv.append("baseline_eta,").append(summary.get("baseline_eta_minutes")).append(",minutes,SIMULATED,Baseline travel time\n");
        csv.append("time_advantage,").append(summary.get("time_advantage_minutes")).append(",minutes,SIMULATED,Dynamic time advantage\n");
        csv.append("total_reroutes,").append(summary.get("total_rerouting_events")).append(",count,RECORDED,Persisted re-routing count\n");
        csv.append("dijkstra_latency,").append(summary.get("dijkstra_calculation_latency_ms")).append(",ms,MEASURED,Calculation latency\n");
        csv.append("astar_latency,").append(summary.get("astar_calculation_latency_ms")).append(",ms,MEASURED,Calculation latency\n");

        return csv.toString();
    }
}
