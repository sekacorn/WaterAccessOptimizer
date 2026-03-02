package com.water.data.service;

import com.water.data.dto.UploadResponse;
import com.water.data.model.*;
import com.water.data.repository.*;
import com.water.data.validator.SchemaValidator;
import com.water.data.validator.ValidationPipeline;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling data uploads.
 * Coordinates validation, storage quota checking, and database insertion.
 */
@Service
public class UploadService {

    private final ValidationPipeline validationPipeline;
    private final StorageQuotaService storageQuotaService;
    private final UploadRepository uploadRepository;
    private final HydroDataRepository hydroDataRepository;
    private final CommunityDataRepository communityDataRepository;
    private final InfrastructureDataRepository infrastructureDataRepository;

    // GeometryFactory for creating PostGIS Point objects (SRID 4326 = WGS84)
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public UploadService(
        ValidationPipeline validationPipeline,
        StorageQuotaService storageQuotaService,
        UploadRepository uploadRepository,
        HydroDataRepository hydroDataRepository,
        CommunityDataRepository communityDataRepository,
        InfrastructureDataRepository infrastructureDataRepository
    ) {
        this.validationPipeline = validationPipeline;
        this.storageQuotaService = storageQuotaService;
        this.uploadRepository = uploadRepository;
        this.hydroDataRepository = hydroDataRepository;
        this.communityDataRepository = communityDataRepository;
        this.infrastructureDataRepository = infrastructureDataRepository;
    }

    /**
     * Uploads and validates data file.
     *
     * Processing flow:
     * 1. Check storage quota
     * 2. Calculate file checksum
     * 3. Run validation pipeline (5 stages)
     * 4. If valid data exists, save to database (transaction)
     * 5. Update storage quota
     * 6. Return response
     *
     * @param file Uploaded CSV file
     * @param dataType Type of data (hydro, community, infrastructure)
     * @param userId User ID from JWT authentication
     * @return UploadResponse with validation results and upload ID
     */
    @Transactional
    public UploadResponse uploadData(
        MultipartFile file,
        SchemaValidator.DataType dataType,
        String userId
    ) {
        long startTime = System.currentTimeMillis();

        // Step 1: Check storage quota
        double fileSizeMb = file.getSize() / (1024.0 * 1024.0);
        boolean hasQuota = storageQuotaService.checkQuota(userId, fileSizeMb);

        if (!hasQuota) {
            return UploadResponse.failure(
                java.util.List.of(
                    com.water.data.dto.ValidationErrorDto.fileError(
                        String.format(
                            "Storage quota exceeded. File size: %.2f MB. " +
                            "Please delete old uploads or contact admin to increase quota.",
                            fileSizeMb
                        ),
                        "Delete unused uploads via DELETE /api/v1/data/uploads/{uploadId}"
                    )
                )
            );
        }

        // Step 2: Calculate file checksum (SHA-256)
        String fileChecksum;
        try {
            fileChecksum = calculateChecksum(file);
        } catch (Exception e) {
            return UploadResponse.failure(
                java.util.List.of(
                    com.water.data.dto.ValidationErrorDto.fileError(
                        "Failed to calculate file checksum: " + e.getMessage(),
                        "Please try uploading again."
                    )
                )
            );
        }

        // Step 3: Run validation pipeline (all 5 stages)
        ValidationPipeline.ValidationResult validationResult =
            validationPipeline.validateCsv(file, dataType);

        // Step 4: Generate upload ID
        String uploadId = UUID.randomUUID().toString();

        // Step 5: If we have valid rows, save to database
        if (validationResult.getValidRows() > 0) {
            try {
                saveToDatabase(uploadId, userId, file, dataType, validationResult);

                // Step 6: Update storage quota
                storageQuotaService.updateQuota(userId, fileSizeMb);

            } catch (Exception e) {
                // Rollback will happen automatically due to @Transactional
                return UploadResponse.failure(
                    java.util.List.of(
                        com.water.data.dto.ValidationErrorDto.fileError(
                            "Database error: " + e.getMessage(),
                            "Please try again or contact support."
                        )
                    )
                );
            }
        }

        // Step 7: Calculate processing time
        long processingTime = System.currentTimeMillis() - startTime;

        // Step 8: Convert validation result to API response
        UploadResponse response = validationResult.toUploadResponse(
            uploadId,
            fileChecksum,
            fileSizeMb
        );
        response.setProcessingTimeMs(processingTime);

        return response;
    }

