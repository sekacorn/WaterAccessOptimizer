package com.water.integrator.repository;

import com.water.integrator.model.CommunityData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunityDataRepository extends JpaRepository<CommunityData, UUID> {
    List<CommunityData> findByUserId(UUID userId);
    List<CommunityData> findByWaterAccessLevel(String waterAccessLevel);
}
