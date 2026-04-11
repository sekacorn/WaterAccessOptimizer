# Complete Validation Pipeline Guide

> Status: reference guide. Confirm implementation details against the current shipped services and UI before relying on this operationally.

## Overview

The WaterAccessOptimizer validation pipeline implements a comprehensive **5-stage validation system** to ensure data quality, provide user-friendly error messages, and calculate confidence levels for uploaded data.

## Pipeline Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    File Upload (CSV)                           │
└────────────────────────┬───────────────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────────┐
│ Stage 1: File-Level Validation                                │
│ - File size ≤ 10MB                                             │
│ - File format (CSV, JSON, GeoJSON)                             │
│ - Character encoding (UTF-8)                                   │
│ - File structure (valid CSV)                                   │
│ Time: ~10ms                                                    │
└────────────────────────┬───────────────────────────────────────┘
                         │ ❌ FAIL → Return file-level errors
                         ▼ [X]PASS
┌────────────────────────────────────────────────────────────────┐
│ Stage 2: Schema Validation                                     │
│ - Required columns present                                     │
│ - Header normalization ("Community Name" → "community_name")  │
│ - Column mapping for parsing                                   │
│ Time: ~5ms                                                     │
└────────────────────────┬───────────────────────────────────────┘
                         │ ❌ FAIL → Return schema errors
                         ▼ [X]PASS
┌────────────────────────────────────────────────────────────────┐
│ Stage 3: Row-Level Validation                                  │
│ - Coordinate validation (lat: -90 to 90, lon: -180 to 180)   │
│ - WHO guideline checking (arsenic, fluoride, nitrate, etc.)   │
│ - Enum validation (water_access_level, operational_status)    │
│ - Business rules (population/household ratio: 2-10)           │
│ Time: ~1-2ms per row                                           │
└────────────────────────┬───────────────────────────────────────┘
                         │ ❌ ALL FAIL → Return all errors
                         ▼ [X]SOME PASS → Continue with valid rows
┌────────────────────────────────────────────────────────────────┐
│ Stage 4: Semantic Validation                                   │
│ - Duplicate detection (exact and near duplicates)              │
│ - Geographic outlier detection (>50km from centroid)           │
│ - Temporal consistency (dates ordered for same location)       │
│ Time: ~100-500ms                                               │
└────────────────────────┬───────────────────────────────────────┘
                         │ ⚠️ WARNINGS → Flag for review
                         ▼ [X]CONTINUE
┌────────────────────────────────────────────────────────────────┐
│ Stage 5: Quality Assessment                                    │
│ - Completeness score (0-100, % of optional fields populated)  │
│ - Confidence level (HIGH/MEDIUM/LOW/NONE)                     │
│ - Error/warning/info counts                                    │
│ Time: ~100ms                                                   │
└────────────────────────┬───────────────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────────┐
│ Return UploadResponse with:                                    │
│ - Status (SUCCESS/PARTIAL_SUCCESS/FAILED)                     │
│ - Valid row count, failed row count                            │
│ - Errors, warnings, info messages                              │
│ - Quality assessment (completeness, confidence)                │
└────────────────────────────────────────────────────────────────┘
```

## Stage Details

### Stage 1: File-Level Validation

**Class**: `FileValidator.java`

**Purpose**: Validates file before expensive parsing operations.

**Checks**:
1. File not null or empty
2. File size ≤ 10 MB
3. Valid file type (CSV, JSON, GeoJSON)
4. UTF-8 encoding
5. Non-empty content

**Example Error**:
```json
{
  "message": "File too large (15 MB). Maximum size is 10 MB. Please split into multiple files or compress your data.",
  "severity": "error"
}
```

**Performance**: ~10ms

---

### Stage 2: Schema Validation

**Class**: `SchemaValidator.java`

**Purpose**: Validates CSV structure and required columns.

**Checks**:
1. All required columns present
2. Header normalization (case-insensitive, space handling)
3. Column order (any order accepted)
4. Extra columns (allowed, stored in metadata)

**Required Columns by Data Type**:
- **Hydro**: source, latitude, longitude, measurement_value, measurement_unit, measurement_date
- **Community**: community_name, latitude, longitude, population
- **Infrastructure**: facility_type, facility_name, latitude, longitude, operational_status

**Header Normalization**:
- "Community Name" → "community_name"
- "LATITUDE" → "latitude"
- "Measurement Value" → "measurement_value"

**Example Error**:
```json
{
  "message": "Missing required columns: measurement_unit. Expected columns for hydro data: source, latitude, longitude, measurement_value, measurement_unit, measurement_date",
  "severity": "error"
}
```

**Performance**: ~5ms

---

### Stage 3: Row-Level Validation

**Class**: `RowValidator.java`

**Purpose**: Validates individual rows against business rules and WHO guidelines.

**Checks**:
1. **Coordinate Validation**:
   - Latitude: -90 to 90
   - Longitude: -180 to 180
   - Swap detection suggestion

2. **WHO Guideline Checking** (INFO severity):
   - Arsenic: ≤10 µg/L
   - Fluoride: ≤1.5 mg/L
   - Nitrate: ≤50 mg/L
   - Lead: ≤10 µg/L
   - Turbidity: <5 NTU

3. **Enum Validation** (ERROR severity):
   - Water access level: none, limited, basic, safely_managed
   - Facility type: well, borehole, treatment_plant, reservoir, etc.
   - Operational status: operational, non_operational, under_maintenance, etc.

4. **Business Rules** (WARNING severity):
   - Population/household ratio: 2-10 typical
   - Operational facility should have capacity

**Three Severity Levels**:
- **ERROR** ❌: Prevents import (missing required fields, invalid data types)
- **WARNING** ⚠️: Imports but flags (unusual ratios, missing optional fields)
- **INFO** ℹ️: Informational (WHO guideline exceedances - still valid data!)

**Example Errors**:
```json
// ERROR: Invalid coordinate
{
  "row": 5,
  "field": "latitude",
  "value": "91.5",
  "message": "Invalid latitude: 91.500000. Must be between -90 and 90.",
  "severity": "error",
  "suggestion": "Check if latitude and longitude are swapped."
}

