package com.water.data.controller;

import com.water.data.model.RiskAssessment;
import com.water.data.service.ExportService;
import com.water.data.service.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for exporting risk assessment reports.
 * Provides endpoints for downloading PDF and Excel exports.
 */
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;
    private final RiskAssessmentService assessmentService;

    /**
     * GET /api/v1/export/assessments/{id}/excel
     * Exports risk assessment to Excel format (.xlsx).
     *
     * Optional query parameters:
     * - riskLevel: Filter by risk level (HIGH, MEDIUM, LOW)
     *
     * @param id Assessment ID
     * @param riskLevel Optional filter
     * @param authentication JWT authentication
     * @return Excel file download
     */
    @GetMapping("/assessments/{id}/excel")
    public ResponseEntity<byte[]> exportToExcel(
        @PathVariable String id,
        @RequestParam(required = false) RiskAssessment.RiskLevel riskLevel,
        Authentication authentication
    ) {
        String userId = authentication.getName();

        try {
            log.info("User {} requesting Excel export for assessment {}", userId, id);

            // Get assessment for filename generation
            RiskAssessment assessment = assessmentService.getAssessment(id, userId);

            // Generate Excel file
            byte[] excelBytes = exportService.exportToExcel(id, userId, riskLevel);

            // Generate filename
            String filename = exportService.generateFilename(assessment, "xlsx", riskLevel);

            log.info("Generated Excel export: {} ({} bytes)", filename, excelBytes.length);

            // Return file download response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);

        } catch (RuntimeException e) {
            log.error("Error exporting to Excel: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        } catch (Exception e) {
            log.error("Unexpected error exporting to Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/v1/export/assessments/{id}/pdf
     * Exports risk assessment to PDF format.
     *
     * Optional query parameters:
     * - riskLevel: Filter by risk level (HIGH, MEDIUM, LOW)
     *
     * @param id Assessment ID
     * @param riskLevel Optional filter
     * @param authentication JWT authentication
     * @return PDF file download
     */
    @GetMapping("/assessments/{id}/pdf")
    public ResponseEntity<byte[]> exportToPdf(
        @PathVariable String id,
        @RequestParam(required = false) RiskAssessment.RiskLevel riskLevel,
        Authentication authentication
    ) {
        String userId = authentication.getName();

        try {
            log.info("User {} requesting PDF export for assessment {}", userId, id);

            // Get assessment for filename generation
            RiskAssessment assessment = assessmentService.getAssessment(id, userId);

            // Generate PDF file
            byte[] pdfBytes = exportService.exportToPdf(id, userId, riskLevel);

            // Generate filename
            String filename = exportService.generateFilename(assessment, "pdf", riskLevel);

            log.info("Generated PDF export: {} ({} bytes)", filename, pdfBytes.length);

            // Return file download response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (RuntimeException e) {
            log.error("Error exporting to PDF: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        } catch (Exception e) {
            log.error("Unexpected error exporting to PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/v1/export/health
     * Health check endpoint for export service.
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        return ResponseEntity.ok(new HealthResponse("healthy", "Export service is operational"));
    }

    /**
     * Health check response.
     */
    private record HealthResponse(String status, String message) {}
}
