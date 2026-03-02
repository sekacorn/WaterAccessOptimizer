package com.water.data.validator;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 1 Validation: File-Level Validation
 * Checks file size, format, encoding, and structure before processing.
 */
@Component
public class FileValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "text/csv",
        "application/csv",
        "application/json",
        "application/geo+json",
        "application/vnd.geo+json"
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".csv", ".json", ".geojson");

    /**
     * Validates a file at the file level (size, format, encoding).
     *
     * @param file The uploaded file
     * @return Validation result with success status and error messages
     */
    public FileValidationResult validateFile(MultipartFile file) {
        List<String> errors = new ArrayList<>();

        // Check if file is null or empty
        if (file == null || file.isEmpty()) {
            errors.add("File is empty. Please upload a file with data.");
            return new FileValidationResult(false, errors);
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            long sizeInMB = file.getSize() / (1024 * 1024);
            errors.add(String.format(
                "File too large (%d MB). Maximum size is 10 MB. " +
                "Please split into multiple files or compress your data.",
                sizeInMB
            ));
        }

        // Check file type
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        if (!isValidFileType(contentType, filename)) {
            errors.add(
                "Invalid file type. Supported formats: CSV (.csv), JSON (.json), GeoJSON (.geojson). " +
                "Received: " + (filename != null ? filename : "unknown")
            );
        }

        // Check encoding (should be UTF-8)
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Check for UTF-8 encoding issues (replacement character indicates encoding problem)
            if (content.contains("\uFFFD")) {
                errors.add(
                    "File encoding issue detected. Please save file as UTF-8. " +
                    "In Excel: File > Save As > Tools > Web Options > Encoding > Unicode (UTF-8)"
                );
            }

            // Check if file is actually empty despite having size
            if (content.trim().isEmpty()) {
                errors.add("File appears to be empty (no readable content).");
            }

        } catch (IOException e) {
            errors.add(
                "Unable to read file. Please ensure file is not corrupted. " +
                "Error: " + e.getMessage()
            );
        }

        return new FileValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Checks if file type is valid based on content type and filename.
     */
    private boolean isValidFileType(String contentType, String filename) {
        // Check content type
        boolean validContentType = contentType != null &&
            ALLOWED_CONTENT_TYPES.stream().anyMatch(contentType::contains);

        // Check file extension
        boolean validExtension = filename != null &&
            ALLOWED_EXTENSIONS.stream().anyMatch(filename.toLowerCase()::endsWith);

        // Accept if either content type or extension is valid
        // (content type can be unreliable across browsers/OS)
        return validContentType || validExtension;
    }

    /**
     * Result of file-level validation.
     */
    public static class FileValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public FileValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorSummary() {
            return String.join("; ", errors);
        }
    }
}
