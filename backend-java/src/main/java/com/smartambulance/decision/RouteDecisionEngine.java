package com.smartambulance.decision;

import com.smartambulance.domain.*;
import com.smartambulance.repository.*;
import com.smartambulance.routing.*;
import com.smartambulance.websocket.LiveEvent;
import com.smartambulance.websocket.LiveEventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class RouteDecisionEngine {

    private static final Logger log = LoggerFactory.getLogger(RouteDecisionEngine.class);

    @Value("${smartambulance.routing.hysteresis-margin-minutes:1.0}")
    private double hysteresisMarginMinutes;

    @Value("${smartambulance.routing.stale-data-timeout-seconds:120}")
    private long staleDataTimeoutSeconds;

    @Autowired
    private DijkstraRouteOptimizer dijkstraOptimizer;

    @Autowired
    private AStarRouteOptimizer aStarOptimizer;

    @Autowired
    private DistanceBaselinePlanner baselinePlanner;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private TrafficObservationRepository observationRepository;

    @Autowired
    private EmergencyTripRepository tripRepository;

    @Autowired
    private RouteUpdateRepository routeUpdateRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private LiveEventBroadcaster liveEventBroadcaster;

    @Transactional
    public DecisionResult evaluateActiveTrip(EmergencyTrip trip, String algorithm) {
        if (trip == null || trip.getStatus() != TripStatus.ACTIVE) {
            return DecisionResult.noAction("Trip not active");
        }

        List<RoadSegment> segments = roadSegmentRepository.findAll();
        RoadGraph graph = RoadGraph.fromSegments(segments);

        // Check for stale or missing camera data
        boolean dataIsStale = false;
        long maxAgeSeconds = 0;
        Instant now = Instant.now();

        for (RoadSegment segment : segments) {
            Optional<TrafficObservation> latestObs = observationRepository.findTopBySegmentIdOrderByTimestampDesc(segment.getId());
            if (latestObs.isEmpty()) {
                dataIsStale = true;
            } else {
                long age = Duration.between(latestObs.get().getTimestamp(), now).getSeconds();
                if (age > maxAgeSeconds) {
                    maxAgeSeconds = age;
                }
                if (age > staleDataTimeoutSeconds || latestObs.get().getIsMasked()) {
                    dataIsStale = true;
                }
            }
        }

        // Baseline route (distance-based)
        RouteCandidate baselineRoute = baselinePlanner.findShortestDistanceRoute(graph, trip.getOrigin(), trip.getDestination());

        // Dynamic route calculation
        RouteOptimizer optimizer = "A_STAR".equalsIgnoreCase(algorithm) ? aStarOptimizer : dijkstraOptimizer;
        List<RouteCandidate> candidates = optimizer.calculateAllRoutes(graph, trip.getOrigin(), trip.getDestination(), false, 0.0);

        if (candidates.isEmpty()) {
            return DecisionResult.noAction("No navigable route found.");
        }

        // Candidate matching the currently selected route
        RouteCandidate currentRouteCandidate = candidates.stream()
                .filter(c -> c.getRouteName().equalsIgnoreCase(trip.getSelectedRoute()))
                .findFirst()
                .orElse(candidates.get(0));

        double currentRouteEta = currentRouteCandidate.getEstimatedTimeMinutes();
        trip.setCurrentEtaMinutes(currentRouteEta);

        // Fallback scenario: Stale or missing data
        if (dataIsStale) {
            String warningMsg = "Traffic camera data is STALE (last update " + maxAgeSeconds + "s ago). Falling back to distance-based baseline. ETA is UNCERTAIN.";
            log.warn("Stale traffic data detected for trip {}: age={}s", trip.getId(), maxAgeSeconds);

            liveEventBroadcaster.broadcast(new LiveEvent(
                    "STALE_DATA",
                    trip.getId(),
                    trip.getSelectedRoute(),
                    baselineRoute != null ? baselineRoute.getRouteName() : trip.getSelectedRoute(),
                    trip.getCurrentEtaMinutes(),
                    baselineRoute != null ? baselineRoute.getEstimatedTimeMinutes() : trip.getCurrentEtaMinutes(),
                    warningMsg,
                    maxAgeSeconds,
                    "FALLBACK"
            ));

            return DecisionResult.staleFallback(warningMsg, baselineRoute, maxAgeSeconds);
        }

        // Evaluate best candidate vs current route
        RouteCandidate bestCandidate = candidates.get(0);
        double bestEta = bestCandidate.getEstimatedTimeMinutes();

        // Check if best route is different from current route
        if (!bestCandidate.getRouteName().equalsIgnoreCase(trip.getSelectedRoute())) {
            double etaImprovement = currentRouteEta - bestEta;

            // Hysteresis rule: switch only if improvement > hysteresisMarginMinutes
            if (etaImprovement > hysteresisMarginMinutes) {
                String reason = buildRerouteReason(trip.getSelectedRoute(), currentRouteEta, bestCandidate.getRouteName(), bestEta, segments);

                String recId = "REC-" + UUID.randomUUID().toString().substring(0, 8);
                Recommendation rec = new Recommendation(recId, trip.getId(), bestCandidate.getRouteName(), reason, bestEta);
                recommendationRepository.save(rec);

                // Broadcast recommendation created
                liveEventBroadcaster.broadcast(new LiveEvent(
                        "RECOMMENDATION_CREATED",
                        trip.getId(),
                        trip.getSelectedRoute(),
                        bestCandidate.getRouteName(),
                        currentRouteEta,
                        bestEta,
                        reason,
                        maxAgeSeconds,
                        "DYNAMIC_OPTIMIZER"
                ));

                return DecisionResult.recommendationCreated(rec, bestCandidate, etaImprovement, reason);
            } else {
                String noSwitchReason = String.format("Current route %s (%.1f min) retained. %s is only %.1f min faster, within the %.1f min hysteresis margin.",
                        trip.getSelectedRoute(), currentRouteEta, bestCandidate.getRouteName(), etaImprovement, hysteresisMarginMinutes);
                return DecisionResult.noAction(noSwitchReason);
            }
        } else {
            String reason = String.format("Current route %s remains optimal (ETA %.1f min). No faster alternative available.",
                    trip.getSelectedRoute(), currentRouteEta);
            return DecisionResult.noAction(reason);
        }
    }

    @Transactional
    public void acceptRecommendation(String tripId, String recommendationId) {
        EmergencyTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));

        String oldRoute = trip.getSelectedRoute();
        double oldEta = trip.getCurrentEtaMinutes();
        String newRoute = rec.getRecommendedRoute();
        double newEta = rec.getEtaMinutes();
        double diff = oldEta - newEta;

        trip.setSelectedRoute(newRoute);
        trip.setCurrentEtaMinutes(newEta);
        trip.setRouteChangeCount(trip.getRouteChangeCount() + 1);
        tripRepository.save(trip);

        rec.setStatus(RecommendationStatus.ACCEPTED);
        rec.setDriverDecision(DriverDecision.ACCEPTED);
        rec.setDecisionTimestamp(Instant.now());
        recommendationRepository.save(rec);

        // Persist RouteUpdate
        RouteUpdate update = new RouteUpdate(trip.getId(), oldRoute, newRoute, oldEta, newEta, diff, rec.getReason(), "DYNAMIC_OPTIMIZER");
        routeUpdateRepository.save(update);

        log.info("Driver accepted re-route for trip {}: {} -> {} (saved {:.1f} min)", trip.getId(), oldRoute, newRoute, diff);

        // Broadcast ROUTE_CHANGED
        liveEventBroadcaster.broadcast(new LiveEvent(
                "ROUTE_CHANGED",
                trip.getId(),
                oldRoute,
                newRoute,
                oldEta,
                newEta,
                rec.getReason(),
                0L,
                "DYNAMIC_OPTIMIZER"
        ));

        // Hospital ETA notification
        liveEventBroadcaster.broadcast(new LiveEvent(
                "HOSPITAL_ETA_UPDATED",
                trip.getId(),
                oldRoute,
                newRoute,
                oldEta,
                newEta,
                "Ambulance switched to " + newRoute + " - ETA updated to " + Math.round(newEta) + " min",
                0L,
                "DYNAMIC_OPTIMIZER"
        ));
    }

    @Transactional
    public void rejectRecommendation(String tripId, String recommendationId) {
        Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));

        rec.setStatus(RecommendationStatus.REJECTED);
        rec.setDriverDecision(DriverDecision.REJECTED);
        rec.setDecisionTimestamp(Instant.now());
        recommendationRepository.save(rec);

        log.info("Driver rejected re-route for trip {}: recommendation {}", tripId, recommendationId);

        liveEventBroadcaster.broadcast(new LiveEvent(
                "RECOMMENDATION_REJECTED",
                tripId,
                rec.getRecommendedRoute(),
                rec.getRecommendedRoute(),
                rec.getEtaMinutes(),
                rec.getEtaMinutes(),
                "Driver rejected route recommendation. Continuing on current route.",
                0L,
                "DRIVER_DECISION"
        ));
    }

    private String buildRerouteReason(String currentRoute, double currentEta, String newRoute, double newEta, List<RoadSegment> segments) {
        // Find if current route has congested segments
        StringBuilder congestedSegments = new StringBuilder();
        for (RoadSegment s : segments) {
            if (s.getId().startsWith(currentRoute.replace("Route ", "").trim())) {
                if (s.getCongestionLevel() == CongestionLevel.HIGH || s.getCongestionLevel() == CongestionLevel.CRITICAL) {
                    if (congestedSegments.length() > 0) congestedSegments.append(", ");
                    congestedSegments.append(s.getId()).append(" (speed ").append(Math.round(s.getCurrentSmoothedSpeedKmph())).append(" km/h)");
                }
            }
        }

        if (congestedSegments.length() > 0) {
            return String.format("%s has heavy congestion on %s (ETA %.0f min). %s is faster (ETA %.0f min).",
                    currentRoute, congestedSegments.toString(), currentEta, newRoute, newEta);
        } else {
            return String.format("%s offers a travel time improvement of %.1f min over %s (%.0f min vs %.0f min).",
                    newRoute, (currentEta - newEta), currentRoute, newEta, currentEta);
        }
    }

    public static class DecisionResult {
        private final String action;
        private final String message;
        private final Recommendation recommendation;
        private final RouteCandidate candidate;
        private final boolean dataStale;

        private DecisionResult(String action, String message, Recommendation recommendation, RouteCandidate candidate, boolean dataStale) {
            this.action = action;
            this.message = message;
            this.recommendation = recommendation;
            this.candidate = candidate;
            this.dataStale = dataStale;
        }

        public static DecisionResult noAction(String reason) {
            return new DecisionResult("NO_ACTION", reason, null, null, false);
        }

        public static DecisionResult recommendationCreated(Recommendation rec, RouteCandidate candidate, double diff, String reason) {
            return new DecisionResult("RECOMMEND_SWITCH", reason, rec, candidate, false);
        }

        public static DecisionResult staleFallback(String warning, RouteCandidate baseline, long ageSeconds) {
            return new DecisionResult("STALE_FALLBACK", warning, null, baseline, true);
        }

        public String getAction() { return action; }
        public String getMessage() { return message; }
        public Recommendation getRecommendation() { return recommendation; }
        public RouteCandidate getCandidate() { return candidate; }
        public boolean isDataStale() { return dataStale; }
    }
}
