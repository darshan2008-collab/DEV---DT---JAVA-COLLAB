package com.smartambulance.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "emergency_trips")
public class EmergencyTrip {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "ambulance_id", nullable = false, length = 64)
    private String ambulanceId;

    @Column(name = "hospital_id", nullable = false, length = 64)
    private String hospitalId;

    @Column(nullable = false, length = 64)
    private String origin;

    @Column(nullable = false, length = 64)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TripStatus status = TripStatus.CREATED;

    @Column(name = "selected_route", nullable = false, length = 32)
    private String selectedRoute; // Route A, Route B, Route C

    @Column(name = "initial_eta_minutes", nullable = false)
    private Double initialEtaMinutes;

    @Column(name = "current_eta_minutes", nullable = false)
    private Double currentEtaMinutes;

    @Column(name = "remaining_distance_km", nullable = false)
    private Double remainingDistanceKm;

    @Column(name = "route_change_count")
    private Integer routeChangeCount = 0;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "arrival_time")
    private Instant arrivalTime;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public EmergencyTrip() {}

    public EmergencyTrip(String id, String ambulanceId, String hospitalId, String origin, String destination, String selectedRoute, Double initialEtaMinutes, Double remainingDistanceKm) {
        this.id = id;
        this.ambulanceId = ambulanceId;
        this.hospitalId = hospitalId;
        this.origin = origin;
        this.destination = destination;
        this.selectedRoute = selectedRoute;
        this.initialEtaMinutes = initialEtaMinutes;
        this.currentEtaMinutes = initialEtaMinutes;
        this.remainingDistanceKm = remainingDistanceKm;
        this.routeChangeCount = 0;
        this.status = TripStatus.CREATED;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAmbulanceId() { return ambulanceId; }
    public void setAmbulanceId(String ambulanceId) { this.ambulanceId = ambulanceId; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
    public String getSelectedRoute() { return selectedRoute; }
    public void setSelectedRoute(String selectedRoute) { this.selectedRoute = selectedRoute; }
    public Double getInitialEtaMinutes() { return initialEtaMinutes; }
    public void setInitialEtaMinutes(Double initialEtaMinutes) { this.initialEtaMinutes = initialEtaMinutes; }
    public Double getCurrentEtaMinutes() { return currentEtaMinutes; }
    public void setCurrentEtaMinutes(Double currentEtaMinutes) { this.currentEtaMinutes = currentEtaMinutes; }
    public Double getRemainingDistanceKm() { return remainingDistanceKm; }
    public void setRemainingDistanceKm(Double remainingDistanceKm) { this.remainingDistanceKm = remainingDistanceKm; }
    public Integer getRouteChangeCount() { return routeChangeCount; }
    public void setRouteChangeCount(Integer routeChangeCount) { this.routeChangeCount = routeChangeCount; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(Instant arrivalTime) { this.arrivalTime = arrivalTime; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
