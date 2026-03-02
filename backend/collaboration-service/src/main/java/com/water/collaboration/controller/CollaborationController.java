package com.water.collaboration.controller;

import com.water.collaboration.model.CollaborationSession;
import com.water.collaboration.service.CollaborationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/collaboration")
@CrossOrigin(origins = "*")
public class CollaborationController {
    private static final Logger logger = LoggerFactory.getLogger(CollaborationController.class);

    @Autowired
    private CollaborationService collaborationService;

    @PostMapping("/sessions")
    public ResponseEntity<CollaborationSession> createSession(@RequestBody CollaborationSession session) {
        logger.info("POST /api/collaboration/sessions - Creating session");
        try {
            CollaborationSession created = collaborationService.createSession(session);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error creating session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<CollaborationSession> getSession(@PathVariable UUID id) {
        logger.info("GET /api/collaboration/sessions/{}", id);
        CollaborationSession session = collaborationService.getSessionById(id);
        if (session != null) {
            return ResponseEntity.ok(session);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/sessions/owner/{ownerId}")
    public ResponseEntity<List<CollaborationSession>> getSessionsByOwner(@PathVariable UUID ownerId) {
        logger.info("GET /api/collaboration/sessions/owner/{}", ownerId);
        List<CollaborationSession> sessions = collaborationService.getSessionsByOwner(ownerId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<List<CollaborationSession>> getActiveSessions() {
        logger.info("GET /api/collaboration/sessions/active");
        List<CollaborationSession> sessions = collaborationService.getActiveSessions();
        return ResponseEntity.ok(sessions);
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<CollaborationSession> updateSession(
            @PathVariable UUID id,
            @RequestBody CollaborationSession session) {
        logger.info("PUT /api/collaboration/sessions/{}", id);
        CollaborationSession updated = collaborationService.updateSession(id, session);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/sessions/{id}/close")
    public ResponseEntity<Void> closeSession(@PathVariable UUID id) {
        logger.info("POST /api/collaboration/sessions/{}/close", id);
        collaborationService.closeSession(id);
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/session/{sessionId}")
    @SendTo("/topic/session/{sessionId}")
    public Map<String, Object> handleSessionMessage(
            @DestinationVariable UUID sessionId,
            Map<String, Object> message) {
        logger.info("WebSocket message to session: {}", sessionId);
        return message;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "collaboration-service"));
    }
}
