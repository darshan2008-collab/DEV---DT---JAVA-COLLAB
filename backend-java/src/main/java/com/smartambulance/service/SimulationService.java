package com.smartambulance.service;

import com.smartambulance.decision.RouteDecisionEngine;
import com.smartambulance.domain.*;
import com.smartambulance.repository.*;
import com.smartambulance.websocket.LiveEvent;
import com.smartambulance.websocket.LiveEventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private boolean isRunning = true;
    private String activeScenario = "NORMAL";
    private final Random random = new Random(42); // Fixed seed for reproducible simulation

    @Autowired
    private ObservationPreprocessingService preprocessingService;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private EmergencyTripRepository tripRepository;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private RouteDecisionEngine decisionEngine;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private LiveEventBroadcaster liveEventBroadcaster;

    // Simulation loop running every 5 seconds (simulates 30s observation steps)
    @Scheduled(fixedDelay = 5000)
    public void simulationStep() {
        if (!isRunning) {
            return;
        }

        try {
            // If scenario is FEED_LOST, simulate missing camera feeds by skipping new observations
            if (!"FEED_LOST".equalsIgnoreCase(activeScenario)) {
                generateObservationsForScenario(activeScenario);
            }

            // Advance active trips along route geometry
            advanceActiveTrips();

        } catch (Exception e) {
            log.error("Error in simulation step: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void setScenario(String scenario) {
        this.activeScenario = scenario.toUpperCase();
        log.info("Simulation scenario switched to: {}", this.activeScenario);

        systemSettingRepository.findById("ACTIVE_SCENARIO").ifPresent(setting -> {
            setting.setSettingValue(this.activeScenario);
            systemSettingRepository.save(setting);
        });

        // Trigger immediate observation update for the scenario
        if (!"FEED_LOST".equalsIgnoreCase(this.activeScenario)) {
            generateObservationsForScenario(this.activeScenario);
        }

        // Broadcast simulation event
        liveEventBroadcaster.broadcast(new LiveEvent(
                "SIMULATION_EVENT",
                null,
                null,
                null,
                null,
                null,
                "Scenario changed to " + this.activeScenario,
                0L,
                "SIMULATION"
        ));

        // Evaluate all active trips with the new traffic state
        List<EmergencyTrip> activeTrips = tripRepository.findByStatus(TripStatus.ACTIVE);
        for (EmergencyTrip trip : activeTrips) {
            decisionEngine.evaluateActiveTrip(trip, "DIJKSTRA");
        }
    }

    public void startSimulation() {
        this.isRunning = true;
        log.info("Simulation started.");
    }

    public void pauseSimulation() {
        this.isRunning = false;
        log.info("Simulation paused.");
    }

    @Transactional
    public void resetSimulation() {
        this.isRunning = true;
        this.activeScenario = "NORMAL";

        // Reset road segments to default Section 5 parameters
        List<RoadSegment> segments = roadSegmentRepository.findAll();
        for (RoadSegment s : segments) {
            s.setIsClosed(false);
            if (s.getId().equals("A1")) { s.setCurrentSmoothedSpeedKmph(30.0); s.setTravelTimeMinutes(6.0); }
            if (s.getId().equals("A2")) { s.setCurrentSmoothedSpeedKmph(15.0); s.setTravelTimeMinutes(12.0); }
            if (s.getId().equals("B1")) { s.setCurrentSmoothedSpeedKmph(40.0); s.setTravelTimeMinutes(6.0); }
            if (s.getId().equals("B2")) { s.setCurrentSmoothedSpeedKmph(40.0); s.setTravelTimeMinutes(6.0); }
            if (s.getId().equals("C1")) { s.setCurrentSmoothedSpeedKmph(40.0); s.setTravelTimeMinutes(7.5); }
            if (s.getId().equals("C2")) { s.setCurrentSmoothedSpeedKmph(40.0); s.setTravelTimeMinutes(7.5); }
            s.setCongestionLevel(CongestionLevel.LOW);
            s.setLastObservationTime(Instant.now());
            roadSegmentRepository.save(s);
        }

        log.info("Simulation reset to default NORMAL state.");
    }

    private void generateObservationsForScenario(String scenario) {
        Instant now = Instant.now();
        List<RoadSegment> segments = roadSegmentRepository.findAll();

        for (RoadSegment segment : segments) {
            double speed = segment.getTypicalSpeedKmph();
            int vehicleCount = 10;
            double queueM = 10.0;
            double density = 20.0;
            double confidence = 0.90 + (random.nextDouble() * 0.08);

            switch (scenario) {
                case "NORMAL":
                    if (segment.getId().equals("A1")) { speed = 30.0; vehicleCount = 14; density = 28.0; }
                    if (segment.getId().equals("A2")) { speed = 15.0; vehicleCount = 22; density = 44.0; queueM = 60.0; }
                    if (segment.getId().equals("B1")) { speed = 40.0; vehicleCount = 10; density = 20.0; }
                    if (segment.getId().equals("B2")) { speed = 40.0; vehicleCount = 12; density = 24.0; }
                    if (segment.getId().equals("C1")) { speed = 40.0; vehicleCount = 8;  density = 16.0; }
                    if (segment.getId().equals("C2")) { speed = 40.0; vehicleCount = 9;  density = 18.0; }
                    break;

                case "QUEUE_ON_B2":
                    // Exact Section 5 numbers: B2 speed drops to 12 km/h
                    if (segment.getId().equals("B2")) {
                        speed = 12.0;
                        vehicleCount = 18;
                        density = 62.0;
                        queueM = 140.0;
                    } else if (segment.getId().equals("A1")) { speed = 30.0; }
                    else if (segment.getId().equals("A2")) { speed = 15.0; }
                    else { speed = 40.0; }
                    break;

                case "SUDDEN_CONGESTION":
                    if (segment.getId().startsWith("B") || segment.getId().startsWith("A")) {
                        speed = Math.max(10.0, segment.getTypicalSpeedKmph() * 0.35);
                        vehicleCount = 26;
                        queueM = 180.0;
                    } else {
                        speed = 40.0;
                    }
                    break;

                case "ROAD_CLOSED":
                    if (segment.getId().equals("B1")) {
                        segment.setIsClosed(true);
                        roadSegmentRepository.save(segment);
                    }
                    break;

                case "RECOVERY":
                    segment.setIsClosed(false);
                    speed = segment.getTypicalSpeedKmph();
                    queueM = 5.0;
                    break;
            }

            // Small realistic noise (+/- 0.5 km/h)
            double noise = (random.nextDouble() - 0.5) * 1.0;
            double finalSpeed = Math.max(5.0, Math.round((speed + noise) * 10.0) / 10.0);

            TrafficObservation obs = new TrafficObservation(
                    segment.getId(),
                    now,
                    vehicleCount,
                    density,
                    finalSpeed,
                    queueM,
                    confidence,
                    DataSource.SIMULATION
            );

            preprocessingService.processIncomingObservation(obs);
        }
    }

    private void advanceActiveTrips() {
        List<EmergencyTrip> activeTrips = tripRepository.findByStatus(TripStatus.ACTIVE);
        for (EmergencyTrip trip : activeTrips) {
            // Decrement remaining distance slightly (simulating travel)
            double currentRemaining = trip.getRemainingDistanceKm();
            double stepKm = 0.5; // Travel step per simulation tick
            double newRemaining = Math.max(0.0, currentRemaining - stepKm);
            trip.setRemainingDistanceKm(Math.round(newRemaining * 100.0) / 100.0);

            // Interpolate GPS coordinates between origin and hospital
            ambulanceRepository.findById(trip.getAmbulanceId()).ifPresent(amb -> {
                double progress = 1.0 - (newRemaining / Math.max(1.0, (trip.getRemainingDistanceKm() + 0.1)));
                // S: (10.9400, 78.0700), H: (10.9700, 78.0800)
                double lat = 10.9400 + (10.9700 - 10.9400) * Math.min(1.0, progress);
                double lon = 78.0700 + (78.0800 - 78.0700) * Math.min(1.0, progress);
                amb.setCurrentLat(lat);
                amb.setCurrentLon(lon);
                amb.setSpeedKmph(45.0);
                ambulanceRepository.save(amb);
            });

            // Re-evaluate routes and decision logic
            decisionEngine.evaluateActiveTrip(trip, "DIJKSTRA");

            // Complete trip if ambulance arrived
            if (newRemaining <= 0.05) {
                trip.setStatus(TripStatus.COMPLETED);
                trip.setArrivalTime(Instant.now());
                trip.setCurrentEtaMinutes(0.0);
                tripRepository.save(trip);

                liveEventBroadcaster.broadcast(new LiveEvent(
                        "TRIP_STATUS_CHANGED",
                        trip.getId(),
                        trip.getSelectedRoute(),
                        trip.getSelectedRoute(),
                        0.0,
                        0.0,
                        "Ambulance arrived at destination hospital. Trip completed.",
                        0L,
                        "SIMULATION"
                ));
            } else {
                tripRepository.save(trip);
            }
        }
    }

    public boolean isRunning() { return isRunning; }
    public String getActiveScenario() { return activeScenario; }
}