// INFO: WHO guideline exceedance
{
  "row": 12,
  "field": "measurement_value",
  "value": "75.50",
  "message": "Arsenic level (75.50) exceeds WHO guideline (10.00) by 7.5x. This may indicate contaminated water requiring urgent attention.",
  "severity": "info",
  "suggestion": "Consider testing additional samples to confirm contamination level."
}

// WARNING: Unusual ratio
{
  "row": 8,
  "field": "household_count",
  "value": "1000",
  "message": "Population/household ratio (1.2) is unusual. Typical range is 2-10 people per household.",
  "severity": "warning",
  "suggestion": "Verify population and household count are correct."
}
```

**Performance**: ~1-2ms per row

---

### Stage 4: Semantic Validation

**Class**: `SemanticValidator.java`

**Purpose**: Validates cross-field consistency and data relationships.

**Checks**:
1. **Duplicate Detection**:
   - Exact duplicates: Same key fields (location + parameter + date for hydro)
   - Near duplicates: Same location/date, different values
   - Deduplication key varies by data type

2. **Geographic Outlier Detection**:
   - Calculate dataset centroid
   - Flag points >50km from centroid
   - Requires ≥3 valid coordinates
   - Uses Haversine formula for distance

3. **Temporal Consistency** (Hydro data only):
   - Group measurements by location
   - Check if dates are chronologically ordered
   - Warn if later measurement appears before earlier one

**Example Warnings**:
```json
// Duplicate
{
  "row": 5,
  "type": "duplicate",
  "message": "Duplicate detected: Rows 5, 12, 23 have identical key fields. Only the first occurrence will be imported.",
  "severity": "warning",
  "suggestion": "Review if these are truly duplicates or if data needs correction."
}

// Geographic outlier
{
  "row": 15,
  "type": "geographic_outlier",
  "message": "This location (12.5000°N, 35.2000°E) is 120.5 km from other data points in this file. Verify coordinates are correct.",
  "severity": "warning",
  "suggestion": "Check if this point should be in a different dataset or if coordinates were entered incorrectly."
}

