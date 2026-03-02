package com.water.visualizer.repository;

import com.water.visualizer.model.VisualizationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface VisualizationRepository extends JpaRepository<VisualizationData, UUID> {
    List<VisualizationData> findByUserId(UUID userId);
    List<VisualizationData> findByVisualizationType(String visualizationType);
}
