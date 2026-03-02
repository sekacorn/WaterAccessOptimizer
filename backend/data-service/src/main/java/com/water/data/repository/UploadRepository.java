package com.water.data.repository;

import com.water.data.model.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Upload entity.
 * Provides database operations for upload metadata and provenance tracking.
 */
@Repository
public interface UploadRepository extends JpaRepository<Upload, UUID> {

    /**
     * Find all uploads by user ID (excluding soft-deleted).
     *
     * @param userId User ID
     * @return List of user's uploads
     */
    @Query("SELECT u FROM Upload u WHERE u.userId = :userId AND u.deletedAt IS NULL ORDER BY u.uploadedAt DESC")
    List<Upload> findByUserId(@Param("userId") UUID userId);

    /**
     * Find uploads by user ID and data type (excluding soft-deleted).
     *
     * @param userId User ID
     * @param dataType Data type
     * @return List of uploads matching criteria
     */
    @Query("SELECT u FROM Upload u WHERE u.userId = :userId AND u.dataType = :dataType AND u.deletedAt IS NULL ORDER BY u.uploadedAt DESC")
    List<Upload> findByUserIdAndDataType(@Param("userId") UUID userId, @Param("dataType") Upload.DataType dataType);

    /**
     * Find uploads by status (excluding soft-deleted).
     *
     * @param status Upload status
     * @return List of uploads with specified status
     */
    @Query("SELECT u FROM Upload u WHERE u.status = :status AND u.deletedAt IS NULL ORDER BY u.uploadedAt DESC")
    List<Upload> findByStatus(@Param("status") Upload.UploadStatus status);

    /**
     * Find upload by checksum to detect duplicates.
     *
     * @param fileChecksum SHA-256 file checksum
     * @return Optional containing upload if duplicate found
     */
    @Query("SELECT u FROM Upload u WHERE u.fileChecksum = :fileChecksum AND u.deletedAt IS NULL ORDER BY u.uploadedAt DESC LIMIT 1")
    Optional<Upload> findByFileChecksum(@Param("fileChecksum") String fileChecksum);

    /**
     * Find soft-deleted uploads older than specified date (for cleanup).
     *
     * @param deleteDate Deletion date threshold
     * @return List of old soft-deleted uploads
     */
    @Query("SELECT u FROM Upload u WHERE u.deletedAt IS NOT NULL AND u.deletedAt < :deleteDate")
    List<Upload> findOldDeletedUploads(@Param("deleteDate") LocalDateTime deleteDate);

    /**
     * Get total storage used by user (sum of all upload file sizes).
     *
     * @param userId User ID
     * @return Total storage in bytes
     */
    @Query("SELECT COALESCE(SUM(u.fileSizeBytes), 0) FROM Upload u WHERE u.userId = :userId AND u.deletedAt IS NULL")
    Long getTotalStorageUsedBytes(@Param("userId") UUID userId);

    /**
     * Count uploads by user and status.
     *
     * @param userId User ID
     * @param status Upload status
     * @return Count of uploads
     */
    @Query("SELECT COUNT(u) FROM Upload u WHERE u.userId = :userId AND u.status = :status AND u.deletedAt IS NULL")
    Long countByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") Upload.UploadStatus status);
}
