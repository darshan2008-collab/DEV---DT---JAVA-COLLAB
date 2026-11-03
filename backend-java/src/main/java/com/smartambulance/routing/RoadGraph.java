package com.smartambulance.routing;

import com.smartambulance.domain.RoadSegment;

import java.util.*;

public class RoadGraph {

    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Map<String, List<GraphEdge>> adjacencyList = new HashMap<>();

    public RoadGraph() {
        // Initialize default demo junctions with geographic coordinates in Karur
        nodes.put("S", new GraphNode("S", 10.9400, 78.0700));
        nodes.put("J1", new GraphNode("J1", 10.9500, 78.0550));
        nodes.put("J2", new GraphNode("J2", 10.9550, 78.0750));
        nodes.put("J3", new GraphNode("J3", 10.9600, 78.0950));
        nodes.put("H", new GraphNode("H", 10.9700, 78.0800));
    }

    public void addNode(GraphNode node) {
        nodes.put(node.getId(), node);
    }

    public void addEdge(GraphEdge edge) {
        adjacencyList.computeIfAbsent(edge.getFromNode(), k -> new ArrayList<>()).add(edge);
    }

    public GraphNode getNode(String id) {
        return nodes.get(id);
    }

    public Map<String, GraphNode> getNodes() {
        return nodes;
    }

    public List<GraphEdge> getEdgesFrom(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    public Optional<GraphEdge> getEdge(String segmentId) {
        for (List<GraphEdge> edges : adjacencyList.values()) {
            for (GraphEdge edge : edges) {
                if (edge.getSegmentId().equalsIgnoreCase(segmentId)) {
                    return Optional.of(edge);
                }
            }
        }
        return Optional.empty();
    }

    public double calculateStraightLineDistanceKm(String fromId, String toId) {
        GraphNode n1 = nodes.get(fromId);
        GraphNode n2 = nodes.get(toId);
        if (n1 == null || n2 == null) {
            return 0.0;
        }
        // Haversine formula
        double lat1 = Math.toRadians(n1.getLatitude());
        double lon1 = Math.toRadians(n1.getLongitude());
        double lat2 = Math.toRadians(n2.getLatitude());
        double lon2 = Math.toRadians(n2.getLongitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double earthRadiusKm = 6371.0;
        return earthRadiusKm * c;
    }

    public static RoadGraph fromSegments(List<RoadSegment> segments) {
        RoadGraph graph = new RoadGraph();
        for (RoadSegment segment : segments) {
            GraphEdge edge = new GraphEdge(
                    segment.getId(),
                    segment.getSourceJunction(),
                    segment.getTargetJunction(),
                    segment.getDistanceKm(),
                    segment.getTypicalSpeedKmph(),
                    segment.getCurrentSmoothedSpeedKmph(),
                    segment.getTravelTimeMinutes(),
                    segment.getIsClosed() != null && segment.getIsClosed(),
                    segment.getCongestionLevel(),
                    segment.getLastObservationTime()
            );
            graph.addEdge(edge);
        }
        return graph;
    }
}
