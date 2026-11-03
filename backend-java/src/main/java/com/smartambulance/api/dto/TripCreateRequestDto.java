package com.smartambulance.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class TripCreateRequestDto {

    @NotBlank
    @JsonProperty("ambulance_id")
    private String ambulanceId;

    @NotBlank
    @JsonProperty("hospital_id")
    private String hospitalId;

    @NotBlank
    private String origin = "S";

    @NotBlank
    private String destination = "H";

    public TripCreateRequestDto() {}

    public TripCreateRequestDto(String ambulanceId, String hospitalId, String origin, String destination) {
        this.ambulanceId = ambulanceId;
        this.hospitalId = hospitalId;
        this.origin = origin;
        this.destination = destination;
    }

    public String getAmbulanceId() { return ambulanceId; }
    public void setAmbulanceId(String ambulanceId) { this.ambulanceId = ambulanceId; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}
