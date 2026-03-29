package com.water.auth.repository;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PasswordResetTokenRepository {

    private final Map<String, PasswordResetTokenRecord> store = new ConcurrentHashMap<>();

    public void save(PasswordResetTokenRecord token) {
        store.put(token.token(), token);
    }

    public Optional<PasswordResetTokenRecord> findUsableToken(String token) {
        PasswordResetTokenRecord record = store.get(token);
        if (record == null || record.used() || record.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(record);
    }

    public void markUsed(String token) {
        PasswordResetTokenRecord record = store.get(token);
        if (record != null) {
            store.put(token, record.withUsed(true));
        }
    }

    public record PasswordResetTokenRecord(
        String token,
        UUID userId,
        Instant expiresAt,
        boolean used
    ) {
        public PasswordResetTokenRecord withUsed(boolean value) {
            return new PasswordResetTokenRecord(token, userId, expiresAt, value);
        }
    }
}
