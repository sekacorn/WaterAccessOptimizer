package com.water.data.validator;

import com.water.data.dto.UploadResponse;
import com.water.data.dto.ValidationErrorDto;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete 5-stage validation pipeline for CSV uploads.
 *
 * Stage 1: File-level validation (size, format, encoding)
 * Stage 2: Schema validation (required columns, header normalization)
 * Stage 3: Row-level validation (data types, ranges, WHO guidelines)
 * Stage 4: Semantic validation (duplicates, geographic clustering)
 * Stage 5: Quality assessment (completeness score, confidence level)
 */
@Component
public class ValidationPipeline {

    private final FileValidator fileValidator;
    private final SchemaValidator schemaValidator;
    private final RowValidator rowValidator;
    private final SemanticValidator semanticValidator;
    private final QualityAssessment qualityAssessment;

    public ValidationPipeline(
        FileValidator fileValidator,
        SchemaValidator schemaValidator,
        RowValidator rowValidator,
        SemanticValidator semanticValidator,
        QualityAssessment qualityAssessment
    ) {
        this.fileValidator = fileValidator;
        this.schemaValidator = schemaValidator;
        this.rowValidator = rowValidator;
        this.semanticValidator = semanticValidator;
        this.qualityAssessment = qualityAssessment;
    }

    /**
     * Validates CSV file through all 5 stages.
     *
     * @param file Uploaded CSV file
     * @param dataType Type of data (hydro, community, infrastructure)
     * @return Validation result with errors, warnings, and quality assessment
     */
    public ValidationResult validateCsv(MultipartFile file, SchemaValidator.DataType dataType) {
        ValidationResult result = new ValidationResult();

        // Stage 1: File-level validation
        FileValidator.FileValidationResult fileResult = fileValidator.validateFile(file);
        if (!fileResult.isValid()) {
            fileResult.getErrors().forEach(error ->
                result.addError(ValidationErrorDto.fileError(error, null))
            );
            result.setStage(ValidationStage.FILE_LEVEL);
            return result;
        }

        // Parse CSV
        List<String[]> csvRows;
        try {
            csvRows = parseCsv(file);
        } catch (Exception e) {
            result.addError(ValidationErrorDto.fileError(
                "Failed to parse CSV file: " + e.getMessage(),
                "Ensure file is valid CSV format with proper line endings."
            ));
            result.setStage(ValidationStage.FILE_LEVEL);
            return result;
        }

        if (csvRows.isEmpty()) {
            result.addError(ValidationErrorDto.fileError(
                "CSV file is empty (no header or data rows).",
                "Add data to the file."
            ));
            result.setStage(ValidationStage.FILE_LEVEL);
            return result;
        }

        // Stage 2: Schema validation
        String[] headers = csvRows.get(0);
        SchemaValidator.SchemaValidationResult schemaResult =
            schemaValidator.validateSchema(headers, dataType);

        if (!schemaResult.isValid()) {
            schemaResult.getErrors().forEach(error ->
                result.addError(ValidationErrorDto.fileError(error, null))
            );
            result.setStage(ValidationStage.SCHEMA);
            return result;
        }

        // Stage 3: Row-level validation
        List<Map<String, String>> validRows = new ArrayList<>();
        Map<String, Integer> columnIndexMap = schemaResult.getColumnIndexMap();

        for (int i = 1; i < csvRows.size(); i++) { // Skip header (index 0)
            String[] row = csvRows.get(i);
            Map<String, String> rowMap = new HashMap<>();

            // Map columns to values
            for (Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
                int index = entry.getValue();
                if (index < row.length) {
                    rowMap.put(entry.getKey(), row[index]);
                }
            }

            // Validate row
            RowValidator.RowValidationResult rowResult = switch (dataType) {
                case HYDRO -> rowValidator.validateHydroRow(rowMap, i + 1);
                case COMMUNITY -> rowValidator.validateCommunityRow(rowMap, i + 1);
                case INFRASTRUCTURE -> rowValidator.validateInfrastructureRow(rowMap, i + 1);
            };

            // Collect errors and warnings
            for (RowValidator.ValidationError error : rowResult.getErrors()) {
                ValidationErrorDto dto = new ValidationErrorDto(
                    error.getRow(),
                    error.getField(),
                    error.getValue(),
                    error.getMessage(),
                    error.getSeverity().name().toLowerCase(),
                    error.getSuggestion()
                );

                switch (error.getSeverity()) {
                    case ERROR -> result.addError(dto);
                    case WARNING -> result.addWarning(dto);
                    case INFO -> result.addInfo(dto);
                }
            }

            // If row has no errors, add to valid rows for semantic validation
            if (!rowResult.hasErrors()) {
                validRows.add(rowMap);
            }
        }

        result.setTotalRows(csvRows.size() - 1); // Exclude header
        result.setValidRows(validRows.size());
        result.setFailedRows(result.getTotalRows() - validRows.size());
        result.setStage(ValidationStage.ROW_LEVEL);

        // Stage 4: Semantic validation (only if we have valid rows)
        if (!validRows.isEmpty()) {
            SemanticValidator.SemanticValidationResult semanticResult =
                semanticValidator.validateSemantic(validRows, dataType);

            for (SemanticValidator.ValidationWarning warning : semanticResult.getWarnings()) {
                result.addWarning(ValidationErrorDto.warning(
                    warning.getRow(),
                    warning.getType(),
                    null,
                    warning.getMessage(),
                    warning.getSuggestion()
                ));
            }

            result.setStage(ValidationStage.SEMANTIC);
        }

        // Stage 5: Quality assessment
        QualityAssessment.QualityAssessmentResult qualityResult = qualityAssessment.assess(
            validRows,
            dataType,
            result.getErrors().size(),
            result.getWarnings().size()
        );

        result.setQualityAssessment(qualityResult);
        result.setStage(ValidationStage.QUALITY_ASSESSMENT);

        return result;
    }

