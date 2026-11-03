package com.smartambulance;

import com.smartambulance.domain.CongestionLevel;
import com.smartambulance.domain.DataSource;
import com.smartambulance.domain.RoadSegment;
import com.smartambulance.domain.TrafficObservation;
import com.smartambulance.repository.RoadSegmentRepository;
import com.smartambulance.repository.TrafficObservationRepository;
import com.smartambulance.service.ObservationPreprocessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
public class PreprocessingServiceTest {

    @Autowired
    private ObservationPreprocessingService preprocessingService;

    @Autowired
    private TrafficObservationRepository observationRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    private Instant testTime;

    @BeforeEach
    public void setup() {
        testTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    }

    @Test
    @DisplayName("Preprocessing: Drop duplicate (timestamp, segment_id) records")
    public void testDropDuplicateObservations() {
        TrafficObservation obs1 = new TrafficObservation("B1", testTime, 10, 20.0, 40.0, 10.0, 0.90, DataSource.SIMULATION);
        ObservationPreprocessingService.PreprocessedResult res1 = preprocessingService.processIncomingObservation(obs1);
        assertTrue(res1.isAccepted());
        assertFalse(res1.isDuplicate());

        // Identical segment_id and timestamp
        TrafficObservation obs2 = new TrafficObservation("B1", testTime, 12, 24.0, 38.0, 15.0, 0.90, DataSource.SIMULATION);
        ObservationPreprocessingService.PreprocessedResult res2 = preprocessingService.processIncomingObservation(obs2);

        assertFalse(res2.isAccepted(), "Duplicate observation should not be accepted");
        assertTrue(res2.isDuplicate(), "Duplicate flag should be true");
    }

    @Test
    @DisplayName("Preprocessing: Mask low confidence frames (< 0.5) so poor video is never read as clear road")
    public void testMaskLowConfidence() {
        TrafficObservation lowConf = new TrafficObservation("A1", testTime.plusSeconds(1), 5, 10.0, 30.0, 5.0, 0.35, DataSource.CAMERA_LIVE);
        ObservationPreprocessingService.PreprocessedResult res = preprocessingService.processIncomingObservation(lowConf);

        assertTrue(res.isAccepted());
        assertTrue(res.isMasked(), "Observation with confidence < 0.5 must be masked");
        assertTrue(res.getMessage().contains("Low confidence"));
    }

    @Test
    @DisplayName("Preprocessing: Mask outlier speeds (> 1.5x reference speed or negative)")
    public void testMaskOutlierSpeeds() {
        // Road A1 reference speed is 30 km/h. 1.5 * 30 = 45 km/h. Speed 75 km/h is an outlier.
        TrafficObservation outlier = new TrafficObservation("A1", testTime.plusSeconds(2), 2, 4.0, 75.0, 0.0, 0.92, DataSource.SIMULATION);
        ObservationPreprocessingService.PreprocessedResult resHigh = preprocessingService.processIncomingObservation(outlier);

        assertTrue(resHigh.isMasked(), "Speed exceeding 1.5x typical speed must be masked");

        TrafficObservation negative = new TrafficObservation("A1", testTime.plusSeconds(3), 2, 4.0, -10.0, 0.0, 0.92, DataSource.SIMULATION);
        ObservationPreprocessingService.PreprocessedResult resNeg = preprocessingService.processIncomingObservation(negative);

        assertTrue(resNeg.isMasked(), "Negative speed must be masked");
    }

    @Test
    @DisplayName("Preprocessing: Trailing 5-sample median speed smoothing")
    public void testTrailingMedianSmoothing() {
        // Send 5 observations with varying speeds for segment B2 (typical 40 km/h)
        double[] speeds = { 35.0, 42.0, 20.0, 38.0, 40.0 };
        // Sorted: 20, 35, 38, 40, 42 -> Median = 38.0
        ObservationPreprocessingService.PreprocessedResult lastRes = null;
        for (int i = 0; i < speeds.length; i++) {
            TrafficObservation obs = new TrafficObservation("B2", testTime.plusSeconds(10 + i), 10, 20.0, speeds[i], 10.0, 0.90, DataSource.SIMULATION);
            lastRes = preprocessingService.processIncomingObservation(obs);
        }

        assertNotNull(lastRes);
        assertEquals(38.0, lastRes.getSmoothedSpeedKmph(), 0.5, "Smoothed speed must match trailing 5-sample median");
    }

    @Test
    @DisplayName("Preprocessing: Congestion classification from speed ratio")
    public void testCongestionClassification() {
        assertEquals(CongestionLevel.LOW, preprocessingService.classifyCongestion(0.9));
        assertEquals(CongestionLevel.MEDIUM, preprocessingService.classifyCongestion(0.7));
        assertEquals(CongestionLevel.HIGH, preprocessingService.classifyCongestion(0.45));
        assertEquals(CongestionLevel.CRITICAL, preprocessingService.classifyCongestion(0.2));
    }
}
