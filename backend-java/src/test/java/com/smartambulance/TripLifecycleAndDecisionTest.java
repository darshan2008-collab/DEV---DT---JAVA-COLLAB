package com.smartambulance;

import com.smartambulance.decision.RouteDecisionEngine;
import com.smartambulance.domain.*;
import com.smartambulance.repository.*;
import com.smartambulance.service.SignalPriorityService;
import com.smartambulance.service.TripService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
public class TripLifecycleAndDecisionTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private EmergencyTripRepository tripRepository;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private TrafficObservationRepository observationRepository;

    @Autowired
    private RouteDecisionEngine decisionEngine;

    @Autowired
    private RouteUpdateRepository routeUpdateRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private SignalPriorityService signalPriorityService;

    @Test
    @DisplayName("Trip creation, duplicate prevention, and starting trip")
    public void testTripCreationAndDuplicatePrevention() {
        EmergencyTrip trip = tripService.createTrip("AMB-02", "H-01", "S", "H");
        assertNotNull(trip);
        assertEquals(TripStatus.CREATED, trip.getStatus());
        assertEquals("Route B", trip.getSelectedRoute());

        // Attempt duplicate trip creation for same ambulance must fail
        assertThrows(IllegalStateException.class, () -> {
            tripService.createTrip("AMB-02", "H-02", "S", "H");
        });

        // Start trip
        EmergencyTrip started = tripService.startTrip(trip.getId());
        assertEquals(TripStatus.ACTIVE, started.getStatus());
        assertNotNull(started.getStartTime());
    }

    @Test
    @DisplayName("Test 3 & Test 5: Traffic recalculation, hysteresis margin check, and driver accept")
    public void testScenario3And5_RecalculationHysteresisAndDriverAccept() {
        EmergencyTrip trip = tripService.createTrip("AMB-03", "H-01", "S", "H");
        trip = tripService.startTrip(trip.getId());
        assertEquals("Route B", trip.getSelectedRoute());

        // Scenario 5: Minor fluctuation (e.g. B2 travel time rises by 0.5 min, within 1.0 min hysteresis margin)
        RoadSegment b2 = roadSegmentRepository.findById("B2").orElseThrow();
        b2.setTravelTimeMinutes(6.5);
        roadSegmentRepository.save(b2);

        RouteDecisionEngine.DecisionResult minorRes = decisionEngine.evaluateActiveTrip(trip, "DIJKSTRA");
        assertEquals("NO_ACTION", minorRes.getAction(), "Should keep current route within hysteresis margin");

        // Scenario 3: Traffic suddenly increases (queue on B2: speed 12 km/h -> travel time 20 min -> Route B = 26 min)
        b2.setCurrentSmoothedSpeedKmph(12.0);
        b2.setTravelTimeMinutes(20.0);
        b2.setCongestionLevel(CongestionLevel.HIGH);
        roadSegmentRepository.save(b2);

        RouteDecisionEngine.DecisionResult queueRes = decisionEngine.evaluateActiveTrip(trip, "DIJKSTRA");
        assertEquals("RECOMMEND_SWITCH", queueRes.getAction());
        assertNotNull(queueRes.getRecommendation());
        assertEquals("Route C", queueRes.getRecommendation().getRecommendedRoute());

        // Driver accepts recommendation
        decisionEngine.acceptRecommendation(trip.getId(), queueRes.getRecommendation().getId());

        EmergencyTrip updatedTrip = tripRepository.findById(trip.getId()).orElseThrow();
        assertEquals("Route C", updatedTrip.getSelectedRoute());
        assertEquals(1, updatedTrip.getRouteChangeCount());

        // Verify RouteUpdate persistence
        List<RouteUpdate> updates = routeUpdateRepository.findByTripIdOrderByTimestampDesc(trip.getId());
        assertFalse(updates.isEmpty());
        assertEquals("Route B", updates.get(0).getPreviousRoute());
        assertEquals("Route C", updates.get(0).getNewRoute());
    }

    @Test
    @DisplayName("Test 4: Camera feed unavailable / stale data triggers distance baseline fallback and ETA uncertain")
    public void testScenario4_StaleDataTriggersFallback() {
        EmergencyTrip trip = tripService.createTrip("AMB-04", "H-01", "S", "H");
        trip = tripService.startTrip(trip.getId());

        // Clear existing seed observations so stale observations are the latest
        observationRepository.deleteAll();

        // Make observations older than 120s
        Instant oldTime = Instant.now().minusSeconds(300);
        List<RoadSegment> segments = roadSegmentRepository.findAll();
        for (RoadSegment s : segments) {
            TrafficObservation obs = new TrafficObservation(s.getId(), oldTime, 10, 20.0, 40.0, 10.0, 0.90, DataSource.SIMULATION);
            observationRepository.saveAndFlush(obs);
        }

        RouteDecisionEngine.DecisionResult result = decisionEngine.evaluateActiveTrip(trip, "DIJKSTRA");
        assertTrue(result.isDataStale(), "Stale data flag must be true");
        assertEquals("STALE_FALLBACK", result.getAction());
        assertTrue(result.getMessage().contains("UNCERTAIN"));
    }

    @Test
    @DisplayName("Test 6: Valid signal priority request executes safe phase sequence")
    public void testScenario6_SignalPriorityRequest() {
        SignalEvent event = signalPriorityService.requestPriority("J2", "NORTH_SOUTH", "AMB-01", "TRIP-001");
        assertNotNull(event);
        assertTrue(event.getAcknowledged());
        assertEquals("J2", event.getJunctionId());
    }

    @Test
    @DisplayName("Test 7: Ambulance reaches destination, completes trip and records arrival")
    public void testScenario7_AmbulanceArrivalAndCompletion() {
        EmergencyTrip trip = tripService.createTrip("AMB-01", "H-01", "S", "H");
        trip = tripService.startTrip(trip.getId());

        EmergencyTrip completed = tripService.completeTrip(trip.getId());
        assertEquals(TripStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getArrivalTime());
        assertEquals(0.0, completed.getRemainingDistanceKm(), 0.01);
        assertEquals(0.0, completed.getCurrentEtaMinutes(), 0.01);

        Ambulance amb = ambulanceRepository.findById("AMB-01").orElseThrow();
        assertEquals(AmbulanceStatus.AVAILABLE, amb.getStatus());
    }
}
