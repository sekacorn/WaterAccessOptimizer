package com.water.llm.service;

import com.water.llm.model.QueryRequest;
import com.water.llm.model.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {
    private static final Logger logger = LoggerFactory.getLogger(LlmService.class);

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.model}")
    private String model;

    private final WebClient webClient;
    private final Map<String, String> mbtiProfiles;

    public LlmService(WebClient.Builder webClientBuilder,
                     @Value("#{${mbti.profiles}}") Map<String, String> mbtiProfiles) {
        this.webClient = webClientBuilder.build();
        this.mbtiProfiles = mbtiProfiles;
    }

    public QueryResponse processQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Processing query for MBTI type: {}", request.getMbtiType());

        String personalizedPrompt = buildPersonalizedPrompt(request);
        String llmResponse = callLlmApi(personalizedPrompt);

        long processingTime = System.currentTimeMillis() - startTime;

        return new QueryResponse(
            llmResponse,
            request.getMbtiType(),
            getMbtiTone(request.getMbtiType()),
            processingTime
        );
    }

    private String buildPersonalizedPrompt(QueryRequest request) {
        String tone = getMbtiTone(request.getMbtiType());

        String systemPrompt = String.format(
            "You are a water management assistant helping with the query. " +
            "Adapt your response style to be %s as this matches the user's MBTI type (%s). " +
            "Focus on water access, management, and optimization.",
            tone, request.getMbtiType()
        );

        return systemPrompt + "\n\nUser Query: " + request.getQuery();
    }

    private String getMbtiTone(String mbtiType) {
        return mbtiProfiles.getOrDefault(mbtiType, "helpful,professional,informative");
    }

    private String callLlmApi(String prompt) {
        try {
            if ("your-api-key".equals(apiKey)) {
                // Fallback response when no API key is configured
                return generateMockResponse(prompt);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 500);

            Mono<Map> response = webClient.post()
                    .uri(apiUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class);

            Map<String, Object> result = response.block();
            if (result != null && result.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            return "I apologize, but I encountered an issue processing your request. Please try again.";

        } catch (Exception e) {
            logger.error("Error calling LLM API: {}", e.getMessage());
            return generateMockResponse(prompt);
        }
    }

    private String generateMockResponse(String prompt) {
        // Mock response for demo/testing when LLM API is unavailable
        if (prompt.toLowerCase().contains("water")) {
            return "Based on your water management query, I recommend: " +
                   "1. Analyze current water usage patterns " +
                   "2. Identify areas with limited access " +
                   "3. Implement data-driven distribution strategies " +
                   "4. Monitor water quality regularly. " +
                   "For more specific guidance, please provide additional context about your region.";
        }
        return "I'm here to help with water access optimization. " +
               "Please ask me about water management, distribution, quality, or infrastructure planning.";
    }
}
