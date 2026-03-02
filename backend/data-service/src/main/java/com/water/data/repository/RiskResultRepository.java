package com.water.data.repository;

import com.water.data.model.RiskAssessment;
import com.water.data.model.RiskResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskResultRepository extends JpaRepository<RiskResult, Long> {
    List<RiskResult> findByAssessmentIdOrderByRiskScoreDesc(UUID assessmentId);
    List<RiskResult> findByRiskLevel(RiskAssessment.RiskLevel riskLevel);
    Long countByAssessmentIdAndRiskLevel(UUID assessmentId, RiskAssessment.RiskLevel riskLevel);
}
