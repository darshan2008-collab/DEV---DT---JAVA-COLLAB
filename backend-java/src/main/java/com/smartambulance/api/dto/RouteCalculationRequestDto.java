package com.smartambulance.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class RouteCalculationRequestDto {

    @NotBlank
    private String origin = "S";

    @NotBlank
    private String destination = "H";

    private String algorithm = "DIJKSTRA";

    @JsonProperty("include_signal_delay")
    private Boolean includeSignalDelay = false;

    @JsonProperty("signal_delay_minutes")
    private Double signalDelayMinutes = 0.25;

    public RouteCalculationRequestDto() {}

    public RouteCalculationRequestDto(String origin, String destination, String algorithm) {
        this.origin = origin;
        this.destination = destination;
        this.algorithm = algorithm;
    }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public Boolean getIncludeSignalDelay() { return includeSignalDelay; }
    public void setIncludeSignalDelay(Boolean includeSignalDelay) { this.includeSignalDelay = includeSignalDelay; }
    public Double getSignalDelayMinutes() { return signalDelayMinutes; }
    public void setSignalDelayMinutes(Double signalDelayMinutes) { this.signalDelayMinutes = signalDelayMinutes; }
}
