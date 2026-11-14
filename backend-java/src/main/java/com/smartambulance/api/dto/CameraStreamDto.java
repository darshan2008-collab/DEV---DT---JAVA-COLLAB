package com.smartambulance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CameraStreamDto {

    @NotBlank
    @Size(max = 50)
    private String streamId;

    @NotBlank
    @Size(max = 500)
    private String rtspUrl;

    @NotBlank
    private String segmentId;

    private Double locationLat;
    private Double locationLng;

    @Size(max = 150)
    private String label;

    private Integer fpsSample = 2;
    private Boolean enabled = true;

    // ── Getters & Setters ──────────────────────────────────────────────

    public String getStreamId() { return streamId; }
    public void setStreamId(String streamId) { this.streamId = streamId; }

    public String getRtspUrl() { return rtspUrl; }
    public void setRtspUrl(String rtspUrl) { this.rtspUrl = rtspUrl; }

    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }

    public Double getLocationLat() { return locationLat; }
    public void setLocationLat(Double locationLat) { this.locationLat = locationLat; }

    public Double getLocationLng() { return locationLng; }
    public void setLocationLng(Double locationLng) { this.locationLng = locationLng; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Integer getFpsSample() { return fpsSample; }
    public void setFpsSample(Integer fpsSample) { this.fpsSample = fpsSample; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
