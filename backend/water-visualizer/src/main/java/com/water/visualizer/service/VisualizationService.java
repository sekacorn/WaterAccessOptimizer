package com.water.visualizer.service;

import com.water.visualizer.model.VisualizationData;
import com.water.visualizer.repository.VisualizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VisualizationService {
    private static final Logger logger = LoggerFactory.getLogger(VisualizationService.class);

    @Autowired
    private VisualizationRepository visualizationRepository;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    private final WebClient webClient;

    public VisualizationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public VisualizationData createVisualization(VisualizationData data) {
        logger.info("Creating visualization of type: {}", data.getVisualizationType());
        return visualizationRepository.save(data);
    }

    public List<VisualizationData> getVisualizationsByUser(UUID userId) {
        return visualizationRepository.findByUserId(userId);
    }

    public List<VisualizationData> getVisualizationsByType(String type) {
        return visualizationRepository.findByVisualizationType(type);
    }

    public VisualizationData getVisualizationById(UUID id) {
        return visualizationRepository.findById(id).orElse(null);
    }

    public Map<String, Object> getAIPredictions(Map<String, Object> data) {
        logger.info("Requesting AI predictions from: {}", aiServiceUrl);
        try {
            Mono<Map> response = webClient.post()
                    .uri(aiServiceUrl + "/predict")
                    .bodyValue(data)
                    .retrieve()
                    .bodyToMono(Map.class);

            return response.block();
        } catch (Exception e) {
            logger.error("Error calling AI service: {}", e.getMessage());
            return Map.of("error", "AI service unavailable", "message", e.getMessage());
        }
    }

    public void deleteVisualization(UUID id) {
        visualizationRepository.deleteById(id);
    }
}
