package com.smartambulance.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "signal_events")
public class SignalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "junction_id", nullable = false, length = 32)
    private String junctionId;

    @Column(nullable = false, length = 32)
    private String direction;

    @Column(name = "ambulance_id", nullable = false, length = 64)
    private String ambulanceId;

    @Column(name = "trip_id", length = 64)
    private String tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SignalState state;

    @Column(nullable = false)
    private Boolean acknowledged = false;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public SignalEvent() {}

    public SignalEvent(String junctionId, String direction, String ambulanceId, String tripId, SignalState state, Boolean acknowledged) {
        this.junctionId = junctionId;
        this.direction = direction;
        this.ambulanceId = ambulanceId;
        this.tripId = tripId;
        this.state = state;
        this.acknowledged = acknowledged;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJunctionId() { return junctionId; }
    public void setJunctionId(String junctionId) { this.junctionId = junctionId; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getAmbulanceId() { return ambulanceId; }
    public void setAmbulanceId(String ambulanceId) { this.ambulanceId = ambulanceId; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public SignalState getState() { return state; }
    public void setState(SignalState state) { this.state = state; }
    public Boolean getAcknowledged() { return acknowledged; }
    public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
