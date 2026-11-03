package com.smartambulance.service;

import com.smartambulance.domain.CongestionLevel;
import com.smartambulance.domain.RoadSegment;
import com.smartambulance.domain.TrafficObservation;
import com.smartambulance.repository.RoadSegmentRepository;
import com.smartambulance.repository.TrafficObservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ObservationPreprocessingService {

    private static final Logger log = LoggerFactory.getLogger(ObservationPreprocessingService.class);

    @Value("${smartambulance.preprocessing.min-confidence:0.5}")
    private double minConfidence;

    @Value("${smartambulance.preprocessing.speed-outlier-multiplier:1.5}")
    private double speedOutlierMultiplier;

    @Value("${smartambulance.preprocessing.max-interpolation-gap-seconds:120}")
    private long maxInterpolationGapSeconds;

    @Value("${smartambulance.preprocessing.median-window-size:5}")
    private int medianWindowSize;

    @Autowired
    private TrafficObservationRepository observationRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    public static class PreprocessedResult {
        private final TrafficObservation observation;
        private final boolean accepted;
        private final boolean isDuplicate;
        private final boolean isMasked;
        private final String message;
        private final double smoothedSpeedKmph;
        private final double derivedTravelTimeMinutes;
        private final CongestionLevel congestionLevel;

        public PreprocessedResult(TrafficObservation observation, boolean accepted, boolean isDuplicate, boolean isMasked, String message, double smoothedSpeedKmph, double derivedTravelTimeMinutes, CongestionLevel congestionLevel) {
            this.observation = observation;
            this.accepted = accepted;
            this.isDuplicate = isDuplicate;
            this.isMasked = isMasked;
            this.message = message;
            this.smoothedSpeedKmph = smoothedSpeedKmph;
            this.derivedTravelTimeMinutes = derivedTravelTimeMinutes;
            this.congestionLevel = congestionLevel;
        }

        public TrafficObservation getObservation() { return observation; }
        public boolean isAccepted() { return accepted; }
        public boolean isDuplicate() { return isDuplicate; }
        public boolean isMasked() { return isMasked; }
        public String getMessage() { return message; }
        public double getSmoothedSpeedKmph() { return smoothedSpeedKmph; }
        public double getDerivedTravelTimeMinutes() { return derivedTravelTimeMinutes; }
        public CongestionLevel getCongestionLevel() { return congestionLevel; }
    }

    @Transactional
    public PreprocessedResult processIncomingObservation(TrafficObservation raw) {
        // Validation: known segment
        Optional<RoadSegment> roadOpt = roadSegmentRepository.findById(raw.getSegmentId());
        if (roadOpt.isEmpty()) {
            throw new IllegalArgumentException("Unknown road segment: " + raw.getSegmentId());
        }
        RoadSegment road = roadOpt.get();

        // Normalize timestamp to millisecond precision for database compatibility
        if (raw.getTimestamp() != null) {
            raw.setTimestamp(raw.getTimestamp().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        }

        // 1. Drop duplicate (timestamp, segment_id)
        if (observationRepository.existsBySegmentIdAndTimestamp(raw.getSegmentId(), raw.getTimestamp())) {
            log.info("Duplicate observation dropped for segment {} at {}", raw.getSegmentId(), raw.getTimestamp());
            return new PreprocessedResult(raw, false, true, false, "Duplicate observation dropped", road.getCurrentSmoothedSpeedKmph(), road.getTravelTimeMinutes(), road.getCongestionLevel());
        }

        boolean shouldMask = false;
        String maskReason = null;

        // 2. Mask frames with confidence < 0.5 as missing, so poor video is never read as a clear road
        if (raw.getConfidence() == null || raw.getConfidence() < minConfidence) {
            shouldMask = true;
            maskReason = "Low confidence: " + raw.getConfidence() + " < threshold " + minConfidence;
        }

        // 3. Mask outlier speeds (negative, or > 1.5 x reference speed)
        double typicalSpeed = road.getTypicalSpeedKmph();
        if (raw.getSpeedKmph() == null || raw.getSpeedKmph() < 0.0 || raw.getSpeedKmph() > (typicalSpeed * speedOutlierMultiplier)) {
            shouldMask = true;
            maskReason = "Speed outlier: " + raw.getSpeedKmph() + " km/h (reference typical " + typicalSpeed + " km/h)";
        }

        raw.setIsMasked(shouldMask);
        raw.setMaskReason(maskReason);

        // Save observation in database immediately flushed
        TrafficObservation saved = observationRepository.saveAndFlush(raw);

        // 4. Interpolate gaps of up to 2 minutes only
        Optional<TrafficObservation> prevObs = observationRepository.findTopBySegmentIdAndIsMaskedFalseOrderByTimestampDesc(raw.getSegmentId());
        if (shouldMask && prevObs.isPresent()) {
            long gapSeconds = Duration.between(prevObs.get().getTimestamp(), raw.getTimestamp()).abs().getSeconds();
            if (gapSeconds <= maxInterpolationGapSeconds) {
                log.debug("Interpolating masked observation for segment {} using previous valid speed {:.1f} km/h (gap {}s)",
                        raw.getSegmentId(), prevObs.get().getSpeedKmph(), gapSeconds);
            }
        }

        // 5. Smooth speed with a trailing 5-sample median
        List<TrafficObservation> recentValid = observationRepository.findRecentValidObservations(raw.getSegmentId(), PageRequest.of(0, medianWindowSize));
        double smoothedSpeed;
        if (recentValid.isEmpty()) {
            smoothedSpeed = road.getTypicalSpeedKmph();
        } else {
            List<Double> speeds = new ArrayList<>();
            for (TrafficObservation obs : recentValid) {
                speeds.add(obs.getSpeedKmph());
            }
            Collections.sort(speeds);
            int mid = speeds.size() / 2;
            if (speeds.size() % 2 == 1) {
                smoothedSpeed = speeds.get(mid);
            } else {
                smoothedSpeed = (speeds.get(mid - 1) + speeds.get(mid)) / 2.0;
            }
        }

        // Minimum boundary protection for speed to prevent division by zero
        smoothedSpeed = Math.max(1.0, smoothedSpeed);

        // 6. Derive travel_min = distance_km / speed_smoothed * 60, ratio = speed_smoothed / typical_speed, and congestion level
        double distanceKm = road.getDistanceKm();
        double travelMin = (distanceKm / smoothedSpeed) * 60.0;
        double ratio = smoothedSpeed / typicalSpeed;
        CongestionLevel congestionLevel = classifyCongestion(ratio);

        // Update road segment record with smoothed metrics
        road.setCurrentSmoothedSpeedKmph(Math.round(smoothedSpeed * 10.0) / 10.0);
        road.setTravelTimeMinutes(Math.round(travelMin * 10.0) / 10.0);
        road.setCongestionLevel(congestionLevel);
        if (!shouldMask) {
            road.setLastObservationTime(raw.getTimestamp());
        }
        road.setUpdatedAt(Instant.now());
        roadSegmentRepository.save(road);

        return new PreprocessedResult(saved, true, false, shouldMask,
                shouldMask ? ("Masked: " + maskReason) : "Processed successfully",
                smoothedSpeed, travelMin, congestionLevel);
    }

    public CongestionLevel classifyCongestion(double speedRatio) {
        if (speedRatio >= 0.8) {
            return CongestionLevel.LOW;
        } else if (speedRatio >= 0.6) {
            return CongestionLevel.MEDIUM;
        } else if (speedRatio >= 0.35) {
            return CongestionLevel.HIGH;
        } else {
            return CongestionLevel.CRITICAL;
        }
    }
}