    /**
     * Saves validated data to database.
     * This method runs within a transaction - if any insert fails, all rollback.
     */
    private void saveToDatabase(
        String uploadId,
        String userId,
        MultipartFile file,
        SchemaValidator.DataType dataType,
        ValidationPipeline.ValidationResult validationResult
    ) throws Exception {
        // 1. Create upload record
        Upload upload = new Upload();
        upload.setId(UUID.fromString(uploadId));
        upload.setUserId(UUID.fromString(userId));
        upload.setFilename(file.getOriginalFilename());
        upload.setFileSizeBytes(file.getSize());
        upload.setFileChecksum(calculateChecksum(file));
        upload.setFileType("csv");
        upload.setDataType(convertDataType(dataType));
        upload.setStatus(Upload.UploadStatus.COMPLETED);
        upload.setRecordsImported(validationResult.getValidRows());
        upload.setRecordsFailed(validationResult.getFailedRows());
        upload.setUploadedAt(LocalDateTime.now());
        upload.setProcessedAt(LocalDateTime.now());

        uploadRepository.save(upload);

        // 2. Insert validated data rows
        switch (dataType) {
            case HYDRO -> {
                // Bulk insert hydro data
                List<HydroData> hydroRecords = convertToHydroData(
                    validationResult.getValidRowsData(),
                    uploadId,
                    userId
                );
                hydroDataRepository.saveAll(hydroRecords);
            }
            case COMMUNITY -> {
                // Bulk insert community data
                List<CommunityData> communityRecords = convertToCommunityData(
                    validationResult.getValidRowsData(),
                    uploadId,
                    userId
                );
                communityDataRepository.saveAll(communityRecords);
            }
            case INFRASTRUCTURE -> {
                // Bulk insert infrastructure data
                List<InfrastructureData> infraRecords = convertToInfrastructureData(
                    validationResult.getValidRowsData(),
                    uploadId,
                    userId
                );
                infrastructureDataRepository.saveAll(infraRecords);
            }
        }

        System.out.println(String.format(
            "Successfully inserted to database: uploadId=%s, dataType=%s, validRows=%d, failedRows=%d",
            uploadId,
            dataType,
            validationResult.getValidRows(),
            validationResult.getFailedRows()
        ));
    }

    /**
     * Converts validation data type to Upload data type enum.
     */
    private Upload.DataType convertDataType(SchemaValidator.DataType dataType) {
        return switch (dataType) {
            case HYDRO -> Upload.DataType.HYDRO;
            case COMMUNITY -> Upload.DataType.COMMUNITY;
            case INFRASTRUCTURE -> Upload.DataType.INFRASTRUCTURE;
        };
    }

    /**
     * Converts validated rows to HydroData entities.
     */
    private List<HydroData> convertToHydroData(
        List<Map<String, String>> validRows,
        String uploadId,
        String userId
    ) {
        List<HydroData> records = new ArrayList<>();
        UUID uploadUUID = UUID.fromString(uploadId);
        UUID userUUID = UUID.fromString(userId);

        for (Map<String, String> row : validRows) {
            HydroData hydro = new HydroData();
            hydro.setUploadId(uploadUUID);
            hydro.setUserId(userUUID);
            hydro.setSource(row.get("source"));
            hydro.setLocationName(row.get("location_name"));
            hydro.setCoordinates(createPoint(row.get("latitude"), row.get("longitude")));
            hydro.setDataType(row.get("data_type"));
            hydro.setParameterName(row.get("parameter_name"));
            hydro.setMeasurementValue(new BigDecimal(row.get("measurement_value")));
            hydro.setMeasurementUnit(row.get("measurement_unit"));
            hydro.setMeasurementDate(parseDateTime(row.get("measurement_date")));

            if (row.containsKey("depth_meters") && !row.get("depth_meters").isEmpty()) {
                hydro.setDepthMeters(new BigDecimal(row.get("depth_meters")));
            }
            if (row.containsKey("notes")) {
                hydro.setNotes(row.get("notes"));
            }

            records.add(hydro);
        }

        return records;
    }

