package com.smartambulance.repository;

import com.smartambulance.domain.SignalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignalEventRepository extends JpaRepository<SignalEvent, Long> {
    List<SignalEvent> findByJunctionIdOrderByCreatedAtDesc(String junctionId);
    List<SignalEvent> findAllByOrderByCreatedAtDesc();
}
