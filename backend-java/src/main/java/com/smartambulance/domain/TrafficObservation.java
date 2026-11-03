package com.smartambulance.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "traffic_observations", indexes = {
    @Index(name = "idx_obs_segment_time", columnList = "segment_id, timestamp DESC")
})
public class TrafficObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "segment_id", nullable = false, length = 32)
    private String segmentId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "vehicle_count", nullable = false)
    private Integer vehicleCount;

    @Column(name = "density_veh_per_km", nullable = false)
    private Double densityVehPerKm;

    @Column(name = "speed_kmph", nullable = false)
    private Double speedKmph;

    @Column(name = "queue_m", nullable = false)
    private Double queueM;

    @Column(nullable = false)
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 32)
    private DataSource dataSource;

    @Column(name = "is_masked")
    private Boolean isMasked = false;

    @Column(name = "mask_reason", length = 128)
    private String maskReason;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public TrafficObservation() {}

    public TrafficObservation(String segmentId, Instant timestamp, Integer vehicleCount, Double densityVehPerKm, Double speedKmph, Double queueM, Double confidence, DataSource dataSource) {
        this.segmentId = segmentId;
        this.timestamp = timestamp;
        this.vehicleCount = vehicleCount;
        this.densityVehPerKm = densityVehPerKm;
        this.speedKmph = speedKmph;
        this.queueM = queueM;
        this.confidence = confidence;
        this.dataSource = dataSource;
        this.isMasked = false;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Integer getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(Integer vehicleCount) { this.vehicleCount = vehicleCount; }
    public Double getDensityVehPerKm() { return densityVehPerKm; }
    public void setDensityVehPerKm(Double densityVehPerKm) { this.densityVehPerKm = densityVehPerKm; }
    public Double getSpeedKmph() { return speedKmph; }
    public void setSpeedKmph(Double speedKmph) { this.speedKmph = speedKmph; }
    public Double getQueueM() { return queueM; }
    public void setQueueM(Double queueM) { this.queueM = queueM; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public DataSource getDataSource() { return dataSource; }
    public void setDataSource(DataSource dataSource) { this.dataSource = dataSource; }
    public Boolean getIsMasked() { return isMasked; }
    public void setIsMasked(Boolean isMasked) { this.isMasked = isMasked; }
    public String getMaskReason() { return maskReason; }
    public void setMaskReason(String maskReason) { this.maskReason = maskReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
