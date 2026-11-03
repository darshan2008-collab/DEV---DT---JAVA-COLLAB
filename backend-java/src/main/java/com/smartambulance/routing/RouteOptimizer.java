package com.smartambulance.routing;

import java.util.List;

public interface RouteOptimizer {
    List<RouteCandidate> calculateAllRoutes(RoadGraph graph, String origin, String destination, boolean includeSignalDelay, double signalDelayMinutes);
    RouteCandidate findBestRoute(RoadGraph graph, String origin, String destination, boolean includeSignalDelay, double signalDelayMinutes);
}
