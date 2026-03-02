package com.water.visualizer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "visualizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisualizationData {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "visualization_type")
    private String visualizationType; // hydro, community, infrastructure

    @Column(columnDefinition = "TEXT")
    private String threeDData; // JSON for Three.js

    @Column(name = "export_format")
    private String exportFormat; // png, svg, stl

    @Column(columnDefinition = "TEXT")
    private String metadata; // Additional visualization metadata

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
