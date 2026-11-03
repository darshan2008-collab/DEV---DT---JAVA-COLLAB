package com.smartambulance.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vehicle_detection_summaries")
public class VehicleDetectionSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camera_id", nullable = false, length = 64)
    private String cameraId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "total_vehicles", nullable = false)
    private Integer totalVehicles;

    @Column(name = "car_count")
    private Integer carCount = 0;

    @Column(name = "bus_count")
    private Integer busCount = 0;

    @Column(name = "truck_count")
    private Integer truckCount = 0;

    @Column(name = "motorcycle_count")
    private Integer motorcycleCount = 0;

    @Column(name = "avg_speed_kmph")
    private Double avgSpeedKmph = 0.0;

    @Column(name = "queue_length_m")
    private Double queueLengthM = 0.0;

    @Column
    private Double confidence = 0.85;

    public VehicleDetectionSummary() {}

    public VehicleDetectionSummary(String cameraId, Instant timestamp, Integer totalVehicles, Integer carCount, Integer busCount, Integer truckCount, Integer motorcycleCount, Double avgSpeedKmph, Double queueLengthM, Double confidence) {
        this.cameraId = cameraId;
        this.timestamp = timestamp;
        this.totalVehicles = totalVehicles;
        this.carCount = carCount;
        this.busCount = busCount;
        this.truckCount = truckCount;
        this.motorcycleCount = motorcycleCount;
        this.avgSpeedKmph = avgSpeedKmph;
        this.queueLengthM = queueLengthM;
        this.confidence = confidence;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCameraId() { return cameraId; }
    public void setCameraId(String cameraId) { this.cameraId = cameraId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Integer getTotalVehicles() { return totalVehicles; }
    public void setTotalVehicles(Integer totalVehicles) { this.totalVehicles = totalVehicles; }
    public Integer getCarCount() { return carCount; }
    public void setCarCount(Integer carCount) { this.carCount = carCount; }
    public Integer getBusCount() { return busCount; }
    public void setBusCount(Integer busCount) { this.busCount = busCount; }
    public Integer getTruckCount() { return truckCount; }
    public void setTruckCount(Integer truckCount) { this.truckCount = truckCount; }
    public Integer getMotorcycleCount() { return motorcycleCount; }
    public void setMotorcycleCount(Integer motorcycleCount) { this.motorcycleCount = motorcycleCount; }
    public Double getAvgSpeedKmph() { return avgSpeedKmph; }
    public void setAvgSpeedKmph(Double avgSpeedKmph) { this.avgSpeedKmph = avgSpeedKmph; }
    public Double getQueueLengthM() { return queueLengthM; }
    public void setQueueLengthM(Double queueLengthM) { this.queueLengthM = queueLengthM; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
