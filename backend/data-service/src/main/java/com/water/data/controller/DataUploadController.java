package com.water.data.controller;

import com.water.data.dto.QuotaInfoResponse;
import com.water.data.dto.UploadListResponse;
import com.water.data.dto.UploadResponse;
import com.water.data.service.StorageQuotaService;
import com.water.data.service.UploadService;
import com.water.data.validator.SchemaValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for data upload endpoints.
 * Handles CSV uploads for hydro, community, and infrastructure data.
 */
@RestController
@RequestMapping("/api/v1/data")
public class DataUploadController {

    private final UploadService uploadService;
    private final StorageQuotaService storageQuotaService;

    public DataUploadController(
        UploadService uploadService,
        StorageQuotaService storageQuotaService
    ) {
        this.uploadService = uploadService;
        this.storageQuotaService = storageQuotaService;
    }

    /**
     * Upload hydrological data (water quality measurements).
     *
     * POST /api/v1/data/upload/hydro
     * Authorization: Bearer {jwt_token}
     * Content-Type: multipart/form-data
     *
     * @param file CSV file with hydro data
     * @param authentication Spring Security authentication (contains user ID from JWT)
     * @return UploadResponse with validation results
     */
    @PostMapping("/upload/hydro")
    public ResponseEntity<UploadResponse> uploadHydroData(
        @RequestParam("file") MultipartFile file,
        Authentication authentication
    ) {
        String userId = authentication.getName(); // User ID from JWT

        UploadResponse response = uploadService.uploadData(
            file,
            SchemaValidator.DataType.HYDRO,
            userId
        );

        // Return 200 OK for success/partial success, 400 Bad Request for failure
        HttpStatus status = response.getStatus() == UploadResponse.UploadStatus.FAILED
            ? HttpStatus.BAD_REQUEST
            : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Upload community data (population and water access).
     *
     * POST /api/v1/data/upload/community
     * Authorization: Bearer {jwt_token}
     * Content-Type: multipart/form-data
     *
     * @param file CSV file with community data
     * @param authentication Spring Security authentication
     * @return UploadResponse with validation results
     */
    @PostMapping("/upload/community")
    public ResponseEntity<UploadResponse> uploadCommunityData(
        @RequestParam("file") MultipartFile file,
        Authentication authentication
    ) {
        String userId = authentication.getName();

        UploadResponse response = uploadService.uploadData(
            file,
            SchemaValidator.DataType.COMMUNITY,
            userId
        );

        HttpStatus status = response.getStatus() == UploadResponse.UploadStatus.FAILED
            ? HttpStatus.BAD_REQUEST
            : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Upload infrastructure data (water facilities).
     *
     * POST /api/v1/data/upload/infrastructure
     * Authorization: Bearer {jwt_token}
     * Content-Type: multipart/form-data
     *
     * @param file CSV file with infrastructure data
     * @param authentication Spring Security authentication
     * @return UploadResponse with validation results
     */
    @PostMapping("/upload/infrastructure")
    public ResponseEntity<UploadResponse> uploadInfrastructureData(
        @RequestParam("file") MultipartFile file,
        Authentication authentication
    ) {
        String userId = authentication.getName();

        UploadResponse response = uploadService.uploadData(
            file,
            SchemaValidator.DataType.INFRASTRUCTURE,
            userId
        );

        HttpStatus status = response.getStatus() == UploadResponse.UploadStatus.FAILED
            ? HttpStatus.BAD_REQUEST
            : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * List user's uploads with pagination.
     *
     * GET /api/v1/data/uploads
     * Authorization: Bearer {jwt_token}
     * Query Parameters:
     *   - page: Page number (0-indexed, default 0)
     *   - pageSize: Items per page (default 20, max 100)
     *   - dataType: Optional filter by data type (hydro, community, infrastructure)
     *
     * @param authentication Spring Security authentication
     * @param page Page number (default 0)
     * @param pageSize Items per page (default 20)
     * @param dataType Optional data type filter
     * @return UploadListResponse with pagination
     */
    @GetMapping("/uploads")
    public ResponseEntity<UploadListResponse> listUploads(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        @RequestParam(required = false) String dataType
    ) {
        String userId = authentication.getName();

        // Validate page size (max 100)
        if (pageSize > 100) {
            pageSize = 100;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }

        UploadListResponse response = uploadService.listUploads(userId, page, pageSize, dataType);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete an upload (soft delete) and release storage quota.
     *
     * DELETE /api/v1/data/uploads/{uploadId}
     * Authorization: Bearer {jwt_token}
     *
     * @param uploadId Upload ID to delete
     * @param authentication Spring Security authentication
     * @return Success message
     */
    @DeleteMapping("/uploads/{uploadId}")
    public ResponseEntity<?> deleteUpload(
        @PathVariable String uploadId,
        Authentication authentication
    ) {
        String userId = authentication.getName();

        try {
            uploadService.deleteUpload(uploadId, userId);

            return ResponseEntity.ok()
                .body(new DeleteResponse(
                    "success",
                    "Upload deleted successfully. Storage quota has been released."
                ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new DeleteResponse("error", e.getMessage()));
        }
    }

    /**
     * Get user's storage quota information.
     *
     * GET /api/v1/data/quota
     * Authorization: Bearer {jwt_token}
     *
     * @param authentication Spring Security authentication
     * @return QuotaInfoResponse with usage and quota details
     */
    @GetMapping("/quota")
    public ResponseEntity<QuotaInfoResponse> getQuotaInfo(Authentication authentication) {
        String userId = authentication.getName();

        StorageQuotaService.StorageQuotaInfo quotaInfo = storageQuotaService.getQuotaInfo(userId);
        QuotaInfoResponse response = QuotaInfoResponse.fromServiceInfo(quotaInfo);

        return ResponseEntity.ok(response);
    }

    /**
     * Simple response for delete operation.
     */
    private record DeleteResponse(String status, String message) {}

    /**
     * Global exception handler for upload errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<UploadResponse> handleException(Exception e) {
        UploadResponse response = UploadResponse.failure(
            java.util.List.of(
                com.water.data.dto.ValidationErrorDto.fileError(
                    "Upload failed: " + e.getMessage(),
                    "Please try again or contact support if the issue persists."
                )
            )
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