    /**
     * Parses CSV file into rows.
     */
    private List<String[]> parseCsv(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Simple CSV parsing (doesn't handle quoted fields with commas)
                // For production, use Apache Commons CSV or OpenCSV
                String[] columns = line.split(",", -1);
                rows.add(columns);
            }
        }

        return rows;
    }

    /**
     * Complete validation result.
     */
    public static class ValidationResult {
        private ValidationStage stage;
        private int totalRows;
        private int validRows;
        private int failedRows;
        private List<ValidationErrorDto> errors = new ArrayList<>();
        private List<ValidationErrorDto> warnings = new ArrayList<>();
        private List<ValidationErrorDto> infos = new ArrayList<>();
        private QualityAssessment.QualityAssessmentResult qualityAssessment;

        public ValidationStage getStage() {
            return stage;
        }

        public void setStage(ValidationStage stage) {
            this.stage = stage;
        }

        public int getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        public int getValidRows() {
            return validRows;
        }

        public void setValidRows(int validRows) {
            this.validRows = validRows;
        }

        public int getFailedRows() {
            return failedRows;
        }

        public void setFailedRows(int failedRows) {
            this.failedRows = failedRows;
        }

        public List<ValidationErrorDto> getErrors() {
            return errors;
        }

        public void addError(ValidationErrorDto error) {
            this.errors.add(error);
        }

        public List<ValidationErrorDto> getWarnings() {
            return warnings;
        }

        public void addWarning(ValidationErrorDto warning) {
            this.warnings.add(warning);
        }

        public List<ValidationErrorDto> getInfos() {
            return infos;
        }

        public void addInfo(ValidationErrorDto info) {
            this.infos.add(info);
        }

        public QualityAssessment.QualityAssessmentResult getQualityAssessment() {
            return qualityAssessment;
        }

        public void setQualityAssessment(QualityAssessment.QualityAssessmentResult qualityAssessment) {
            this.qualityAssessment = qualityAssessment;
        }

        /**
         * Returns true if validation passed all stages with no errors.
         */
        public boolean isSuccess() {
            return errors.isEmpty() && stage == ValidationStage.QUALITY_ASSESSMENT;
        }

        /**
         * Returns true if some rows passed validation.
         */
        public boolean isPartialSuccess() {
            return validRows > 0 && failedRows > 0;
        }

        /**
         * Converts validation result to API response.
         */
        public UploadResponse toUploadResponse(String uploadId, String fileChecksum, double storageMb) {
            if (errors.isEmpty() && stage == ValidationStage.QUALITY_ASSESSMENT) {
                // Success
                UploadResponse response = UploadResponse.success(
                    uploadId,
                    validRows,
                    fileChecksum,
                    storageMb,
                    0 // Processing time set by controller
                );

                // Include warnings and infos if any
                if (!warnings.isEmpty()) {
                    response.setWarnings(warnings);
                }
                return response;

            } else if (validRows > 0) {
                // Partial success
                UploadResponse.UploadSummary summary = new UploadResponse.UploadSummary(
                    totalRows,
                    validRows,
                    failedRows,
                    warnings.size()
                );

                // Combine errors and high-severity warnings
                List<ValidationErrorDto> allErrors = new ArrayList<>(errors);

                return UploadResponse.partialSuccess(
                    uploadId,
                    summary,
                    allErrors,
                    warnings,
                    fileChecksum,
                    storageMb
                );

            } else {
                // Complete failure
                return UploadResponse.failure(errors);
            }
        }
    }

    /**
     * Validation stages.
     */
    public enum ValidationStage {
        FILE_LEVEL,
        SCHEMA,
        ROW_LEVEL,
        SEMANTIC,
        QUALITY_ASSESSMENT
    }
}
