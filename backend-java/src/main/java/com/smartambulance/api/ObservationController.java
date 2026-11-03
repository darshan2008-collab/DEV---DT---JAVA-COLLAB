package com.smartambulance.api;

import com.smartambulance.api.dto.ObservationDto;
import com.smartambulance.domain.DataSource;
import com.smartambulance.domain.TrafficObservation;
import com.smartambulance.service.ObservationPreprocessingService;
import com.smartambulance.websocket.LiveEvent;
import com.smartambulance.websocket.LiveEventBroadcaster;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ObservationController {

    @Autowired
    private ObservationPreprocessingService preprocessingService;

    @Autowired
    private LiveEventBroadcaster liveEventBroadcaster;

    @PostMapping("/observations")
    public ResponseEntity<Map<String, Object>> submitObservation(@Valid @RequestBody ObservationDto dto) {
        DataSource ds;
        try {
            ds = DataSource.valueOf(dto.getDataSource().toUpperCase());
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Invalid data_source: " + dto.getDataSource() + ". Must be one of SIMULATION, CAMERA_LIVE, CAMERA_REPLAY, CSV");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }

        TrafficObservation obs = new TrafficObservation(
                dto.getSegmentId(),
                dto.getTimestamp(),
                dto.getVehicleCount(),
                dto.getDensityVehPerKm(),
                dto.getSpeedKmph(),
                dto.getQueueM(),
                dto.getConfidence(),
                ds
        );

        try {
            ObservationPreprocessingService.PreprocessedResult result = preprocessingService.processIncomingObservation(obs);

            // Broadcast traffic updated event
            liveEventBroadcaster.broadcast(new LiveEvent(
                    "TRAFFIC_UPDATED",
                    null,
                    dto.getSegmentId(),
                    dto.getSegmentId(),
                    null,
                    result.getDerivedTravelTimeMinutes(),
                    "Traffic update on " + dto.getSegmentId() + ": " + Math.round(result.getSmoothedSpeedKmph()) + " km/h (" + result.getCongestionLevel() + ")",
                    0L,
                    dto.getDataSource()
            ));

            Map<String, Object> resp = new HashMap<>();
            resp.put("accepted", result.isAccepted());
            resp.put("duplicate", result.isDuplicate());
            resp.put("masked", result.isMasked());
            resp.put("message", result.getMessage());
            resp.put("segment_id", dto.getSegmentId());
            resp.put("smoothed_speed_kmph", result.getSmoothedSpeedKmph());
            resp.put("travel_time_minutes", result.getDerivedTravelTimeMinutes());
            resp.put("congestion_level", result.getCongestionLevel());

            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
    }
}
