package com.smartambulance.routing;

import org.springframework.stereotype.Component;

import java.util.*;

@Component("aStarOptimizer")
public class AStarRouteOptimizer implements RouteOptimizer {

    private static final double MAX_SPEED_KMPH = 60.0; // Network maximum speed for admissible heuristic

    @Override
    public List<RouteCandidate> calculateAllRoutes(RoadGraph graph, String origin, String destination, boolean includeSignalDelay, double signalDelayMinutes) {
        // Find best path with A*
        RouteCandidate best = findBestRoute(graph, origin, destination, includeSignalDelay, signalDelayMinutes);

        // Also enumerate paths using A* ordering
        List<RouteCandidate> candidates = new ArrayList<>();
        List<GraphEdge> currentPath = new ArrayList<>();
        Set<String> visitedNodes = new HashSet<>();
        visitedNodes.add(origin);

        dfsFindPaths(graph, origin, destination, currentPath, visitedNodes, candidates, includeSignalDelay, signalDelayMinutes);
        candidates.sort(Comparator.comparingDouble(RouteCandidate::getEstimatedTimeMinutes));

        if (!candidates.isEmpty()) {
            candidates.get(0).setRecommended(true);
        }
        return candidates;
    }

    @Override
    public RouteCandidate findBestRoute(RoadGraph graph, String origin, String destination, boolean includeSignalDelay, double signalDelayMinutes) {
        Map<String, Double> gScore = new HashMap<>();
        Map<String, Double> fScore = new HashMap<>();
        Map<String, GraphEdge> prevEdge = new HashMap<>();
        Map<String, String> prevNode = new HashMap<>();

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));

        gScore.put(origin, 0.0);
        double initialH = heuristic(graph, origin, destination);
        fScore.put(origin, initialH);
        openSet.add(new AStarNode(origin, initialH));

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            if (current.nodeId.equals(destination)) {
                break;
            }

            for (GraphEdge edge : graph.getEdgesFrom(current.nodeId)) {
                if (edge.isClosed()) {
                    continue; // Exclude closed roads
                }

                double edgeCost = edge.getTravelTimeMinutes();
                if (includeSignalDelay && !edge.getToNode().equals(destination)) {
                    edgeCost += signalDelayMinutes;
                }

                double tentativeG = gScore.getOrDefault(current.nodeId, Double.MAX_VALUE) + edgeCost;
                if (tentativeG < gScore.getOrDefault(edge.getToNode(), Double.MAX_VALUE)) {
                    gScore.put(edge.getToNode(), tentativeG);
                    prevEdge.put(edge.getToNode(), edge);
                    prevNode.put(edge.getToNode(), current.nodeId);

                    double h = heuristic(graph, edge.getToNode(), destination);
                    double f = tentativeG + h;
                    fScore.put(edge.getToNode(), f);
                    openSet.add(new AStarNode(edge.getToNode(), f));
                }
            }
        }

        if (!gScore.containsKey(destination)) {
            return null;
        }

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

        String routeName = DijkstraRouteOptimizer.determineRouteName(pathEdges);
        RouteCandidate candidate = new RouteCandidate(routeName, pathEdges, totalDistance, totalTime, "A_STAR");
        candidate.setRecommended(true);
        return candidate;
    }

    private double heuristic(RoadGraph graph, String fromNode, String toNode) {
        if (fromNode.equals(toNode)) return 0.0;
        double straightDist = graph.calculateStraightLineDistanceKm(fromNode, toNode);
        return (straightDist / MAX_SPEED_KMPH) * 60.0; // Admissible remaining travel time
    }

    private void dfsFindPaths(RoadGraph graph, String current, String destination, List<GraphEdge> currentPath, Set<String> visitedNodes, List<RouteCandidate> candidates, boolean includeSignalDelay, double signalDelayMinutes) {
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
            String routeName = DijkstraRouteOptimizer.determineRouteName(currentPath);
            RouteCandidate candidate = new RouteCandidate(routeName, new ArrayList<>(currentPath), totalDist, totalTime, "A_STAR");
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
                dfsFindPaths(graph, nextNode, destination, currentPath, visitedNodes, candidates, includeSignalDelay, signalDelayMinutes);
                currentPath.remove(currentPath.size() - 1);
                visitedNodes.remove(nextNode);
            }
        }
    }

    private static class AStarNode {
        String nodeId;
        double f;
        AStarNode(String nodeId, double f) {
            this.nodeId = nodeId;
            this.f = f;
        }
    }
}
