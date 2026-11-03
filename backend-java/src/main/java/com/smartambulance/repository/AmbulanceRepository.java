package com.smartambulance.repository;

import com.smartambulance.domain.Ambulance;
import com.smartambulance.domain.AmbulanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, String> {
    List<AmbulanceStatus> findByStatus(AmbulanceStatus status);
}
