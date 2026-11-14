package com.smartambulance.repository;

import com.smartambulance.domain.CameraStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CameraStreamRepository extends JpaRepository<CameraStream, Long> {

    Optional<CameraStream> findByStreamId(String streamId);

    List<CameraStream> findByEnabled(Boolean enabled);

    List<CameraStream> findBySegmentId(String segmentId);

    boolean existsByStreamId(String streamId);
}
