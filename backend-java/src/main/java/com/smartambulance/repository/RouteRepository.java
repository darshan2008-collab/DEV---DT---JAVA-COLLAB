package com.smartambulance.repository;

import com.smartambulance.domain.RouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<RouteEntity, String> {
    List<RouteEntity> findByTripId(String tripId);
}
