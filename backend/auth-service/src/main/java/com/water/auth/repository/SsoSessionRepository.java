package com.water.auth.repository;

import com.water.auth.model.SsoSession;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSO Session Repository
 *
 * Manages persistence of active SSO sessions
 */
@Repository
public class SsoSessionRepository {

    // In-memory storage for demo (use Redis in production for distributed sessions)
    private final Map<UUID, SsoSession> store = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> userIndex = new ConcurrentHashMap<>();

    public void save(SsoSession session) {
        store.put(session.getId(), session);
        userIndex.put(session.getUserId(), session.getId());
    }

    public Optional<SsoSession> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<SsoSession> findByUserId(UUID userId) {
        UUID sessionId = userIndex.get(userId);
        if (sessionId != null) {
            return Optional.ofNullable(store.get(sessionId));
        }
        return Optional.empty();
    }

    public void deleteByUserId(UUID userId) {
        UUID sessionId = userIndex.remove(userId);
        if (sessionId != null) {
            store.remove(sessionId);
        }
    }

    public void delete(UUID id) {
        SsoSession session = store.remove(id);
        if (session != null) {
            userIndex.remove(session.getUserId());
        }
    }
}
