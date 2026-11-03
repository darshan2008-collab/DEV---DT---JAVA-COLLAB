package com.smartambulance.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 64)
    private String tripId;

    @Column(name = "recommended_route", nullable = false, length = 32)
    private String recommendedRoute;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "eta_minutes", nullable = false)
    private Double etaMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecommendationStatus status = RecommendationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "driver_decision", length = 32)
    private DriverDecision driverDecision = DriverDecision.NONE;

    @Column(name = "decision_timestamp")
    private Instant decisionTimestamp;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Recommendation() {}

    public Recommendation(String id, String tripId, String recommendedRoute, String reason, Double etaMinutes) {
        this.id = id;
        this.tripId = tripId;
        this.recommendedRoute = recommendedRoute;
        this.reason = reason;
        this.etaMinutes = etaMinutes;
        this.status = RecommendationStatus.PENDING;
        this.driverDecision = DriverDecision.NONE;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getRecommendedRoute() { return recommendedRoute; }
    public void setRecommendedRoute(String recommendedRoute) { this.recommendedRoute = recommendedRoute; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Double getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Double etaMinutes) { this.etaMinutes = etaMinutes; }
    public RecommendationStatus getStatus() { return status; }
    public void setStatus(RecommendationStatus status) { this.status = status; }
    public DriverDecision getDriverDecision() { return driverDecision; }
    public void setDriverDecision(DriverDecision driverDecision) { this.driverDecision = driverDecision; }
    public Instant getDecisionTimestamp() { return decisionTimestamp; }
    public void setDecisionTimestamp(Instant decisionTimestamp) { this.decisionTimestamp = decisionTimestamp; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
