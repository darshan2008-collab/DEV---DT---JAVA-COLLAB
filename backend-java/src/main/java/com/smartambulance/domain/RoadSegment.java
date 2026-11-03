package com.smartambulance.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "roads")
public class RoadSegment {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "source_junction", nullable = false, length = 32)
    private String sourceJunction;

    @Column(name = "target_junction", nullable = false, length = 32)
    private String targetJunction;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "typical_speed_kmph", nullable = false)
    private Double typicalSpeedKmph;

    @Column(name = "current_smoothed_speed_kmph", nullable = false)
    private Double currentSmoothedSpeedKmph;

    @Column(name = "travel_time_minutes", nullable = false)
    private Double travelTimeMinutes;

    @Column(name = "is_closed")
    private Boolean isClosed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "congestion_level", length = 32)
    private CongestionLevel congestionLevel = CongestionLevel.LOW;

    @Column(name = "last_observation_time")
    private Instant lastObservationTime;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public RoadSegment() {}

    public RoadSegment(String id, String sourceJunction, String targetJunction, Double distanceKm, Double typicalSpeedKmph, Double currentSmoothedSpeedKmph, Double travelTimeMinutes) {
        this.id = id;
        this.sourceJunction = sourceJunction;
        this.targetJunction = targetJunction;
        this.distanceKm = distanceKm;
        this.typicalSpeedKmph = typicalSpeedKmph;
        this.currentSmoothedSpeedKmph = currentSmoothedSpeedKmph;
        this.travelTimeMinutes = travelTimeMinutes;
        this.isClosed = false;
        this.congestionLevel = CongestionLevel.LOW;
        this.lastObservationTime = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceJunction() { return sourceJunction; }
    public void setSourceJunction(String sourceJunction) { this.sourceJunction = sourceJunction; }
    public String getTargetJunction() { return targetJunction; }
    public void setTargetJunction(String targetJunction) { this.targetJunction = targetJunction; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public Double getTypicalSpeedKmph() { return typicalSpeedKmph; }
    public void setTypicalSpeedKmph(Double typicalSpeedKmph) { this.typicalSpeedKmph = typicalSpeedKmph; }
    public Double getCurrentSmoothedSpeedKmph() { return currentSmoothedSpeedKmph; }
    public void setCurrentSmoothedSpeedKmph(Double currentSmoothedSpeedKmph) { this.currentSmoothedSpeedKmph = currentSmoothedSpeedKmph; }
    public Double getTravelTimeMinutes() { return travelTimeMinutes; }
    public void setTravelTimeMinutes(Double travelTimeMinutes) { this.travelTimeMinutes = travelTimeMinutes; }
    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
    public CongestionLevel getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(CongestionLevel congestionLevel) { this.congestionLevel = congestionLevel; }
    public Instant getLastObservationTime() { return lastObservationTime; }
    public void setLastObservationTime(Instant lastObservationTime) { this.lastObservationTime = lastObservationTime; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