    /**
     * Converts validated rows to CommunityData entities.
     */
    private List<CommunityData> convertToCommunityData(
        List<Map<String, String>> validRows,
        String uploadId,
        String userId
    ) {
        List<CommunityData> records = new ArrayList<>();
        UUID uploadUUID = UUID.fromString(uploadId);
        UUID userUUID = UUID.fromString(userId);

        for (Map<String, String> row : validRows) {
            CommunityData community = new CommunityData();
            community.setUploadId(uploadUUID);
            community.setUserId(userUUID);
            community.setCommunityName(row.get("community_name"));
            community.setCoordinates(createPoint(row.get("latitude"), row.get("longitude")));
            community.setPopulation(Integer.parseInt(row.get("population")));
            community.setSource(row.get("source"));

            if (row.containsKey("household_count") && !row.get("household_count").isEmpty()) {
                community.setHouseholdCount(Integer.parseInt(row.get("household_count")));
            }
            if (row.containsKey("water_access_level") && !row.get("water_access_level").isEmpty()) {
                community.setWaterAccessLevel(parseWaterAccessLevel(row.get("water_access_level")));
            }
            if (row.containsKey("primary_water_source")) {
                community.setPrimaryWaterSource(row.get("primary_water_source"));
            }
            if (row.containsKey("collection_date") && !row.get("collection_date").isEmpty()) {
                community.setCollectionDate(LocalDate.parse(row.get("collection_date")));
            }
            if (row.containsKey("notes")) {
                community.setNotes(row.get("notes"));
            }

            records.add(community);
        }

        return records;
    }

    /**
     * Converts validated rows to InfrastructureData entities.
     */
    private List<InfrastructureData> convertToInfrastructureData(
        List<Map<String, String>> validRows,
        String uploadId,
        String userId
    ) {
        List<InfrastructureData> records = new ArrayList<>();
        UUID uploadUUID = UUID.fromString(uploadId);
        UUID userUUID = UUID.fromString(userId);

        for (Map<String, String> row : validRows) {
            InfrastructureData infra = new InfrastructureData();
            infra.setUploadId(uploadUUID);
            infra.setUserId(userUUID);
            infra.setFacilityType(parseFacilityType(row.get("facility_type")));
            infra.setFacilityName(row.get("facility_name"));
            infra.setCoordinates(createPoint(row.get("latitude"), row.get("longitude")));
            infra.setOperationalStatus(parseOperationalStatus(row.get("operational_status")));
            infra.setSource(row.get("source"));

            if (row.containsKey("capacity") && !row.get("capacity").isEmpty()) {
                infra.setCapacity(new BigDecimal(row.get("capacity")));
            }
            if (row.containsKey("capacity_unit")) {
                infra.setCapacityUnit(row.get("capacity_unit"));
            }
            if (row.containsKey("population_served") && !row.get("population_served").isEmpty()) {
                infra.setPopulationServed(Integer.parseInt(row.get("population_served")));
            }
            if (row.containsKey("installation_date") && !row.get("installation_date").isEmpty()) {
                infra.setInstallationDate(LocalDate.parse(row.get("installation_date")));
            }
            if (row.containsKey("last_maintenance_date") && !row.get("last_maintenance_date").isEmpty()) {
                infra.setLastMaintenanceDate(LocalDate.parse(row.get("last_maintenance_date")));
            }
            if (row.containsKey("notes")) {
                infra.setNotes(row.get("notes"));
            }

            records.add(infra);
        }

        return records;
    }

