package com.water.integrator.repository;

import com.water.integrator.model.InfrastructureData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureDataRepository extends JpaRepository<InfrastructureData, UUID> {
    List<InfrastructureData> findByUserId(UUID userId);
    List<InfrastructureData> findByFacilityType(String facilityType);
}
