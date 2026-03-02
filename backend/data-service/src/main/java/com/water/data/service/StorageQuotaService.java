package com.water.data.service;

import com.water.data.model.User;
import com.water.data.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing user storage quotas.
 * Checks available quota before upload and updates usage after successful upload.
 */
@Service
public class StorageQuotaService {

    private final UserRepository userRepository;

    // Default storage quota (MB)
    private static final double DEFAULT_QUOTA_MB = 100.0;  // 100 MB for free tier
    private static final double ADMIN_QUOTA_MB = 1000.0;   // 1 GB for admins

    public StorageQuotaService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Checks if user has sufficient storage quota for upload.
     *
     * @param userId User ID
     * @param fileSizeMb File size in MB
     * @return true if user has quota, false otherwise
     */
    public boolean checkQuota(String userId, double fileSizeMb) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        double availableQuota = user.getAvailableQuotaMb();

        System.out.println(String.format(
            "Checking storage quota for user %s: file size %.2f MB, available %.2f MB",
            userId,
            fileSizeMb,
            availableQuota
        ));

        return availableQuota >= fileSizeMb;
    }

    /**
     * Updates user's storage usage after successful upload.
     *
     * @param userId User ID
     * @param fileSizeMb File size in MB to add to usage
     */
    @Transactional
    public void updateQuota(String userId, double fileSizeMb) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Use User entity method to add storage (includes validation)
        user.addStorageUsed(fileSizeMb);
        userRepository.save(user);

        System.out.println(String.format(
            "Updated storage quota for user %s: +%.2f MB (now %.2f MB / %d MB)",
            userId,
            fileSizeMb,
            user.getStorageUsedMb().doubleValue(),
            user.getStorageQuotaMb()
        ));
    }

    /**
     * Releases storage quota after upload deletion.
     *
     * @param userId User ID
     * @param fileSizeMb File size in MB to release
     */
    @Transactional
    public void releaseQuota(String userId, double fileSizeMb) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Subtract storage used (release quota)
        java.math.BigDecimal currentUsed = user.getStorageUsedMb();
        java.math.BigDecimal toRelease = java.math.BigDecimal.valueOf(fileSizeMb);
        java.math.BigDecimal newUsed = currentUsed.subtract(toRelease);

        // Ensure it doesn't go below zero
        if (newUsed.compareTo(java.math.BigDecimal.ZERO) < 0) {
            newUsed = java.math.BigDecimal.ZERO;
        }

        user.setStorageUsedMb(newUsed);
        userRepository.save(user);

        System.out.println(String.format(
            "Released storage quota for user %s: -%.2f MB (now %.2f MB / %d MB)",
            userId,
            fileSizeMb,
            user.getStorageUsedMb().doubleValue(),
            user.getStorageQuotaMb()
        ));
    }

    /**
     * Gets user's current storage usage and quota.
     *
     * @param userId User ID
     * @return StorageQuotaInfo with usage and quota details
     */
    public StorageQuotaInfo getQuotaInfo(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return new StorageQuotaInfo(
            user.getStorageUsedMb().doubleValue(),
            user.getStorageQuotaMb(),
            user.getAvailableQuotaMb()
        );
    }

    /**
     * Storage quota information for a user.
     */
    public static class StorageQuotaInfo {
        private final double usedMb;
        private final double quotaMb;
        private final double availableMb;

        public StorageQuotaInfo(double usedMb, double quotaMb, double availableMb) {
            this.usedMb = usedMb;
            this.quotaMb = quotaMb;
            this.availableMb = availableMb;
        }

        public double getUsedMb() {
            return usedMb;
        }

        public double getQuotaMb() {
            return quotaMb;
        }

        public double getAvailableMb() {
            return availableMb;
        }

        public double getUsagePercentage() {
            return (usedMb / quotaMb) * 100.0;
        }
    }
}
