package com.smartambulance.repository;

import com.smartambulance.domain.RoadSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadSegmentRepository extends JpaRepository<RoadSegment, String> {
    List<RoadSegment> findByIsClosedFalse();
    List<RoadSegment> findBySourceJunction(String sourceJunction);
}
