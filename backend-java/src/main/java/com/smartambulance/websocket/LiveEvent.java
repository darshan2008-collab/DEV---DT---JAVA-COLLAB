package com.smartambulance.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class LiveEvent {

    @JsonProperty("type")
    private String type;

    @JsonProperty("trip_id")
    private String tripId;

    @JsonProperty("previous_route")
    private String previousRoute;

    @JsonProperty("new_route")
    private String newRoute;

    @JsonProperty("previous_eta_minutes")
    private Double previousEtaMinutes;

    @JsonProperty("new_eta_minutes")
    private Double newEtaMinutes;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("data_age_s")
    private Long dataAgeSeconds;

    @JsonProperty("data_source")
    private String dataSource;

    @JsonProperty("timestamp")
    private Instant timestamp = Instant.now();

    public LiveEvent() {}

    public LiveEvent(String type, String tripId, String previousRoute, String newRoute, Double previousEtaMinutes, Double newEtaMinutes, String reason, Long dataAgeSeconds, String dataSource) {
        this.type = type;
        this.tripId = tripId;
        this.previousRoute = previousRoute;
        this.newRoute = newRoute;
        this.previousEtaMinutes = previousEtaMinutes != null ? Math.round(previousEtaMinutes * 10.0) / 10.0 : null;
        this.newEtaMinutes = newEtaMinutes != null ? Math.round(newEtaMinutes * 10.0) / 10.0 : null;
        this.reason = reason;
        this.dataAgeSeconds = dataAgeSeconds;
        this.dataSource = dataSource;
        this.timestamp = Instant.now();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getPreviousRoute() { return previousRoute; }
    public void setPreviousRoute(String previousRoute) { this.previousRoute = previousRoute; }
    public String getNewRoute() { return newRoute; }
    public void setNewRoute(String newRoute) { this.newRoute = newRoute; }
    public Double getPreviousEtaMinutes() { return previousEtaMinutes; }
    public void setPreviousEtaMinutes(Double previousEtaMinutes) { this.previousEtaMinutes = previousEtaMinutes; }
    public Double getNewEtaMinutes() { return newEtaMinutes; }
    public void setNewEtaMinutes(Double newEtaMinutes) { this.newEtaMinutes = newEtaMinutes; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getDataAgeSeconds() { return dataAgeSeconds; }
    public void setDataAgeSeconds(Long dataAgeSeconds) { this.dataAgeSeconds = dataAgeSeconds; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
