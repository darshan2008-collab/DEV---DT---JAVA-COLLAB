package com.smartambulance.routing;

import com.smartambulance.domain.CongestionLevel;
import java.time.Instant;

public class GraphEdge {
    private final String segmentId;
    private final String fromNode;
    private final String toNode;
    private final double distanceKm;
    private final double typicalSpeedKmph;
    private double currentSmoothedSpeedKmph;
    private double travelTimeMinutes;
    private boolean isClosed;
    private CongestionLevel congestionLevel;
    private Instant lastObservationTime;

    public GraphEdge(String segmentId, String fromNode, String toNode, double distanceKm, double typicalSpeedKmph, double currentSmoothedSpeedKmph, double travelTimeMinutes, boolean isClosed, CongestionLevel congestionLevel, Instant lastObservationTime) {
        this.segmentId = segmentId;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.distanceKm = distanceKm;
        this.typicalSpeedKmph = typicalSpeedKmph;
        this.currentSmoothedSpeedKmph = currentSmoothedSpeedKmph;
        this.travelTimeMinutes = travelTimeMinutes;
        this.isClosed = isClosed;
        this.congestionLevel = congestionLevel;
        this.lastObservationTime = lastObservationTime;
    }

    public String getSegmentId() { return segmentId; }
    public String getFromNode() { return fromNode; }
    public String getToNode() { return toNode; }
    public double getDistanceKm() { return distanceKm; }
    public double getTypicalSpeedKmph() { return typicalSpeedKmph; }
    public double getCurrentSmoothedSpeedKmph() { return currentSmoothedSpeedKmph; }
    public void setCurrentSmoothedSpeedKmph(double currentSmoothedSpeedKmph) { this.currentSmoothedSpeedKmph = currentSmoothedSpeedKmph; }
    public double getTravelTimeMinutes() { return travelTimeMinutes; }
    public void setTravelTimeMinutes(double travelTimeMinutes) { this.travelTimeMinutes = travelTimeMinutes; }
    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean closed) { isClosed = closed; }
    public CongestionLevel getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(CongestionLevel congestionLevel) { this.congestionLevel = congestionLevel; }
    public Instant getLastObservationTime() { return lastObservationTime; }
    public void setLastObservationTime(Instant lastObservationTime) { this.lastObservationTime = lastObservationTime; }
}
