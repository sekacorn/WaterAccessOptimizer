package com.water.data.repository;

import com.water.data.model.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {
    List<RiskAssessment> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<RiskAssessment> findByIsPublicTrue();
}