// Temporal inconsistency
{
  "row": 8,
  "type": "temporal_inconsistency",
  "message": "Measurements at same location (rows 8 and 12) appear out of chronological order. Earlier date (2024-01-10) appears after later date (2024-01-15).",
  "severity": "warning",
  "suggestion": "Consider sorting data by date for better time-series analysis."
}
```

**Performance**: ~100-500ms

---

### Stage 5: Quality Assessment

**Class**: `QualityAssessment.java`

**Purpose**: Calculates completeness score and confidence level.

**Metrics**:

1. **Completeness Score** (0-100):
   - Percentage of optional fields populated
   - Score = (populated_optional_fields / total_optional_fields) × 100

2. **Confidence Level** (HIGH/MEDIUM/LOW/NONE):
   - Scoring algorithm:
     - Completeness: 0-40 points (>75% = 40, >50% = 25, else 10)
     - Error penalty: -10 points per error
     - Warning penalty: -2 points per warning
     - Row count: 0-60 points (≥30 = 60, ≥10 = 40, ≥5 = 20, ≥1 = 10)
   - Thresholds:
     - **HIGH**: ≥80 points
     - **MEDIUM**: 50-79 points
     - **LOW**: 20-49 points
     - **NONE**: <20 points

**Example Assessment**:
```json
{
  "completeness_score": 85,
  "confidence_level": "HIGH",
  "error_count": 0,
  "warning_count": 2
}
```

**Interpretation**:
- **HIGH**: >30 rows, >75% completeness, no/few errors → Ready for production risk assessment
- **MEDIUM**: >10 rows, >50% completeness, some errors → Acceptable for preliminary assessment
- **LOW**: >5 rows, basic completeness, many errors → Data quality issues, review before use
- **NONE**: <5 rows or critical errors → Insufficient data for meaningful assessment

**Performance**: ~100ms

---

## Complete Pipeline Usage

### Example: Successful Upload

**Input**: `sample_hydro_data.csv` (12 rows, all valid, 1 high arsenic)

**Processing Flow**:
1. **Stage 1**: [X]Pass (file size OK, UTF-8, valid CSV)
2. **Stage 2**: [X]Pass (all required columns present)
3. **Stage 3**: [X]Pass (12/12 rows valid, 1 INFO for high arsenic)
4. **Stage 4**: [X]Pass (no duplicates, no outliers, temporal consistency OK)
5. **Stage 5**: [X]Pass (completeness 85%, confidence MEDIUM, 0 errors, 0 warnings)

**Response**:
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "records_imported": 12,
  "records_failed": 0,
  "warnings": [
    {
      "row": 3,
      "field": "measurement_value",
      "value": "75.50",
      "message": "Arsenic level (75.50) exceeds WHO guideline (10.00) by 7.5x...",
      "severity": "info",
      "suggestion": "Consider testing additional samples..."
    }
  ],
  "quality_assessment": {
    "completeness_score": 85,
    "confidence_level": "MEDIUM"
  },
  "file_checksum": "sha256:a3c2f1e8b9d4...",
  "storage_used_mb": 0.02,
  "processing_time_ms": 1234
}
```

---

### Example: Partial Success

**Input**: 100 rows, 85 valid, 15 invalid

**Processing Flow**:
1. **Stage 1**: [X]Pass
2. **Stage 2**: [X]Pass
3. **Stage 3**: ⚠️ Partial (85/100 valid, 15 errors)
4. **Stage 4**: ⚠️ 3 duplicates detected, 1 geographic outlier
5. **Stage 5**: [X]Pass (completeness 70%, confidence MEDIUM)

**Response**:
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PARTIAL_SUCCESS",
  "summary": {
    "total_rows": 100,
    "imported": 85,
    "failed": 15,
    "warnings": 4
  },
  "errors": [
    // 15 row-level errors
  ],
  "warnings": [
    // 4 semantic warnings (duplicates, outliers)
  ],
  "quality_assessment": {
    "completeness_score": 70,
    "confidence_level": "MEDIUM"
  },
  "file_checksum": "sha256:b4d3e2f9c8a1...",
  "storage_used_mb": 0.12
}
```

---

### Example: Complete Failure

**Input**: Missing required column (`longitude`)

**Processing Flow**:
1. **Stage 1**: [X]Pass
2. **Stage 2**: ❌ **FAIL** - Missing required column
3. **Stage 3**: Not reached
4. **Stage 4**: Not reached
5. **Stage 5**: Not reached

**Response**:
```json
{
  "status": "FAILED",
  "errors": [
    {
      "message": "Missing required columns: longitude. Expected columns for hydro data: source, latitude, longitude, measurement_value, measurement_unit, measurement_date",
      "severity": "error"
    }
  ]
}
```

---

## Performance Benchmarks

| Dataset Size | Total Time | Breakdown |
|--------------|------------|-----------|
| 10 rows | ~50ms | Stage 1: 10ms, Stage 2: 5ms, Stage 3: 20ms, Stage 4: 10ms, Stage 5: 5ms |
| 100 rows | ~320ms | Stage 1: 10ms, Stage 2: 5ms, Stage 3: 200ms, Stage 4: 100ms, Stage 5: 5ms |
| 1000 rows | ~2.2s | Stage 1: 10ms, Stage 2: 5ms, Stage 3: 2s, Stage 4: 100ms, Stage 5: 100ms |
| 10000 rows | ~22s | Stage 1: 10ms, Stage 2: 5ms, Stage 3: 20s, Stage 4: 1s, Stage 5: 1s |

**Bottleneck**: Stage 3 (row-level validation) dominates for large files.

**Optimization Opportunities** (Future):
1. **Parallel Validation**: Validate rows in parallel (4-8x speedup)
2. **Early Termination**: Stop after N errors for quick feedback
3. **Async Processing**: Queue large files for background processing

---

## Integration Example

```java
@RestController
@RequestMapping("/api/data")
public class DataUploadController {

