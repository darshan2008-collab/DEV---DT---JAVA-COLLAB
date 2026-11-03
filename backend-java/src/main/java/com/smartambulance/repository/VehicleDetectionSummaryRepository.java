package com.smartambulance.repository;

import com.smartambulance.domain.VehicleDetectionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleDetectionSummaryRepository extends JpaRepository<VehicleDetectionSummary, Long> {
    List<VehicleDetectionSummary> findByCameraIdOrderByTimestampDesc(String cameraId);
    List<VehicleDetectionSummary> findAllByOrderByTimestampDesc();
}