    /**
     * Creates PostGIS Point from latitude and longitude.
     * Note: PostGIS uses (longitude, latitude) order!
     */
    private Point createPoint(String latitude, String longitude) {
        double lat = Double.parseDouble(latitude);
        double lon = Double.parseDouble(longitude);
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));  // longitude first!
    }

    /**
     * Parses datetime string (supports ISO 8601 formats).
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        // Try ISO 8601 with time first
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            // Fall back to date only
            return LocalDate.parse(dateTimeStr, DateTimeFormatter.ISO_DATE).atStartOfDay();
        }
    }

    /**
     * Parses water access level enum.
     */
    private CommunityData.WaterAccessLevel parseWaterAccessLevel(String level) {
        return switch (level.toUpperCase()) {
            case "NONE" -> CommunityData.WaterAccessLevel.NONE;
            case "LIMITED" -> CommunityData.WaterAccessLevel.LIMITED;
            case "BASIC" -> CommunityData.WaterAccessLevel.BASIC;
            case "SAFELY_MANAGED" -> CommunityData.WaterAccessLevel.SAFELY_MANAGED;
            default -> CommunityData.WaterAccessLevel.BASIC;  // Default fallback
        };
    }

    /**
     * Parses facility type enum.
     */
    private InfrastructureData.FacilityType parseFacilityType(String type) {
        return switch (type.toUpperCase()) {
            case "WELL" -> InfrastructureData.FacilityType.WELL;
            case "BOREHOLE" -> InfrastructureData.FacilityType.BOREHOLE;
            case "TREATMENT_PLANT" -> InfrastructureData.FacilityType.TREATMENT_PLANT;
            case "RESERVOIR" -> InfrastructureData.FacilityType.RESERVOIR;
            case "DISTRIBUTION_POINT" -> InfrastructureData.FacilityType.DISTRIBUTION_POINT;
            case "PUMP_STATION" -> InfrastructureData.FacilityType.PUMP_STATION;
            case "WATER_TOWER" -> InfrastructureData.FacilityType.WATER_TOWER;
            case "SPRING_PROTECTION" -> InfrastructureData.FacilityType.SPRING_PROTECTION;
            default -> InfrastructureData.FacilityType.OTHER;
        };
    }

    /**
     * Parses operational status enum.
     */
    private InfrastructureData.OperationalStatus parseOperationalStatus(String status) {
        return switch (status.toUpperCase()) {
            case "OPERATIONAL" -> InfrastructureData.OperationalStatus.OPERATIONAL;
            case "NON_OPERATIONAL" -> InfrastructureData.OperationalStatus.NON_OPERATIONAL;
            case "UNDER_MAINTENANCE" -> InfrastructureData.OperationalStatus.UNDER_MAINTENANCE;
            case "PLANNED" -> InfrastructureData.OperationalStatus.PLANNED;
            case "ABANDONED" -> InfrastructureData.OperationalStatus.ABANDONED;
            default -> InfrastructureData.OperationalStatus.NON_OPERATIONAL;
        };
    }

    /**
     * Lists user's uploads with pagination.
     *
     * @param userId User ID
     * @param page Page number (0-indexed)
     * @param pageSize Items per page
     * @param dataType Optional data type filter (hydro, community, infrastructure)
     * @return UploadListResponse with pagination
     */
    public com.water.data.dto.UploadListResponse listUploads(
        String userId,
        int page,
        int pageSize,
        String dataType
    ) {
        UUID userUUID = UUID.fromString(userId);

        // Get uploads (filtered by data type if specified)
        List<Upload> uploads;
        long totalItems;

        if (dataType != null && !dataType.isEmpty()) {
            Upload.DataType type = Upload.DataType.valueOf(dataType.toUpperCase());
            uploads = uploadRepository.findByUserIdAndDataType(userUUID, type);
            totalItems = uploads.size();
        } else {
            uploads = uploadRepository.findByUserId(userUUID);
            totalItems = uploads.size();
        }

        // Apply pagination manually (Spring Data Pageable can be added later)
        int start = page * pageSize;
        int end = Math.min(start + pageSize, uploads.size());

        List<Upload> paginatedUploads = uploads.subList(start, end);

        // Convert to DTOs
        List<com.water.data.dto.UploadSummaryDto> uploadDtos = paginatedUploads.stream()
            .map(com.water.data.dto.UploadSummaryDto::fromEntity)
            .toList();

        return com.water.data.dto.UploadListResponse.create(uploadDtos, page, pageSize, totalItems);
    }

    /**
     * Soft deletes an upload and releases storage quota.
     *
     * @param uploadId Upload ID
     * @param userId User ID (for authorization check)
     */
    @Transactional
    public void deleteUpload(String uploadId, String userId) {
        UUID uploadUUID = UUID.fromString(uploadId);
        UUID userUUID = UUID.fromString(userId);

        // Find upload
        Upload upload = uploadRepository.findById(uploadUUID)
            .orElseThrow(() -> new RuntimeException("Upload not found: " + uploadId));

        // Verify ownership
        if (!upload.getUserId().equals(userUUID)) {
            throw new RuntimeException("Unauthorized: Upload does not belong to user");
        }

        // Check if already deleted
        if (upload.isDeleted()) {
            throw new RuntimeException("Upload already deleted");
        }

        // Get file size to release quota
        double fileSizeMb = upload.getFileSizeMb();

        // Soft delete upload
        upload.softDelete();
        uploadRepository.save(upload);

        // Release storage quota
        User user = uploadRepository.findById(userUUID)
            .map(u -> {
                User foundUser = new User();
                foundUser.setId(userUUID);
                // Retrieve user to update quota
                return foundUser;
            })
            .orElse(null);

        // Update user quota (subtract storage used)
        if (user != null) {
            storageQuotaService.releaseQuota(userId, fileSizeMb);
        }

        System.out.println(String.format(
            "Soft deleted upload %s, released %.2f MB quota for user %s",
            uploadId,
            fileSizeMb,
            userId
        ));
    }

    /**
     * Calculates SHA-256 checksum of uploaded file.
     *
     * @param file Uploaded file
     * @return SHA-256 checksum as hex string (e.g., "sha256:a3c2f1e8b9d4...")
     */
    private String calculateChecksum(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = file.getBytes();
        byte[] hashBytes = digest.digest(fileBytes);

        // Convert to hex string
        StringBuilder hexString = new StringBuilder("sha256:");
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
