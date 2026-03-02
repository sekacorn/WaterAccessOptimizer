package com.water.data.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Upload entity for tracking file upload metadata and provenance.
 * Mapped to uploads table in PostgreSQL database.
 */
@Entity
@Table(name = "uploads")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Upload {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    // File metadata
    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "file_checksum", nullable = false, length = 64)
    private String fileChecksum;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType = "csv";

    @Column(name = "data_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DataType dataType;

    // Processing status
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private UploadStatus status = UploadStatus.PENDING;

    @Column(name = "records_imported")
    private Integer recordsImported;

    @Column(name = "records_failed")
    private Integer recordsFailed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Provenance timestamps
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    /**
     * Upload status enum.
     */
    public enum UploadStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    /**
     * Data type enum.
     */
    public enum DataType {
        HYDRO,
        COMMUNITY,
        INFRASTRUCTURE,
        GENERIC
    }

    /**
     * Marks upload as completed.
     *
     * @param recordsImported Number of records successfully imported
     * @param recordsFailed Number of records that failed validation
     */
    public void markCompleted(int recordsImported, int recordsFailed) {
        this.status = UploadStatus.COMPLETED;
        this.recordsImported = recordsImported;
        this.recordsFailed = recordsFailed;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Marks upload as failed.
     *
     * @param errorMessage Error message describing the failure
     */
    public void markFailed(String errorMessage) {
        this.status = UploadStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Soft deletes the upload.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Checks if upload is deleted.
     *
     * @return true if soft deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Gets file size in megabytes.
     *
     * @return File size in MB
     */
    public double getFileSizeMb() {
        return fileSizeBytes / (1024.0 * 1024.0);
    }
}
