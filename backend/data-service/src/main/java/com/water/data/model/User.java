package com.water.data.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity for authentication and storage quota management.
 * Mapped to users table in PostgreSQL database.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 200)
    private String organization;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    // Storage quota
    @Column(name = "storage_quota_mb", nullable = false)
    private Integer storageQuotaMb = 100;

    @Column(name = "storage_used_mb", nullable = false, precision = 10, scale = 2)
    private BigDecimal storageUsedMb = BigDecimal.ZERO;

    // Account status
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

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
     * User roles enum.
     */
    public enum UserRole {
        USER,
        ADMIN
    }

    /**
     * Checks if user has sufficient storage quota.
     *
     * @param requiredMb Storage required in MB
     * @return true if user has sufficient quota
     */
    public boolean hasQuota(double requiredMb) {
        double availableMb = storageQuotaMb - storageUsedMb.doubleValue();
        return availableMb >= requiredMb;
    }

    /**
     * Updates storage used by adding the specified amount.
     *
     * @param additionalMb Storage to add in MB
     * @throws IllegalStateException if update would exceed quota
     */
    public void addStorageUsed(double additionalMb) {
        BigDecimal newUsed = storageUsedMb.add(BigDecimal.valueOf(additionalMb));
        if (newUsed.doubleValue() > storageQuotaMb) {
            throw new IllegalStateException(
                String.format("Storage quota exceeded: %.2f MB / %d MB",
                    newUsed.doubleValue(), storageQuotaMb)
            );
        }
        this.storageUsedMb = newUsed;
    }

    /**
     * Gets available storage quota in MB.
     *
     * @return Available quota in MB
     */
    public double getAvailableQuotaMb() {
        return storageQuotaMb - storageUsedMb.doubleValue();
    }

    /**
     * Gets storage usage percentage.
     *
     * @return Usage percentage (0-100)
     */
    public double getUsagePercentage() {
        return (storageUsedMb.doubleValue() / storageQuotaMb) * 100.0;
    }
}
