package com.water.auth.repository;

import com.water.auth.model.SsoAuthLog;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SSO Authentication Log Repository
 *
 * Manages persistence of SSO authentication logs
 */
@Repository
public class SsoAuthLogRepository {

    // In-memory storage for demo (use database in production)
    private final Map<UUID, SsoAuthLog> store = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> enterpriseIndex = new ConcurrentHashMap<>();

    public void save(SsoAuthLog log) {
        store.put(log.getId(), log);

        // Index by enterprise
        if (log.getEnterpriseId() != null) {
            enterpriseIndex.computeIfAbsent(log.getEnterpriseId(), k -> new ArrayList<>())
                          .add(log.getId());
        }
    }

    public Optional<SsoAuthLog> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<SsoAuthLog> findByEnterpriseId(UUID enterpriseId, int page, int size) {
        List<UUID> logIds = enterpriseIndex.getOrDefault(enterpriseId, new ArrayList<>());

        return logIds.stream()
            .skip((long) page * size)
            .limit(size)
            .map(store::get)
            .filter(Objects::nonNull)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
    }
}
