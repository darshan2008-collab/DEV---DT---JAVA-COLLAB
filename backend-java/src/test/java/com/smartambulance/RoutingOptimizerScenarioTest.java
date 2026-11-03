package com.smartambulance;

import com.smartambulance.domain.CongestionLevel;
import com.smartambulance.routing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoutingOptimizerScenarioTest {

    private RoadGraph graph;
    private DijkstraRouteOptimizer dijkstra;
    private AStarRouteOptimizer aStar;
    private DistanceBaselinePlanner baseline;

    @BeforeEach
    public void setup() {
        dijkstra = new DijkstraRouteOptimizer();
        aStar = new AStarRouteOptimizer();
        baseline = new DistanceBaselinePlanner();
        graph = new RoadGraph();

        // Exact Section 5 Default Network:
        // A1: S -> J1 (3 km, 30 km/h -> 6 min)
        // A2: J1 -> H (3 km, 15 km/h -> 12 min)
        // B1: S -> J2 (4 km, 40 km/h -> 6 min)
        // B2: J2 -> H (4 km, 40 km/h -> 6 min)
        // C1: S -> J3 (5 km, 40 km/h -> 7.5 min)
        // C2: J3 -> H (5 km, 40 km/h -> 7.5 min)
        graph.addEdge(new GraphEdge("A1", "S", "J1", 3.0, 30.0, 30.0, 6.0, false, CongestionLevel.LOW, Instant.now()));
        graph.addEdge(new GraphEdge("A2", "J1", "H", 3.0, 15.0, 15.0, 12.0, false, CongestionLevel.MEDIUM, Instant.now()));
        graph.addEdge(new GraphEdge("B1", "S", "J2", 4.0, 40.0, 40.0, 6.0, false, CongestionLevel.LOW, Instant.now()));
        graph.addEdge(new GraphEdge("B2", "J2", "H", 4.0, 40.0, 40.0, 6.0, false, CongestionLevel.LOW, Instant.now()));
        graph.addEdge(new GraphEdge("C1", "S", "J3", 5.0, 40.0, 40.0, 7.5, false, CongestionLevel.LOW, Instant.now()));
        graph.addEdge(new GraphEdge("C2", "J3", "H", 5.0, 40.0, 40.0, 7.5, false, CongestionLevel.LOW, Instant.now()));
    }

    @Test
    @DisplayName("Test 1: Normal traffic - Route B is chosen (12 min) despite longer distance than Route A")
    public void testScenario1_BothRoutesClear_SelectLowestTravelTime() {
        List<RouteCandidate> routes = dijkstra.calculateAllRoutes(graph, "S", "H", false, 0.0);
        assertEquals(3, routes.size());

        RouteCandidate best = routes.get(0);
        assertEquals("Route B", best.getRouteName());
        assertEquals(12.0, best.getEstimatedTimeMinutes(), 0.1);
        assertEquals(8.0, best.getTotalDistanceKm(), 0.1);

        // Verify all 3 routes travel times from Section 5
        RouteCandidate routeA = routes.stream().filter(r -> r.getRouteName().equals("Route A")).findFirst().orElseThrow();
        RouteCandidate routeB = routes.stream().filter(r -> r.getRouteName().equals("Route B")).findFirst().orElseThrow();
        RouteCandidate routeC = routes.stream().filter(r -> r.getRouteName().equals("Route C")).findFirst().orElseThrow();

        assertEquals(18.0, routeA.getEstimatedTimeMinutes(), 0.1);
        assertEquals(12.0, routeB.getEstimatedTimeMinutes(), 0.1);
        assertEquals(15.0, routeC.getEstimatedTimeMinutes(), 0.1);
    }

    @Test
    @DisplayName("Distance baseline picks Route A (6 km) solely based on shortest distance")
    public void testDistanceBaseline_PicksShortestDistance() {
        RouteCandidate baselineRoute = baseline.findShortestDistanceRoute(graph, "S", "H");
        assertNotNull(baselineRoute);
        assertEquals("Route A", baselineRoute.getRouteName());
        assertEquals(6.0, baselineRoute.getTotalDistanceKm(), 0.1);
        assertEquals(18.0, baselineRoute.getEstimatedTimeMinutes(), 0.1);
    }

    @Test
    @DisplayName("Test 2: Queue forms on B2 (speed drops to 12 km/h) - Route C (15 min) is preferred over Route B (26 min)")
    public void testScenario2_QueueOnB2_PreferFasterAlternativeC() {
        // Update B2 speed to 12 km/h: travel time = 4.0 / 12.0 * 60 = 20.0 min
        GraphEdge b2 = graph.getEdge("B2").orElseThrow();
        b2.setCurrentSmoothedSpeedKmph(12.0);
        b2.setTravelTimeMinutes(20.0);
        b2.setCongestionLevel(CongestionLevel.HIGH);

        List<RouteCandidate> routes = dijkstra.calculateAllRoutes(graph, "S", "H", false, 0.0);
        RouteCandidate best = routes.get(0);

        // Section 5 assertion: Route C (15 min) is the fastest after queue
        assertEquals("Route C", best.getRouteName());
        assertEquals(15.0, best.getEstimatedTimeMinutes(), 0.1);

        // Route B estimate has risen to 26 min (6 min + 20 min)
        RouteCandidate routeB = routes.stream().filter(r -> r.getRouteName().equals("Route B")).findFirst().orElseThrow();
        assertEquals(26.0, routeB.getEstimatedTimeMinutes(), 0.1);
    }

    @Test
    @DisplayName("Dijkstra and A* return identical costs and route recommendations")
    public void testDijkstraAndAStar_ReturnIdenticalCosts() {
        RouteCandidate dijkstraBest = dijkstra.findBestRoute(graph, "S", "H", false, 0.0);
        RouteCandidate aStarBest = aStar.findBestRoute(graph, "S", "H", false, 0.0);

        assertNotNull(dijkstraBest);
        assertNotNull(aStarBest);
        assertEquals(dijkstraBest.getRouteName(), aStarBest.getRouteName());
        assertEquals(dijkstraBest.getEstimatedTimeMinutes(), aStarBest.getEstimatedTimeMinutes(), 0.01);
        assertEquals(dijkstraBest.getTotalDistanceKm(), aStarBest.getTotalDistanceKm(), 0.01);
    }

    @Test
    @DisplayName("Closed road exclusion: Closed segment is excluded from candidate routes")
    public void testClosedRoadExclusion() {
        // Close segment B1
        GraphEdge b1 = graph.getEdge("B1").orElseThrow();
        b1.setClosed(true);

        List<RouteCandidate> routes = dijkstra.calculateAllRoutes(graph, "S", "H", false, 0.0);

        // Route B must NOT be present
        boolean containsRouteB = routes.stream().anyMatch(r -> r.getRouteName().equals("Route B"));
        assertFalse(containsRouteB, "Route B should be excluded when B1 is closed");

        // Best remaining route is Route C (15 min < Route A 18 min)
        assertEquals("Route C", routes.get(0).getRouteName());
    }
}
