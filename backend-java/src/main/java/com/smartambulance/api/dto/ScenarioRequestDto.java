package com.smartambulance.api.dto;

import jakarta.validation.constraints.NotBlank;

public class ScenarioRequestDto {

    @NotBlank
    private String scenario; // NORMAL, QUEUE_ON_B2, SUDDEN_CONGESTION, FEED_LOST, ROAD_CLOSED, RECOVERY

    public ScenarioRequestDto() {}

    public ScenarioRequestDto(String scenario) {
        this.scenario = scenario;
    }

    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
}
