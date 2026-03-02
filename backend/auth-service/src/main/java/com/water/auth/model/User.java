package com.water.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User entity representing system users (MVP v0.1.0).
 *
 * MVP Roles:
 * - USER: Regular user with data upload and analysis permissions
 * - ADMIN: Can manage users and view system statistics
 *
 * Future Roles (V2):
 * - MODERATOR: Can moderate content and resolve reports
 * - ENTERPRISE_ADMIN: Can manage enterprise and its users
 * - SUPER_ADMIN: Full system access
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 255)
    private String organization;

    @Column(length = 50, nullable = false)
    private String role = "USER";

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Account lockout fields (MVP security requirement)
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // Storage quota (MVP requirement)
    @Column(name = "storage_quota_mb")
    private Integer storageQuotaMb = 100;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "login_count")
    private Integer loginCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String roleName) {
        return this.role != null && this.role.equals(roleName);
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String... roles) {
        for (String r : roles) {
            if (hasRole(r)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if account is currently locked
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Increment failed login attempts and lock account if threshold reached
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            // Lock account for 30 minutes
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }

    /**
     * Reset failed login attempts on successful login
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
}
