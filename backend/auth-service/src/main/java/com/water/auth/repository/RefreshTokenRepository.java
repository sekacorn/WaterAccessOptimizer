package com.water.auth.repository;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RefreshTokenRepository {

    private final Map<String, RefreshTokenRecord> store = new ConcurrentHashMap<>();

    public void save(RefreshTokenRecord token) {
        store.put(token.tokenId(), token);
    }

    public Optional<RefreshTokenRecord> findActiveByTokenId(String tokenId) {
        RefreshTokenRecord token = store.get(tokenId);
        if (token == null || token.revokedAt() != null || token.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    public void revoke(String tokenId) {
        RefreshTokenRecord token = store.get(tokenId);
        if (token != null) {
            store.put(tokenId, token.withRevokedAt(Instant.now()));
        }
    }

    public record RefreshTokenRecord(
        String tokenId,
        UUID userId,
        Instant expiresAt,
        Instant revokedAt,
        String ipAddress,
        String userAgent
    ) {
        public RefreshTokenRecord withRevokedAt(Instant value) {
            return new RefreshTokenRecord(tokenId, userId, expiresAt, value, ipAddress, userAgent);
        }
    }
}