    @Autowired
    private ValidationPipeline validationPipeline;

    @PostMapping("/upload/hydro")
    public ResponseEntity<UploadResponse> uploadHydroData(
        @RequestParam("file") MultipartFile file
    ) {
        long startTime = System.currentTimeMillis();

        // Run validation pipeline
        ValidationPipeline.ValidationResult result =
            validationPipeline.validateCsv(file, SchemaValidator.DataType.HYDRO);

        // Calculate processing time
        long processingTime = System.currentTimeMillis() - startTime;

        // Convert to API response
        String uploadId = UUID.randomUUID().toString();
        String fileChecksum = calculateChecksum(file);
        double storageMb = file.getSize() / (1024.0 * 1024.0);

        UploadResponse response = result.toUploadResponse(
            uploadId,
            fileChecksum,
            storageMb
        );
        response.setProcessingTimeMs(processingTime);

        // If success or partial success, save to database
        if (result.isSuccess() || result.isPartialSuccess()) {
            saveToDatabase(result, uploadId);
        }

        return ResponseEntity.ok(response);
    }
}
```

---

## Testing the Pipeline

### Unit Tests

```java
@Test
void testCompleteValidation_Success() {
    MultipartFile file = createMockCsv("sample_hydro_data.csv");
    ValidationPipeline.ValidationResult result =
        pipeline.validateCsv(file, SchemaValidator.DataType.HYDRO);

    assertTrue(result.isSuccess());
    assertEquals(12, result.getValidRows());
    assertEquals(0, result.getFailedRows());
    assertEquals(QualityAssessment.ConfidenceLevel.MEDIUM,
        result.getQualityAssessment().getConfidenceLevel());
}

@Test
void testCompleteValidation_PartialSuccess() {
    MultipartFile file = createMockCsvWithErrors("mixed_data.csv");
    ValidationPipeline.ValidationResult result =
        pipeline.validateCsv(file, SchemaValidator.DataType.HYDRO);

    assertTrue(result.isPartialSuccess());
    assertTrue(result.getValidRows() > 0);
    assertTrue(result.getFailedRows() > 0);
}

@Test
void testCompleteValidation_MissingColumn() {
    MultipartFile file = createMockCsvMissingColumn("missing_longitude.csv");
    ValidationPipeline.ValidationResult result =
        pipeline.validateCsv(file, SchemaValidator.DataType.HYDRO);

    assertFalse(result.isSuccess());
    assertEquals(ValidationPipeline.ValidationStage.SCHEMA, result.getStage());
    assertTrue(result.getErrors().get(0).getMessage().contains("Missing required columns"));
}
```

---

## Best Practices

1. **Use ValidationPipeline** as single entry point (don't call individual validators directly)
2. **Check stage** in result to understand where validation failed
3. **Display quality assessment** to users (helps them understand data quality)
4. **Group errors by type** in UI (file-level, schema, row-level, semantic)
5. **Provide download** for error report CSV (for offline review)
6. **Show completeness score** with suggestions for improvement

---

## Future Enhancements

### V1 Enhancements
- **Parallel validation**: Validate rows concurrently for 4-8x speedup
- **Streaming validation**: Process large files (>100MB) in chunks
- **Advanced duplicate detection**: Fuzzy matching for near-duplicates
- **Machine learning validation**: Learn from user corrections to improve validation rules

### V2 Enhancements
- **Cross-dataset validation**: Check consistency across multiple uploads
- **Time-series validation**: Detect anomalies in measurement trends
- **Geospatial validation**: Validate against known geographic features (coastlines, water bodies)

---

## References

- **Agent 02**: DATA_SOURCES_CONNECTORS.md (validation specification)
- **Implementation**: backend/data-service/src/main/java/com/water/data/validator/
- **Examples**: docs/VALIDATION_EXAMPLES.md
- **API Contract**: docs/API_CONTRACTS_MVP.md

