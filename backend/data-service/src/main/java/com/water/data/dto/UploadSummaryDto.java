package com.water.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for upload summary in list responses.
 * Contains essential information about an upload without full validation details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadSummaryDto {

    private String uploadId;
    private String filename;
    private String dataType;  // hydro, community, infrastructure
    private String status;    // pending, processing, completed, failed

    private Integer recordsImported;
    private Integer recordsFailed;

    private Double fileSizeMb;
    private String fileChecksum;

    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;

    /**
     * Creates summary from Upload entity.
     */
    public static UploadSummaryDto fromEntity(com.water.data.model.Upload upload) {
        UploadSummaryDto dto = new UploadSummaryDto();
        dto.setUploadId(upload.getId().toString());
        dto.setFilename(upload.getFilename());
        dto.setDataType(upload.getDataType().name().toLowerCase());
        dto.setStatus(upload.getStatus().name().toLowerCase());
        dto.setRecordsImported(upload.getRecordsImported());
        dto.setRecordsFailed(upload.getRecordsFailed());
        dto.setFileSizeMb(upload.getFileSizeMb());
        dto.setFileChecksum(upload.getFileChecksum());
        dto.setUploadedAt(upload.getUploadedAt());
        dto.setProcessedAt(upload.getProcessedAt());
        return dto;
    }
}
