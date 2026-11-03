package com.smartambulance.routing;

import java.util.ArrayList;
import java.util.List;

public class RouteCandidate {
    private String routeName;
    private List<GraphEdge> segments;
    private double totalDistanceKm;
    private double estimatedTimeMinutes;
    private String algorithm;
    private boolean isRecommended;
    private String reason;
    private List<String> warnings;

    public RouteCandidate() {
        this.segments = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public RouteCandidate(String routeName, List<GraphEdge> segments, double totalDistanceKm, double estimatedTimeMinutes, String algorithm) {
        this.routeName = routeName;
        this.segments = segments != null ? segments : new ArrayList<>();
        this.totalDistanceKm = Math.round(totalDistanceKm * 100.0) / 100.0;
        this.estimatedTimeMinutes = Math.round(estimatedTimeMinutes * 10.0) / 10.0;
        this.algorithm = algorithm;
        this.isRecommended = false;
        this.reason = "";
        this.warnings = new ArrayList<>();
    }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public List<GraphEdge> getSegments() { return segments; }
    public void setSegments(List<GraphEdge> segments) { this.segments = segments; }
    public double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }
    public double getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
    public void setEstimatedTimeMinutes(double estimatedTimeMinutes) { this.estimatedTimeMinutes = estimatedTimeMinutes; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public boolean isRecommended() { return isRecommended; }
    public void setRecommended(boolean recommended) { isRecommended = recommended; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public void addWarning(String warning) { this.warnings.add(warning); }
}
