package com.smartambulance.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "route_updates")
public class RouteUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false, length = 64)
    private String tripId;

    @Column(name = "previous_route", nullable = false, length = 32)
    private String previousRoute;

    @Column(name = "new_route", nullable = false, length = 32)
    private String newRoute;

    @Column(name = "previous_eta_minutes", nullable = false)
    private Double previousEtaMinutes;

    @Column(name = "new_eta_minutes", nullable = false)
    private Double newEtaMinutes;

    @Column(name = "time_difference_minutes", nullable = false)
    private Double timeDifferenceMinutes;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "data_source", nullable = false, length = 32)
    private String dataSource;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    public RouteUpdate() {}

    public RouteUpdate(String tripId, String previousRoute, String newRoute, Double previousEtaMinutes, Double newEtaMinutes, Double timeDifferenceMinutes, String reason, String dataSource) {
        this.tripId = tripId;
        this.previousRoute = previousRoute;
        this.newRoute = newRoute;
        this.previousEtaMinutes = previousEtaMinutes;
        this.newEtaMinutes = newEtaMinutes;
        this.timeDifferenceMinutes = timeDifferenceMinutes;
        this.reason = reason;
        this.dataSource = dataSource;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Double getTimeDifferenceMinutes() { return timeDifferenceMinutes; }
    public void setTimeDifferenceMinutes(Double timeDifferenceMinutes) { this.timeDifferenceMinutes = timeDifferenceMinutes; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
