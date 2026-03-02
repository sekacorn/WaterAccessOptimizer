package com.water.collaboration.service;

import com.water.collaboration.model.CollaborationSession;
import com.water.collaboration.repository.CollaborationSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CollaborationService {
    private static final Logger logger = LoggerFactory.getLogger(CollaborationService.class);

    @Autowired
    private CollaborationSessionRepository sessionRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public CollaborationSession createSession(CollaborationSession session) {
        logger.info("Creating collaboration session: {}", session.getSessionName());
        return sessionRepository.save(session);
    }

    public CollaborationSession getSessionById(UUID id) {
        return sessionRepository.findById(id).orElse(null);
    }

    public List<CollaborationSession> getSessionsByOwner(UUID ownerId) {
        return sessionRepository.findByOwnerId(ownerId);
    }

    public List<CollaborationSession> getActiveSessions() {
        return sessionRepository.findByIsActive(true);
    }

    public CollaborationSession updateSession(UUID id, CollaborationSession updatedSession) {
        CollaborationSession session = sessionRepository.findById(id).orElse(null);
        if (session != null) {
            if (updatedSession.getSharedData() != null) {
                session.setSharedData(updatedSession.getSharedData());
            }
            if (updatedSession.getParticipants() != null) {
                session.setParticipants(updatedSession.getParticipants());
            }
            return sessionRepository.save(session);
        }
        return null;
    }

    public void broadcastToSession(UUID sessionId, Map<String, Object> message) {
        logger.info("Broadcasting message to session: {}", sessionId);
        messagingTemplate.convertAndSend("/topic/session/" + sessionId, message);
    }

    public void closeSession(UUID id) {
        CollaborationSession session = sessionRepository.findById(id).orElse(null);
        if (session != null) {
            session.setIsActive(false);
            sessionRepository.save(session);
            logger.info("Closed collaboration session: {}", id);
        }
    }
}
