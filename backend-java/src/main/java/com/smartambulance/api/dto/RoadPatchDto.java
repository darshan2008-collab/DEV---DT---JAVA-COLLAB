package com.smartambulance.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoadPatchDto {

    @JsonProperty("is_closed")
    private Boolean isClosed;

    @JsonProperty("typical_speed_kmph")
    private Double typicalSpeedKmph;

    public RoadPatchDto() {}

    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
    public Double getTypicalSpeedKmph() { return typicalSpeedKmph; }
    public void setTypicalSpeedKmph(Double typicalSpeedKmph) { this.typicalSpeedKmph = typicalSpeedKmph; }
}
