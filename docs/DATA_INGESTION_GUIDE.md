# Data Ingestion Guide for WaterAccessOptimizer

## Overview

This guide covers the complete data ingestion pipeline for WaterAccessOptimizer, from CSV upload to database storage. The system uses a **5-stage validation pipeline** to ensure data quality and provide user-friendly error reporting.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Supported Data Formats](#supported-data-formats)
3. [5-Stage Validation Pipeline](#5-stage-validation-pipeline)
4. [Error Handling](#error-handling)
5. [API Reference](#api-reference)
6. [Data Provenance Tracking](#data-provenance-tracking)
7. [Storage Quotas](#storage-quotas)
8. [Best Practices](#best-practices)

---

## Quick Start

### Upload via API

```bash
# 1. Upload hydrological data
curl -X POST http://localhost:8080/api/data/upload/hydro \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@sample_hydro_data.csv"

# 2. Upload community data
curl -X POST http://localhost:8080/api/data/upload/community \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@sample_community_data.csv"

# 3. Upload infrastructure data
curl -X POST http://localhost:8080/api/data/upload/infrastructure \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@sample_infrastructure_data.csv"
```

### Upload via Frontend (React)

```tsx
const handleUpload = async (file: File, dataType: 'hydro' | 'community' | 'infrastructure') => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`/api/data/upload/${dataType}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
    },
    body: formData,
  });

  const result = await response.json();

  if (result.status === 'success') {
    toast.success(`[X]${result.records_imported} records imported!`);
  } else {
    toast.error('Upload failed. See errors for details.');
  }
};
```

---

## Supported Data Formats

### CSV Files

**File Requirements**:
- **Max size**: 10 MB
- **Encoding**: UTF-8
- **Format**: Standard CSV with header row
- **Line endings**: CRLF or LF

**Supported Data Types**:
1. **Hydro Data**: Water quality measurements, groundwater levels, stream flow
2. **Community Data**: Population centers with water access information
3. **Infrastructure Data**: Water facilities with operational status

### GeoJSON Files (Future)

Planned for V1 release. Will support:
- Point geometries (water sources, facilities)
- LineString (pipelines, rivers)
- Polygon (water bodies, service areas)

---

## 5-Stage Validation Pipeline

### Stage 1: File-Level Validation

**Checks**:
- File size ≤ 10 MB
- File format (CSV, JSON, GeoJSON)
- Character encoding (UTF-8)
- File structure (valid CSV syntax)
- Non-empty file

**Implementation**: `FileValidator.java`

**Example Errors**:
```json
{
  "errors": [
    "File too large (15 MB). Maximum size is 10 MB. Please split into multiple files.",
    "File encoding issue detected. Please save file as UTF-8."
  ]
}
```

---

### Stage 2: Schema Validation

**Checks**:
- Required columns present
- Column name format (auto-normalized: lowercase, underscores)
- Column order (any order accepted)
- Extra columns (allowed, stored in JSONB metadata)

**Required Columns by Data Type**:

**Hydro Data**:
- `source`
- `latitude`
- `longitude`
- `measurement_value`
- `measurement_unit`
- `measurement_date`

**Community Data**:
- `community_name`
- `latitude`
- `longitude`
- `population`

**Infrastructure Data**:
- `facility_type`
- `facility_name`
- `latitude`
- `longitude`
- `operational_status`

**Implementation**: `SchemaValidator.java`

**Header Normalization**:
```
"Community Name" → "community_name"
"LAT"           → "lat" (mapped to latitude)
"Long"          → "long" (mapped to longitude)
```

---

### Stage 3: Row-Level Validation

**Common Validations**:

| Field | Rule | Error Message |
|-------|------|---------------|
| `latitude` | -90 ≤ lat ≤ 90 | "Invalid latitude: {value}. Must be between -90 and 90." |
| `longitude` | -180 ≤ lon ≤ 180 | "Invalid longitude: {value}. Must be between -180 and 180." |
| Text fields | No HTML tags, max length | "Field '{field}' exceeds maximum length of {max} characters." |
| Dates | Valid ISO 8601, not future | "Invalid date: {value}. Use format YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS." |

**Hydro Data Validations**:

| Field | Rule | Severity | Action |
|-------|------|----------|--------|
| `measurement_value` | Must be numeric | ERROR | Skip row |
| `parameter_name` | Known parameter | WARNING | Flag for review |
| Arsenic > 10 µg/L | WHO guideline exceeded | INFO | Flag, but import |
| pH 0-14 | Valid pH range | WARNING | Flag outlier |

**WHO Water Quality Guidelines** (for reference):
- **Arsenic**: 10 µg/L
- **Fluoride**: 1.5 mg/L
- **Nitrate**: 50 mg/L
- **Lead**: 10 µg/L
- **pH**: 6.5-8.5 (recommended)
- **Turbidity**: <5 NTU

**Community Data Validations**:

| Field | Rule | Severity | Action |
|-------|------|----------|--------|
| `population` | > 0, integer | ERROR | Skip row |
| `household_count` | If provided, > 0 | ERROR | Skip row |
| Pop/household ratio | 2 ≤ ratio ≤ 10 | WARNING | Flag outlier |
| `water_access_level` | One of: none, limited, basic, safely_managed | ERROR | Skip row |

**Infrastructure Data Validations**:

| Field | Rule | Severity | Action |
|-------|------|----------|--------|
| `facility_type` | Known type | ERROR | Skip row |
| `operational_status` | Known status | ERROR | Skip row |
| `capacity` | If provided, > 0 | ERROR | Skip row |
| Operational + no capacity | Operational should have capacity | WARNING | Flag missing data |

**Implementation**: `RowValidator.java`

---

### Stage 4: Semantic Validation

**Cross-Field Consistency Checks**:
1. **Coordinate Plausibility**: Flag if coordinates are far from dataset cluster
2. **Temporal Consistency**: Measurements from same location should have increasing dates
3. **Geographic Clustering**: Flag outliers >50km from dataset centroid

**Duplicate Detection**:
- **Exact Duplicates**: Same values in all key columns → Skip
- **Near Duplicates**: Same location + date, different values → Flag for review
- **External Source Duplicates**: Check `external_source_id` → Update existing record

**Example**:
```json
{
  "warnings": [
    {
      "row": 23,
      "message": "This location (12.5°N, 35.2°E) is 120 km from other data points in this file. Verify coordinates are correct."
    },
    {
      "row": 45,
      "message": "Duplicate detected: Same location, parameter, and date as row 12, but different value (existing: 10.5, new: 15.2). This may indicate a data quality issue."
    }
  ]
}
```

---

### Stage 5: Quality Assessment

**Completeness Score** (0-100):
- Percentage of optional fields populated
- Score = (populated_optional_fields / total_optional_fields) × 100

**Confidence Rating**:
- **HIGH**: All required + >75% optional fields, no warnings
- **MEDIUM**: All required + >50% optional fields, few warnings
- **LOW**: All required only, many warnings
- **NONE**: Missing required fields or critical errors

**Example Output**:
```json
{
  "quality_assessment": {
    "completeness_score": 85,
    "confidence_level": "HIGH",
    "error_count": 0,
    "warning_count": 2,
    "info_count": 5
  }
}
```

---

## Error Handling

### User-Friendly Error Messages

**Principles**:
1. **Be Specific**: Tell user exactly what's wrong and where
2. **Be Actionable**: Provide suggestions to fix the problem
3. **Be Kind**: Assume user error, not incompetence

### Error Response Format

```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "partial_success",
  "summary": {
    "total_rows": 150,
    "imported": 143,
    "failed": 7,
    "warnings": 12
  },
  "errors": [
    {
      "row": 5,
      "field": "latitude",
      "value": "91.5",
      "message": "Invalid latitude: 91.5. Must be between -90 and 90.",
      "severity": "error",
      "suggestion": "Check if latitude and longitude are swapped."
    },
    {
      "row": 12,
      "field": "measurement_unit",
      "value": null,
      "message": "Missing required field 'measurement_unit'.",
      "severity": "error",
      "suggestion": "Add a unit for this measurement (e.g., 'ppm', 'mg/L', 'meters')."
    }
  ],
  "warnings": [
    {
      "row": 23,
      "field": "measurement_value",
      "value": "125.5",
      "message": "Arsenic level (125.5 µg/L) exceeds WHO guideline (10 µg/L) by 12.5x.",
      "severity": "warning",
      "suggestion": "This may indicate contaminated water requiring urgent attention."
    }
  ],
  "file_checksum": "sha256:a3c2f1e8b9d4...",
  "storage_used_mb": 3.2
}
```

### Download Error Report

Users can download a CSV with all errors for easy review:

```csv
row,field,value,severity,message,suggestion
5,latitude,91.5,error,"Invalid latitude: 91.5. Must be between -90 and 90.","Check if latitude and longitude are swapped."
12,measurement_unit,,error,"Missing required field 'measurement_unit'.","Add a unit (e.g., 'ppm', 'mg/L', 'meters')."
23,measurement_value,125.5,warning,"Arsenic (125.5 µg/L) exceeds WHO guideline (10 µg/L).","This may indicate contaminated water requiring urgent attention."
```

---

## API Reference

### Upload Hydro Data

**Endpoint**: `POST /api/data/upload/hydro`

**Request**:
```http
POST /api/data/upload/hydro
Authorization: Bearer {jwt_token}
Content-Type: multipart/form-data

file: hydro_data.csv
```

**Response (Success)**:
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "success",
  "records_imported": 127,
  "records_failed": 0,
  "file_checksum": "sha256:a3c2f1e8b9d4...",
  "storage_used_mb": 2.3,
  "processing_time_ms": 1234
}
```

**Response (Partial Success)**:
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "partial_success",
  "summary": {
    "total_rows": 150,
    "imported": 143,
    "failed": 7,
    "warnings": 12
  },
  "errors": [...],
  "warnings": [...],
  "file_checksum": "sha256:a3c2f1e8b9d4...",
  "storage_used_mb": 3.2
}
```

**Error Responses**:
- `413 Payload Too Large`: File > 10MB
- `507 Insufficient Storage`: User quota exceeded
- `400 Bad Request`: Validation errors (see response body)
- `500 Internal Server Error`: Server error (contact support)

### List Uploads

**Endpoint**: `GET /api/data/uploads`

**Response**:
```json
{
  "uploads": [
    {
      "upload_id": "550e8400-e29b-41d4-a716-446655440000",
      "filename": "hydro_data.csv",
      "data_type": "hydro",
      "status": "completed",
      "records_imported": 127,
      "records_failed": 3,
      "uploaded_at": "2024-01-15T10:30:00Z",
      "file_size_mb": 2.3
    }
  ],
  "total_storage_used_mb": 15.7,
  "storage_quota_mb": 100
}
```

### Delete Upload

**Endpoint**: `DELETE /api/data/uploads/{upload_id}`

**Response**:
```json
{
  "success": true,
  "message": "Upload soft-deleted. Data will be permanently removed in 30 days.",
  "storage_freed_mb": 2.3
}
```

---

## Data Provenance Tracking

Every data record tracks its origin:

**Provenance Fields**:
- `upload_id`: UUID linking to upload metadata
- `uploaded_by`: User ID who uploaded
- `uploaded_at`: Server timestamp
- `source`: Data source ("USGS", "Field Survey", etc.)
- `filename`: Original filename
- `file_checksum`: SHA-256 hash of file
- `external_source_id`: ID in external system (for external APIs)
- `data_version`: Version/snapshot identifier

**Query Provenance**:
```sql
SELECT
  hd.location_name,
  hd.measurement_value,
  hd.source,
  u.filename,
  u.uploaded_at,
  usr.email as uploaded_by_email
FROM hydro_data hd
JOIN uploads u ON hd.upload_id = u.id
JOIN users usr ON hd.user_id = usr.id
WHERE hd.location_name = 'River Site 1';
```

**Benefits**:
- **Reproducibility**: Trace every result back to source data
- **Auditing**: Know when and by whom data was added
- **Deduplication**: Avoid importing same data multiple times
- **Change Detection**: Identify when external sources are updated

---

## Storage Quotas

**Default Quotas**:
- **Free Tier**: 100 MB per user
- **Admin**: 1 GB per user

**Quota Enforcement**:
1. Check available storage before upload
2. Reject upload if would exceed quota
3. Track storage usage in `users.storage_used_mb`
4. Update on upload and delete

**Check Storage**:
```http
GET /api/users/me
```

**Response**:
```json
{
  "id": "user-id",
  "email": "user@example.com",
  "storage_used_mb": 45.3,
  "storage_quota_mb": 100,
  "storage_available_mb": 54.7
}
```

---

## Best Practices

### Preparing Data for Upload

1. **Use UTF-8 Encoding**:
   - Excel: File > Save As > Tools > Web Options > Encoding > Unicode (UTF-8)
   - LibreOffice: File > Save As > Character Set > UTF-8

2. **Validate Coordinates**:
   - Latitude: -90 to 90
   - Longitude: -180 to 180
   - Check if lat/lon are swapped (common mistake)

3. **Use ISO 8601 Dates**:
   - `2024-01-15` (date only)
   - `2024-01-15T10:30:00` (date with time)
   - Avoid: `1/15/2024` or `15-Jan-24`

4. **Start Small**:
   - Upload 5-10 rows first
   - Fix any validation errors
   - Then upload full dataset

5. **Check Sample Data**:
   - Use `sample_data/` as templates
   - Copy structure, replace values

### Troubleshooting Common Issues

**Problem**: "Invalid latitude: 91.5"
- **Cause**: Latitude out of range or lat/lon swapped
- **Solution**: Check if lat and lon columns are swapped

**Problem**: "Missing required column: measurement_unit"
- **Cause**: CSV missing a required column
- **Solution**: Add the column or check spelling

**Problem**: "File encoding issue detected"
- **Cause**: File not saved as UTF-8
- **Solution**: Re-save file with UTF-8 encoding

**Problem**: "File too large (15 MB)"
- **Cause**: File exceeds 10 MB limit
- **Solution**: Split into multiple files or remove unnecessary columns

**Problem**: Upload succeeds but 0 records imported
- **Cause**: All rows failed validation
- **Solution**: Download error report CSV to see specific errors

### Data Quality Tips

1. **Include Optional Fields**: Higher completeness score → higher confidence
2. **Recent Data**: Data <6 months old is best
3. **Geographic Overlap**: Ensure datasets cover same area
4. **Sufficient Samples**: Aim for 30+ hydro measurements, 10+ communities, 10+ facilities for HIGH confidence

---

## Example Workflows

### Workflow 1: Upload and Analyze

1. Upload 3 datasets (hydro, community, infrastructure)
2. Check upload status and fix any errors
3. Create risk assessment
4. View results on interactive map

### Workflow 2: Incremental Updates

1. Upload initial dataset (e.g., 50 hydro measurements)
2. Run risk assessment
3. Upload additional data (e.g., 20 more measurements)
4. Re-run risk assessment with updated data
5. Compare results to see impact of new data

### Workflow 3: Data Correction

1. Upload dataset with errors
2. Download error report CSV
3. Fix errors in original CSV
4. Delete failed upload
5. Re-upload corrected CSV

---

## Support

For issues with data upload:
- Check validation errors in upload response
- Review file format specifications
- Consult sample data in `sample_data/` directory
- Report bugs at https://github.com/anthropics/wateraccessoptimizer/issues

## References

- **Agent 02**: DATA_SOURCES_CONNECTORS.md (complete data ingestion spec)
- **Database Schema**: database/README.md
- **API Contracts**: docs/API_CONTRACTS_MVP.md
- **Sample Data**: sample_data/README.md
