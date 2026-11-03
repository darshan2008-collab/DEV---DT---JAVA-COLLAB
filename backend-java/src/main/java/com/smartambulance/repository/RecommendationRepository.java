package com.smartambulance.repository;

import com.smartambulance.domain.Recommendation;
import com.smartambulance.domain.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, String> {
    List<Recommendation> findByTripIdOrderByCreatedAtDesc(String tripId);
    Optional<Recommendation> findFirstByTripIdAndStatusOrderByCreatedAtDesc(String tripId, RecommendationStatus status);
}
