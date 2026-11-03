package com.smartambulance.routing;

import org.springframework.stereotype.Component;

import java.util.*;

@Component("distanceBaselinePlanner")
public class DistanceBaselinePlanner {

    public RouteCandidate findShortestDistanceRoute(RoadGraph graph, String origin, String destination) {
        List<RouteCandidate> candidates = new ArrayList<>();
        List<GraphEdge> currentPath = new ArrayList<>();
        Set<String> visitedNodes = new HashSet<>();
        visitedNodes.add(origin);

        dfsFindPaths(graph, origin, destination, currentPath, visitedNodes, candidates);

        // Sort by pure distance
        candidates.sort(Comparator.comparingDouble(RouteCandidate::getTotalDistanceKm));

        if (!candidates.isEmpty()) {
            RouteCandidate best = candidates.get(0);
            best.setRecommended(true);
            best.setReason("Baseline static route: shortest geographic distance (" + best.getTotalDistanceKm() + " km).");
            return best;
        }
        return null;
    }

    private void dfsFindPaths(RoadGraph graph, String current, String destination, List<GraphEdge> currentPath, Set<String> visitedNodes, List<RouteCandidate> candidates) {
        if (current.equals(destination)) {
            double totalDist = 0.0;
            double totalTime = 0.0;
            for (GraphEdge edge : currentPath) {
                totalDist += edge.getDistanceKm();
                totalTime += edge.getTravelTimeMinutes();
            }
            String routeName = DijkstraRouteOptimizer.determineRouteName(currentPath);
            RouteCandidate candidate = new RouteCandidate(routeName, new ArrayList<>(currentPath), totalDist, totalTime, "DISTANCE_BASELINE");
            candidates.add(candidate);
            return;
        }

        for (GraphEdge edge : graph.getEdgesFrom(current)) {
            if (edge.isClosed()) {
                continue;
            }
            String nextNode = edge.getToNode();
            if (!visitedNodes.contains(nextNode)) {
                visitedNodes.add(nextNode);
                currentPath.add(edge);
                dfsFindPaths(graph, nextNode, destination, currentPath, visitedNodes, candidates);
                currentPath.remove(currentPath.size() - 1);
                visitedNodes.remove(nextNode);
            }
        }
    }
}
