package com.water.data.repository;

import com.water.data.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 * Provides database operations for user management and quota checking.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address.
     *
     * @param email Email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user with email exists.
     *
     * @param email Email address
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Find active users by role.
     *
     * @param role User role
     * @return List of active users with specified role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    java.util.List<User> findActiveUsersByRole(@Param("role") User.UserRole role);

    /**
     * Get user's current storage usage.
     *
     * @param userId User ID
     * @return Storage used in MB, or empty if user not found
     */
    @Query("SELECT u.storageUsedMb FROM User u WHERE u.id = :userId")
    Optional<java.math.BigDecimal> getStorageUsed(@Param("userId") UUID userId);

    /**
     * Get user's storage quota.
     *
     * @param userId User ID
     * @return Storage quota in MB, or empty if user not found
     */
    @Query("SELECT u.storageQuotaMb FROM User u WHERE u.id = :userId")
    Optional<Integer> getStorageQuota(@Param("userId") UUID userId);
}
