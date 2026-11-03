package com.smartambulance.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ambulances")
public class Ambulance {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "plate_number", nullable = false, unique = true, length = 32)
    private String plateNumber;

    @Column(nullable = false, length = 64)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AmbulanceStatus status;

    @Column(name = "current_lat", nullable = false)
    private Double currentLat;

    @Column(name = "current_lon", nullable = false)
    private Double currentLon;

    @Column(name = "speed_kmph")
    private Double speedKmph = 0.0;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Ambulance() {}

    public Ambulance(String id, String plateNumber, String model, AmbulanceStatus status, Double currentLat, Double currentLon, Double speedKmph) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.model = model;
        this.status = status;
        this.currentLat = currentLat;
        this.currentLon = currentLon;
        this.speedKmph = speedKmph;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public AmbulanceStatus getStatus() { return status; }
    public void setStatus(AmbulanceStatus status) { this.status = status; }
    public Double getCurrentLat() { return currentLat; }
    public void setCurrentLat(Double currentLat) { this.currentLat = currentLat; }
    public Double getCurrentLon() { return currentLon; }
    public void setCurrentLon(Double currentLon) { this.currentLon = currentLon; }
    public Double getSpeedKmph() { return speedKmph; }
    public void setSpeedKmph(Double speedKmph) { this.speedKmph = speedKmph; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
