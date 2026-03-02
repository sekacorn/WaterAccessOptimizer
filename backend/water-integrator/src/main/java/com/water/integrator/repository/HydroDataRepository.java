package com.water.integrator.repository;

import com.water.integrator.model.HydroData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HydroDataRepository extends JpaRepository<HydroData, UUID> {
    List<HydroData> findByUserId(UUID userId);
    List<HydroData> findByDataType(String dataType);
}
