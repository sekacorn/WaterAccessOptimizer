package com.water.auth.repository;

import com.water.auth.model.User;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User Repository (MVP v0.1.0)
 *
 * Manages persistence of user accounts
 * NOTE: In-memory storage for demo (use JPA/JDBC in production)
 */
@Repository
public class UserRepository {

    // In-memory storage for demo (use JPA/JDBC in production)
    private final Map<UUID, User> store = new ConcurrentHashMap<>();
    private final Map<String, UUID> emailIndex = new ConcurrentHashMap<>();

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(java.time.LocalDateTime.now());
        }
        user.setUpdatedAt(java.time.LocalDateTime.now());
        store.put(user.getId(), user);
        emailIndex.put(user.getEmail(), user.getId());
        return user;
    }

    public User update(User user) {
        return save(user);
    }

    public boolean existsByEmail(String email) {
        return emailIndex.containsKey(email);
    }

    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<User> findByEmail(String email) {
        UUID userId = emailIndex.get(email);
        if (userId != null) {
            return Optional.ofNullable(store.get(userId));
        }
        return Optional.empty();
    }

    public void delete(UUID id) {
        User user = store.remove(id);
        if (user != null) {
            emailIndex.remove(user.getEmail());
        }
    }
}
