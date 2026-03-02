package com.water.llm.controller;

import com.water.llm.model.QueryRequest;
import com.water.llm.model.QueryResponse;
import com.water.llm.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/llm")
@CrossOrigin(origins = "*")
public class LlmController {
    private static final Logger logger = LoggerFactory.getLogger(LlmController.class);

    @Autowired
    private LlmService llmService;

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> processQuery(@RequestBody QueryRequest request) {
        logger.info("POST /api/llm/query - Processing query");
        try {
            QueryResponse response = llmService.processQuery(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing query: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new QueryResponse(
                        "Error processing your request: " + e.getMessage(),
                        request.getMbtiType(),
                        "error",
                        0L
                    ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "llm-service"));
    }
}
