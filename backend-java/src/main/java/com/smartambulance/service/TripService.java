package com.smartambulance.service;

import com.smartambulance.decision.RouteDecisionEngine;
import com.smartambulance.domain.*;
import com.smartambulance.repository.*;
import com.smartambulance.routing.*;
import com.smartambulance.websocket.LiveEvent;
import com.smartambulance.websocket.LiveEventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    @Autowired
    private EmergencyTripRepository tripRepository;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private DijkstraRouteOptimizer dijkstraOptimizer;

    @Autowired
    private RouteDecisionEngine decisionEngine;

    @Autowired
    private SignalPriorityService signalPriorityService;

    @Autowired
    private LiveEventBroadcaster liveEventBroadcaster;

    @Transactional
    public EmergencyTrip createTrip(String ambulanceId, String hospitalId, String origin, String destination) {
        // Prevent duplicate trip creation for the same ambulance
        List<EmergencyTrip> activeTrips = tripRepository.findByAmbulanceIdAndStatusIn(
                ambulanceId, List.of(TripStatus.CREATED, TripStatus.ACTIVE, TripStatus.PAUSED));
        if (!activeTrips.isEmpty()) {
            throw new IllegalStateException("Ambulance " + ambulanceId + " already has an active emergency trip: " + activeTrips.get(0).getId());
        }

        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found: " + ambulanceId));
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + hospitalId));

        List<RoadSegment> segments = roadSegmentRepository.findAll();
        RoadGraph graph = RoadGraph.fromSegments(segments);

        List<RouteCandidate> candidates = dijkstraOptimizer.calculateAllRoutes(graph, origin, destination, false, 0.0);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No navigable route between " + origin + " and " + destination);
        }

        RouteCandidate best = candidates.get(0);
        String tripId = "TRIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        EmergencyTrip trip = new EmergencyTrip(
                tripId,
                ambulanceId,
                hospitalId,
                origin,
                destination,
                best.getRouteName(),
                best.getEstimatedTimeMinutes(),
                best.getTotalDistanceKm()
        );

        ambulance.setStatus(AmbulanceStatus.ON_TRIP);
        ambulanceRepository.save(ambulance);

        EmergencyTrip saved = tripRepository.save(trip);
        log.info("Created emergency trip {}: ambulance {}, destination {}, recommended {}",
                tripId, ambulanceId, hospital.getName(), best.getRouteName());

        liveEventBroadcaster.broadcast(new LiveEvent(
                "TRIP_STATUS_CHANGED",
                saved.getId(),
                null,
                saved.getSelectedRoute(),
                null,
                saved.getCurrentEtaMinutes(),
                "Emergency trip created towards " + hospital.getName(),
                0L,
                "SYSTEM"
        ));

        return saved;
    }

    @Transactional
    public EmergencyTrip startTrip(String tripId) {
        EmergencyTrip trip = getTripOrThrow(tripId);
        validateStatusTransition(trip.getStatus(), TripStatus.ACTIVE);

        trip.setStatus(TripStatus.ACTIVE);
        trip.setStartTime(Instant.now());
        EmergencyTrip saved = tripRepository.save(trip);

        // Check if route approaches a junction to request priority
        triggerInitialSignalPriority(saved);

        liveEventBroadcaster.broadcast(new LiveEvent(
                "TRIP_STATUS_CHANGED",
                saved.getId(),
                null,
                saved.getSelectedRoute(),
                null,
                saved.getCurrentEtaMinutes(),
                "Trip started. Navigation active on " + saved.getSelectedRoute(),
                0L,
                "SYSTEM"
        ));
        return saved;
    }

    @Transactional
    public EmergencyTrip pauseTrip(String tripId) {
        EmergencyTrip trip = getTripOrThrow(tripId);
        validateStatusTransition(trip.getStatus(), TripStatus.PAUSED);

        trip.setStatus(TripStatus.PAUSED);
        EmergencyTrip saved = tripRepository.save(trip);

        liveEventBroadcaster.broadcast(new LiveEvent(
                "TRIP_STATUS_CHANGED",
                saved.getId(),
                saved.getSelectedRoute(),
                saved.getSelectedRoute(),
                saved.getCurrentEtaMinutes(),
                saved.getCurrentEtaMinutes(),
                "Trip paused by dispatcher/operator.",
                0L,
                "SYSTEM"
        ));
        return saved;
    }

    @Transactional
    public EmergencyTrip resumeTrip(String tripId) {
        EmergencyTrip trip = getTripOrThrow(tripId);
        validateStatusTransition(trip.getStatus(), TripStatus.ACTIVE);

        trip.setStatus(TripStatus.ACTIVE);
        EmergencyTrip saved = tripRepository.save(trip);

        liveEventBroadcaster.broadcast(new LiveEvent(
                "TRIP_STATUS_CHANGED",
                saved.getId(),
                saved.getSelectedRoute(),
                saved.getSelectedRoute(),
                saved.getCurrentEtaMinutes(),
                saved.getCurrentEtaMinutes(),
                "Trip resumed.",
                0L,
                "SYSTEM"
        ));
        return saved;
    }

    @Transactional
    public EmergencyTrip completeTrip(String tripId) {
        EmergencyTrip trip = getTripOrThrow(tripId);
        validateStatusTransition(trip.getStatus(), TripStatus.COMPLETED);

        trip.setStatus(TripStatus.COMPLETED);
        trip.setArrivalTime(Instant.now());
        trip.setCurrentEtaMinutes(0.0);
        trip.setRemainingDistanceKm(0.0);

        ambulanceRepository.findById(trip.getAmbulanceId()).ifPresent(amb -> {
            amb.setStatus(AmbulanceStatus.AVAILABLE);
            amb.setSpeedKmph(0.0);
            ambulanceRepository.save(amb);
        });

        EmergencyTrip saved = tripRepository.save(trip);
        log.info("Trip {} completed safely at hospital {}", tripId, trip.getHospitalId());

        liveEventBroadcaster.broadcast(new LiveEvent(
                "TRIP_STATUS_CHANGED",
                saved.getId(),
                saved.getSelectedRoute(),
                saved.getSelectedRoute(),
                0.0,
                0.0,
                "Ambulance arrived at destination hospital. Trip completed.",
                0L,
                "SYSTEM"
        ));

        liveEventBroadcaster.broadcast(new LiveEvent(
                "HOSPITAL_ETA_UPDATED",
                saved.getId(),
                saved.getSelectedRoute(),
                saved.getSelectedRoute(),
                0.0,
                0.0,
                "Patient arrived at emergency bay.",
                0L,
                "SYSTEM"
        ));

        return saved;
    }

    @Transactional
    public EmergencyTrip cancelTrip(String tripId) {
        EmergencyTrip trip = getTripOrThrow(tripId);
        validateStatusTransition(trip.getStatus(), TripStatus.CANCELLED);

        trip.setStatus(TripStatus.CANCELLED);

        ambulanceRepository.findById(trip.getAmbulanceId()).ifPresent(amb -> {
            amb.setStatus(AmbulanceStatus.AVAILABLE);
            amb.setSpeedKmph(0.0);
            ambulanceRepository.save(amb);
        });

        EmergencyTrip saved = tripRepository.save(trip);

        liveEventBroadcaster.broadcast(new LiveEvent(
                "TRIP_STATUS_CHANGED",
                saved.getId(),
                saved.getSelectedRoute(),
                saved.getSelectedRoute(),
                saved.getCurrentEtaMinutes(),
                saved.getCurrentEtaMinutes(),
                "Trip cancelled.",
                0L,
                "SYSTEM"
        ));
        return saved;
    }

    private void triggerInitialSignalPriority(EmergencyTrip trip) {
        // Approaching first junction based on route
        String junctionId = "J2"; // Default intermediate junction for Route B
        if (trip.getSelectedRoute().contains("A")) junctionId = "J1";
        if (trip.getSelectedRoute().contains("C")) junctionId = "J3";

        signalPriorityService.requestPriority(junctionId, "NORTH_SOUTH", trip.getAmbulanceId(), trip.getId());
    }

    private void validateStatusTransition(TripStatus from, TripStatus to) {
        boolean valid = switch (from) {
            case CREATED -> to == TripStatus.ACTIVE || to == TripStatus.CANCELLED;
            case ACTIVE -> to == TripStatus.PAUSED || to == TripStatus.COMPLETED || to == TripStatus.CANCELLED;
            case PAUSED -> to == TripStatus.ACTIVE || to == TripStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new IllegalStateException("Invalid trip status transition from " + from + " to " + to);
        }
    }

    private EmergencyTrip getTripOrThrow(String tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
    }
}
