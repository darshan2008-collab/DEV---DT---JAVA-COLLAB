package com.smartambulance.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "routes")
public class RouteEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "trip_id", length = 64)
    private String tripId;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(name = "segments_json", nullable = false, columnDefinition = "TEXT")
    private String segmentsJson;

    @Column(name = "total_distance_km", nullable = false)
    private Double totalDistanceKm;

    @Column(name = "estimated_time_minutes", nullable = false)
    private Double estimatedTimeMinutes;

    @Column(length = 32)
    private String algorithm = "DIJKSTRA";

    @Column(name = "is_recommended")
    private Boolean isRecommended = false;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public RouteEntity() {}

    public RouteEntity(String id, String tripId, String name, String segmentsJson, Double totalDistanceKm, Double estimatedTimeMinutes, String algorithm, Boolean isRecommended) {
        this.id = id;
        this.tripId = tripId;
        this.name = name;
        this.segmentsJson = segmentsJson;
        this.totalDistanceKm = totalDistanceKm;
        this.estimatedTimeMinutes = estimatedTimeMinutes;
        this.algorithm = algorithm;
        this.isRecommended = isRecommended;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSegmentsJson() { return segmentsJson; }
    public void setSegmentsJson(String segmentsJson) { this.segmentsJson = segmentsJson; }
    public Double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(Double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }
    public Double getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
    public void setEstimatedTimeMinutes(Double estimatedTimeMinutes) { this.estimatedTimeMinutes = estimatedTimeMinutes; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public Boolean getIsRecommended() { return isRecommended; }
    public void setIsRecommended(Boolean isRecommended) { this.isRecommended = isRecommended; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
