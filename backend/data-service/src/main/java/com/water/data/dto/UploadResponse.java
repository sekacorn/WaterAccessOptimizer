package com.water.data.dto;

import com.fasterxml.jackson.annotation.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Response for file upload API endpoint.
 * Matches API contract defined in docs/API_CONTRACTS_MVP.md
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadResponse {

    private String uploadId;
    private UploadStatus status;
    private Integer recordsImported;
    private Integer recordsFailed;
    private UploadSummary summary;
    private List<ValidationErrorDto> errors;
    private List<ValidationErrorDto> warnings;
    private String fileChecksum;
    private Double storageUsedMb;
    private Long processingTimeMs;

    // Constructors
    public UploadResponse() {}

    // Success response
    public static UploadResponse success(
        String uploadId,
        int recordsImported,
        String fileChecksum,
        double storageUsedMb,
        long processingTimeMs
    ) {
        UploadResponse response = new UploadResponse();
        response.setUploadId(uploadId);
        response.setStatus(UploadStatus.SUCCESS);
        response.setRecordsImported(recordsImported);
        response.setRecordsFailed(0);
        response.setFileChecksum(fileChecksum);
        response.setStorageUsedMb(storageUsedMb);
        response.setProcessingTimeMs(processingTimeMs);
        return response;
    }

    // Partial success response
    public static UploadResponse partialSuccess(
        String uploadId,
        UploadSummary summary,
        List<ValidationErrorDto> errors,
        List<ValidationErrorDto> warnings,
        String fileChecksum,
        double storageUsedMb
    ) {
        UploadResponse response = new UploadResponse();
        response.setUploadId(uploadId);
        response.setStatus(UploadStatus.PARTIAL_SUCCESS);
        response.setSummary(summary);
        response.setErrors(errors);
        response.setWarnings(warnings);
        response.setFileChecksum(fileChecksum);
        response.setStorageUsedMb(storageUsedMb);
        return response;
    }

    // Failure response
    public static UploadResponse failure(
        List<ValidationErrorDto> errors
    ) {
        UploadResponse response = new UploadResponse();
        response.setStatus(UploadStatus.FAILED);
        response.setErrors(errors);
        return response;
    }

    // Getters and setters
    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }

    public Integer getRecordsImported() {
        return recordsImported;
    }

    public void setRecordsImported(Integer recordsImported) {
        this.recordsImported = recordsImported;
    }

    public Integer getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(Integer recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public UploadSummary getSummary() {
        return summary;
    }

    public void setSummary(UploadSummary summary) {
        this.summary = summary;
    }

    public List<ValidationErrorDto> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationErrorDto> errors) {
        this.errors = errors;
    }

    public List<ValidationErrorDto> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ValidationErrorDto> warnings) {
        this.warnings = warnings;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    public Double getStorageUsedMb() {
        return storageUsedMb;
    }

    public void setStorageUsedMb(Double storageUsedMb) {
        this.storageUsedMb = storageUsedMb;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    /**
     * Upload status enum.
     */
    public enum UploadStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED
    }

    /**
     * Upload summary for partial success.
     */
    public static class UploadSummary {
        private int totalRows;
        private int imported;
        private int failed;
        private int warnings;

        public UploadSummary() {}

        public UploadSummary(int totalRows, int imported, int failed, int warnings) {
            this.totalRows = totalRows;
            this.imported = imported;
            this.failed = failed;
            this.warnings = warnings;
        }

        public int getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        public int getImported() {
            return imported;
        }

        public void setImported(int imported) {
            this.imported = imported;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }

        public int getWarnings() {
            return warnings;
        }

        public void setWarnings(int warnings) {
            this.warnings = warnings;
        }
    }
}
