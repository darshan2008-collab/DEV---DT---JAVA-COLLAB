package com.smartambulance.repository;

import com.smartambulance.domain.EmergencyTrip;
import com.smartambulance.domain.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyTripRepository extends JpaRepository<EmergencyTrip, String> {
    List<EmergencyTrip> findByStatus(TripStatus status);
    List<EmergencyTrip> findByAmbulanceIdAndStatusIn(String ambulanceId, List<TripStatus> statuses);
    Optional<EmergencyTrip> findFirstByAmbulanceIdAndStatusIn(String ambulanceId, List<TripStatus> statuses);
    List<EmergencyTrip> findAllByOrderByCreatedAtDesc();
}
