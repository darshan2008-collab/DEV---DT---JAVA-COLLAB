package com.smartambulance.routing;

import org.springframework.stereotype.Component;

import java.util.*;

@Component("dijkstraOptimizer")
public class DijkstraRouteOptimizer implements RouteOptimizer {

    @Override
    public List<RouteCandidate> calculateAllRoutes(RoadGraph graph, String origin, String destination, boolean includeSignalDelay, double signalDelayMinutes) {
        List<RouteCandidate> candidates = new ArrayList<>();
        List<GraphEdge> currentPath = new ArrayList<>();
        Set<String> visitedNodes = new HashSet<>();
        visitedNodes.add(origin);

        dfsFindPaths(graph, origin, destination, currentPath, visitedNodes, candidates, includeSignalDelay, signalDelayMinutes, "DIJKSTRA");

        // Primary objective: Rank routes by travel time, not by vehicle count and not by distance
        candidates.sort(Comparator.comparingDouble(RouteCandidate::getEstimatedTimeMinutes));

        if (!candidates.isEmpty()) {
            candidates.get(0).setRecommended(true);
        }

        return candidates;
    }

    @Override
    public RouteCandidate findBestRoute(RoadGraph graph, String origin, String destination, boolean includeSignalDelay, double signalDelayMinutes) {
        // PriorityQueue Dijkstra implementation for shortest travel time
        Map<String, Double> minTime = new HashMap<>();
        Map<String, GraphEdge> prevEdge = new HashMap<>();
        Map<String, String> prevNode = new HashMap<>();

        // Priority queue ordered by travel time from origin
        PriorityQueue<NodeTime> pq = new PriorityQueue<>(Comparator.comparingDouble(nt -> nt.time));

        minTime.put(origin, 0.0);
        pq.add(new NodeTime(origin, 0.0));

        while (!pq.isEmpty()) {
            NodeTime current = pq.poll();
            if (current.time > minTime.getOrDefault(current.nodeId, Double.MAX_VALUE)) {
                continue;
            }
            if (current.nodeId.equals(destination)) {
                break;
            }

            for (GraphEdge edge : graph.getEdgesFrom(current.nodeId)) {
                if (edge.isClosed()) {
                    continue; // Exclude closed roads
                }
                double edgeCost = edge.getTravelTimeMinutes();
                // Add optional expected signal delay for intermediate junctions
                if (includeSignalDelay && !edge.getToNode().equals(destination)) {
                    edgeCost += signalDelayMinutes;
                }

                double newTime = current.time + edgeCost;
                if (newTime < minTime.getOrDefault(edge.getToNode(), Double.MAX_VALUE)) {
                    minTime.put(edge.getToNode(), newTime);
                    prevEdge.put(edge.getToNode(), edge);
                    prevNode.put(edge.getToNode(), current.nodeId);
                    pq.add(new NodeTime(edge.getToNode(), newTime));
                }
            }
        }

        if (!minTime.containsKey(destination)) {
            return null;
        }

        // Reconstruct path
        List<GraphEdge> pathEdges = new ArrayList<>();
        String curr = destination;
        double totalDistance = 0.0;
        double totalTime = 0.0;

        while (!curr.equals(origin) && prevEdge.containsKey(curr)) {
            GraphEdge edge = prevEdge.get(curr);
            pathEdges.add(0, edge);
            totalDistance += edge.getDistanceKm();
            totalTime += edge.getTravelTimeMinutes();
            if (includeSignalDelay && !edge.getToNode().equals(destination)) {
                totalTime += signalDelayMinutes;
            }
            curr = prevNode.get(curr);
        }

        String routeName = determineRouteName(pathEdges);
        RouteCandidate candidate = new RouteCandidate(routeName, pathEdges, totalDistance, totalTime, "DIJKSTRA");
        candidate.setRecommended(true);
        return candidate;
    }

    private void dfsFindPaths(RoadGraph graph, String current, String destination, List<GraphEdge> currentPath, Set<String> visitedNodes, List<RouteCandidate> candidates, boolean includeSignalDelay, double signalDelayMinutes, String algo) {
        if (current.equals(destination)) {
            double totalDist = 0.0;
            double totalTime = 0.0;
            for (int i = 0; i < currentPath.size(); i++) {
                GraphEdge edge = currentPath.get(i);
                totalDist += edge.getDistanceKm();
                totalTime += edge.getTravelTimeMinutes();
                if (includeSignalDelay && i < currentPath.size() - 1) {
                    totalTime += signalDelayMinutes;
                }
            }
            String routeName = determineRouteName(currentPath);
            RouteCandidate candidate = new RouteCandidate(routeName, new ArrayList<>(currentPath), totalDist, totalTime, algo);
            candidates.add(candidate);
            return;
        }

        for (GraphEdge edge : graph.getEdgesFrom(current)) {
            if (edge.isClosed()) {
                continue; // Exclude closed roads
            }
            String nextNode = edge.getToNode();
            if (!visitedNodes.contains(nextNode)) {
                visitedNodes.add(nextNode);
                currentPath.add(edge);
                dfsFindPaths(graph, nextNode, destination, currentPath, visitedNodes, candidates, includeSignalDelay, signalDelayMinutes, algo);
                currentPath.remove(currentPath.size() - 1);
                visitedNodes.remove(nextNode);
            }
        }
    }

    public static String determineRouteName(List<GraphEdge> edges) {
        if (edges == null || edges.isEmpty()) return "Unknown";
        String first = edges.get(0).getSegmentId();
        if (first.startsWith("A")) return "Route A";
        if (first.startsWith("B")) return "Route B";
        if (first.startsWith("C")) return "Route C";
        return "Route " + first;
    }

    private static class NodeTime {
        String nodeId;
        double time;
        NodeTime(String nodeId, double time) {
            this.nodeId = nodeId;
            this.time = time;
        }
    }
}
