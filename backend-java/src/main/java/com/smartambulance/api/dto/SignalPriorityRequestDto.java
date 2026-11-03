package com.smartambulance.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class SignalPriorityRequestDto {

    @NotBlank
    private String direction = "NORTH_SOUTH";

    @JsonProperty("ambulance_id")
    private String ambulanceId = "AMB-01";

    @JsonProperty("trip_id")
    private String tripId;

    public SignalPriorityRequestDto() {}

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getAmbulanceId() { return ambulanceId; }
    public void setAmbulanceId(String ambulanceId) { this.ambulanceId = ambulanceId; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
}
