package com.smartambulance.repository;

import com.smartambulance.domain.TrafficObservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrafficObservationRepository extends JpaRepository<TrafficObservation, Long> {

    Optional<TrafficObservation> findTopBySegmentIdOrderByTimestampDesc(String segmentId);

    Optional<TrafficObservation> findTopBySegmentIdAndIsMaskedFalseOrderByTimestampDesc(String segmentId);

    @Query("SELECT o FROM TrafficObservation o WHERE o.segmentId = :segmentId AND o.isMasked = false ORDER BY o.timestamp DESC")
    List<TrafficObservation> findRecentValidObservations(@Param("segmentId") String segmentId, Pageable pageable);

    List<TrafficObservation> findBySegmentIdAndTimestampBetweenOrderByTimestampAsc(String segmentId, Instant start, Instant end);

    boolean existsBySegmentIdAndTimestamp(String segmentId, Instant timestamp);

    @Query("SELECT o FROM TrafficObservation o WHERE o.timestamp >= :since ORDER BY o.timestamp DESC")
    List<TrafficObservation> findObservationsSince(@Param("since") Instant since);
}
