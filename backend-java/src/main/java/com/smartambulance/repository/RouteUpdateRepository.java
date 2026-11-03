package com.smartambulance.repository;

import com.smartambulance.domain.RouteUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteUpdateRepository extends JpaRepository<RouteUpdate, Long> {
    List<RouteUpdate> findByTripIdOrderByTimestampDesc(String tripId);
    List<RouteUpdate> findAllByOrderByTimestampDesc();
}
