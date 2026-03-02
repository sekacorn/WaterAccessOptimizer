package com.water.visualizer.controller;

import com.water.visualizer.model.VisualizationData;
import com.water.visualizer.service.VisualizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/visualizer")
@CrossOrigin(origins = "*")
public class VisualizationController {
    private static final Logger logger = LoggerFactory.getLogger(VisualizationController.class);

    @Autowired
    private VisualizationService visualizationService;

    @PostMapping("/create")
    public ResponseEntity<VisualizationData> createVisualization(@RequestBody VisualizationData data) {
        logger.info("POST /api/visualizer/create - Creating visualization");
        try {
            VisualizationData created = visualizationService.createVisualization(data);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error creating visualization: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VisualizationData>> getByUser(@PathVariable UUID userId) {
        logger.info("GET /api/visualizer/user/{} - Getting visualizations by user", userId);
        List<VisualizationData> visualizations = visualizationService.getVisualizationsByUser(userId);
        return ResponseEntity.ok(visualizations);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<VisualizationData>> getByType(@PathVariable String type) {
        logger.info("GET /api/visualizer/type/{} - Getting visualizations by type", type);
        List<VisualizationData> visualizations = visualizationService.getVisualizationsByType(type);
        return ResponseEntity.ok(visualizations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisualizationData> getById(@PathVariable UUID id) {
        logger.info("GET /api/visualizer/{} - Getting visualization by ID", id);
        VisualizationData data = visualizationService.getVisualizationById(id);
        if (data != null) {
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/ai-predictions")
    public ResponseEntity<Map<String, Object>> getAIPredictions(@RequestBody Map<String, Object> data) {
        logger.info("POST /api/visualizer/ai-predictions - Getting AI predictions");
        try {
            Map<String, Object> predictions = visualizationService.getAIPredictions(data);
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            logger.error("Error getting AI predictions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisualization(@PathVariable UUID id) {
        logger.info("DELETE /api/visualizer/{} - Deleting visualization", id);
        visualizationService.deleteVisualization(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "water-visualizer"));
    }
}
