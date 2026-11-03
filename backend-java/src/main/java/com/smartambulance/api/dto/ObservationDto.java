package com.smartambulance.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class ObservationDto {

    @NotBlank(message = "segment_id is mandatory")
    @JsonProperty("segment_id")
    private String segmentId;

    @NotNull(message = "timestamp is mandatory")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @NotNull(message = "vehicle_count is mandatory")
    @Min(value = 0, message = "vehicle_count must be non-negative")
    @JsonProperty("vehicle_count")
    private Integer vehicleCount;

    @NotNull(message = "density_veh_per_km is mandatory")
    @Min(value = 0, message = "density_veh_per_km must be non-negative")
    @JsonProperty("density_veh_per_km")
    private Double densityVehPerKm;

    @NotNull(message = "speed_kmph is mandatory")
    @Min(value = 0, message = "speed_kmph must be non-negative")
    @JsonProperty("speed_kmph")
    private Double speedKmph;

    @NotNull(message = "queue_m is mandatory")
    @Min(value = 0, message = "queue_m must be non-negative")
    @JsonProperty("queue_m")
    private Double queueM;

    @NotNull(message = "confidence is mandatory")
    @Min(value = 0, message = "confidence must be >= 0.0")
    @Max(value = 1, message = "confidence must be <= 1.0")
    @JsonProperty("confidence")
    private Double confidence;

    @NotBlank(message = "data_source is mandatory")
    @JsonProperty("data_source")
    private String dataSource;

    public ObservationDto() {}

    public ObservationDto(String segmentId, Instant timestamp, Integer vehicleCount, Double densityVehPerKm, Double speedKmph, Double queueM, Double confidence, String dataSource) {
        this.segmentId = segmentId;
        this.timestamp = timestamp;
        this.vehicleCount = vehicleCount;
        this.densityVehPerKm = densityVehPerKm;
        this.speedKmph = speedKmph;
        this.queueM = queueM;
        this.confidence = confidence;
        this.dataSource = dataSource;
    }

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
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
}
