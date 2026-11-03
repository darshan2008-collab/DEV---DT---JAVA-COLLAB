package com.smartambulance.api;

import com.smartambulance.api.dto.ObservationDto;
import com.smartambulance.domain.DataSource;
import com.smartambulance.domain.RoadSegment;
import com.smartambulance.domain.TrafficObservation;
import com.smartambulance.repository.RoadSegmentRepository;
import com.smartambulance.repository.TrafficObservationRepository;
import com.smartambulance.service.ObservationPreprocessingService;
import com.smartambulance.websocket.LiveEvent;
import com.smartambulance.websocket.LiveEventBroadcaster;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    @Autowired
    private ObservationPreprocessingService preprocessingService;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private TrafficObservationRepository observationRepository;

    @Autowired
    private LiveEventBroadcaster liveEventBroadcaster;

    @GetMapping("/current")
    public ResponseEntity<List<Map<String, Object>>> getCurrentTraffic() {
        List<RoadSegment> segments = roadSegmentRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        Instant now = Instant.now();

        for (RoadSegment s : segments) {
            Map<String, Object> map = new HashMap<>();
            map.put("segment_id", s.getId());
            map.put("source_junction", s.getSourceJunction());
            map.put("target_junction", s.getTargetJunction());
            map.put("distance_km", s.getDistanceKm());
            map.put("typical_speed_kmph", s.getTypicalSpeedKmph());
            map.put("speed_kmph", s.getCurrentSmoothedSpeedKmph());
            map.put("travel_time_minutes", s.getTravelTimeMinutes());
            map.put("congestion_level", s.getCongestionLevel());
            map.put("is_closed", s.getIsClosed());

            Optional<TrafficObservation> latest = observationRepository.findTopBySegmentIdOrderByTimestampDesc(s.getId());
            if (latest.isPresent()) {
                TrafficObservation obs = latest.get();
                map.put("vehicle_count", obs.getVehicleCount());
                map.put("density_veh_per_km", obs.getDensityVehPerKm());
                map.put("queue_m", obs.getQueueM());
                map.put("confidence", obs.getConfidence());
                map.put("data_source", obs.getDataSource());
                map.put("data_age_seconds", Duration.between(obs.getTimestamp(), now).getSeconds());
                map.put("last_updated", obs.getTimestamp().toString());
            } else {
                map.put("data_age_seconds", 9999);
                map.put("data_source", "SIMULATION");
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TrafficObservation>> getTrafficHistory(
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        Instant start = from != null ? Instant.parse(from) : Instant.now().minus(Duration.ofHours(2));
        Instant end = to != null ? Instant.parse(to) : Instant.now();

        if (segment != null) {
            return ResponseEntity.ok(observationRepository.findBySegmentIdAndTimestampBetweenOrderByTimestampAsc(segment, start, end));
        } else {
            return ResponseEntity.ok(observationRepository.findObservationsSince(start));
        }
    }

    @GetMapping("/events")
    public ResponseEntity<List<LiveEvent>> getLiveEventsFallback(@RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(liveEventBroadcaster.getRecentEvents(limit));
    }
}
