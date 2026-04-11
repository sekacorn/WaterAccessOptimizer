# Validation Examples for WaterAccessOptimizer

> Status: examples/reference. They illustrate validation concepts but should be checked against the current active upload implementation.

This document provides real-world validation examples showing how the 5-stage validation pipeline works.

## Table of Contents

1. [Stage 1: File-Level Validation Examples](#stage-1-file-level-validation-examples)
2. [Stage 2: Schema Validation Examples](#stage-2-schema-validation-examples)
3. [Stage 3: Row-Level Validation Examples](#stage-3-row-level-validation-examples)
4. [Complete Upload Examples](#complete-upload-examples)

---

## Stage 1: File-Level Validation Examples

### Example 1: File Too Large

**Input**: 15 MB CSV file

**Response**:
```json
{
  "status": "FAILED",
  "errors": [
    {
      "message": "File too large (15 MB). Maximum size is 10 MB. Please split into multiple files or compress your data.",
      "severity": "error",
      "suggestion": null
    }
  ]
}
```

**Fix**: Split file into multiple smaller files or remove unnecessary columns.

---

### Example 2: Invalid Encoding

**Input**: CSV file saved with ISO-8859-1 encoding

**Response**:
```json
{
  "status": "FAILED",
  "errors": [
    {
      "message": "File encoding issue detected. Please save file as UTF-8. In Excel: File > Save As > Tools > Web Options > Encoding > Unicode (UTF-8)",
      "severity": "error",
      "suggestion": null
    }
  ]
}
```

**Fix**: Re-save file with UTF-8 encoding in Excel or text editor.

---

## Stage 2: Schema Validation Examples

### Example 3: Missing Required Column

**Input CSV** (hydro data):
```csv
source,latitude,measurement_value,measurement_unit,measurement_date
Field Survey,0.3476,125,ppm,2024-01-10
```

**Missing**: `longitude` column

**Response**:
```json
{
  "status": "FAILED",
  "errors": [
    {
      "message": "Missing required columns: longitude. Expected columns for hydro data: source, latitude, longitude, measurement_value, measurement_unit, measurement_date",
      "severity": "error",
      "suggestion": null
    }
  ]
}
```

**Fix**: Add the `longitude` column to the CSV.

---

### Example 4: Column Name Variations (Auto-Normalized)

**Input CSV**:
```csv
Source,LATITUDE,Longitude,Measurement Value,Measurement Unit,Measurement Date
Field Survey,0.3476,32.5825,125,ppm,2024-01-10
```

**Result**: [X]**Success** - Headers automatically normalized:
- "Source" → "source"
- "LATITUDE" → "latitude"
- "Longitude" → "longitude"
- "Measurement Value" → "measurement_value"
- "Measurement Unit" → "measurement_unit"
- "Measurement Date" → "measurement_date"

---

## Stage 3: Row-Level Validation Examples

### Example 5: Invalid Latitude (Out of Range)

**Input CSV**:
```csv
source,latitude,longitude,measurement_value,measurement_unit,measurement_date
Field Survey,91.5,32.5825,125,ppm,2024-01-10
```

**Response**:
```json
{
  "status": "PARTIAL_SUCCESS",
  "summary": {
    "total_rows": 1,
    "imported": 0,
    "failed": 1,
    "warnings": 0
  },
  "errors": [
    {
      "row": 1,
      "field": "latitude",
      "value": "91.5",
      "message": "Invalid latitude: 91.500000. Must be between -90 and 90.",
      "severity": "error",
      "suggestion": "Check if latitude and longitude are swapped."
    }
  ]
}
```

**Fix**: Latitude should be between -90 and 90. Check if lat/lon are swapped.

---

### Example 6: Arsenic Exceeds WHO Guideline

**Input CSV**:
```csv
source,data_type,location_name,latitude,longitude,measurement_value,measurement_unit,measurement_date,parameter_name
Field Survey,water_quality,Well Alpha,0.3476,32.5825,75.5,µg/L,2024-01-10,arsenic
```

**Response**:
```json
{
  "status": "SUCCESS",
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "records_imported": 1,
  "records_failed": 0,
  "warnings": [
    {
      "row": 1,
      "field": "measurement_value",
      "value": "75.50",
      "message": "Arsenic level (75.50) exceeds WHO guideline (10.00) by 7.5x. This may indicate contaminated water requiring urgent attention.",
      "severity": "info",
      "suggestion": "Consider testing additional samples to confirm contamination level."
    }
  ],
  "file_checksum": "sha256:a3c2f1e8b9d4...",
  "storage_used_mb": 0.02
}
```

**Note**: This is **INFO**, not ERROR. Data is imported but flagged for attention.

---

### Example 7: Population/Household Ratio Warning

**Input CSV** (community data):
```csv
community_name,latitude,longitude,population,household_count
Village A,0.3450,32.5800,1200,1000
```

**Response**:
```json
{
  "status": "SUCCESS",
  "upload_id": "550e8400-e29b-41d4-a716-446655440001",
  "records_imported": 1,
  "records_failed": 0,
  "warnings": [
    {
      "row": 1,
      "field": "household_count",
      "value": "1000",
      "message": "Population/household ratio (1.2) is unusual. Typical range is 2-10 people per household.",
      "severity": "warning",
      "suggestion": "Verify population and household count are correct."
    }
  ]
}
```

**Note**: This is a **WARNING**. Data is imported but flagged for review.

---

### Example 8: Missing Measurement Unit

**Input CSV**:
```csv
source,latitude,longitude,measurement_value,measurement_unit,measurement_date
Field Survey,0.3476,32.5825,125,,2024-01-10
```

**Response**:
```json
{
  "status": "PARTIAL_SUCCESS",
  "summary": {
    "total_rows": 1,
    "imported": 0,
    "failed": 1,
    "warnings": 0
  },
  "errors": [
    {
      "row": 1,
      "field": "measurement_unit",
      "value": null,
      "message": "Missing required field 'measurement_unit'.",
      "severity": "error",
      "suggestion": "Add a unit for this measurement (e.g., 'ppm', 'mg/L', 'µg/L', 'meters', 'pH', 'NTU')."
    }
  ]
}
```

**Fix**: Add measurement_unit (e.g., "ppm", "mg/L").

---

### Example 9: Invalid Water Access Level

**Input CSV** (community data):
```csv
community_name,latitude,longitude,population,water_access_level
Village A,0.3450,32.5800,1200,good
```

**Response**:
```json
{
  "status": "PARTIAL_SUCCESS",
  "summary": {
    "total_rows": 1,
    "imported": 0,
    "failed": 1,
    "warnings": 0
  },
  "errors": [
    {
      "row": 1,
      "field": "water_access_level",
      "value": "good",
      "message": "Invalid water_access_level: 'good'. Must be one of: none, limited, basic, safely_managed (WHO JMP Service Ladder).",
      "severity": "error",
      "suggestion": "Use a valid WHO JMP service level. See documentation for definitions."
    }
  ]
}
```

**Fix**: Use valid WHO JMP levels: "none", "limited", "basic", or "safely_managed".

---

### Example 10: Invalid Date Format

**Input CSV**:
```csv
source,latitude,longitude,measurement_value,measurement_unit,measurement_date
Field Survey,0.3476,32.5825,125,ppm,1/15/2024
```

**Response**:
```json
{
  "status": "PARTIAL_SUCCESS",
  "summary": {
    "total_rows": 1,
    "imported": 0,
    "failed": 1,
    "warnings": 0
  },
  "errors": [
    {
      "row": 1,
      "field": "measurement_date",
      "value": "1/15/2024",
      "message": "Invalid date format: '1/15/2024'. Use ISO 8601 format: YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS",
      "severity": "error",
      "suggestion": "Example valid dates: 2024-01-15 or 2024-01-15T10:30:00"
    }
  ]
}
```

**Fix**: Use ISO 8601 format: "2024-01-15" or "2024-01-15T10:30:00".

---

### Example 11: Operational Facility Without Capacity (Warning)

**Input CSV** (infrastructure data):
```csv
facility_type,facility_name,latitude,longitude,operational_status
borehole,Borehole Alpha,0.3476,32.5825,operational
```

**Response**:
```json
{
  "status": "SUCCESS",
  "upload_id": "550e8400-e29b-41d4-a716-446655440002",
  "records_imported": 1,
  "records_failed": 0,
  "warnings": [
    {
      "row": 1,
      "field": "capacity",
      "value": null,
      "message": "Operational facilities should have capacity specified.",
      "severity": "warning",
      "suggestion": "Add capacity information for better risk assessment accuracy."
    }
  ]
}
```

**Note**: This is a **WARNING**. Data is imported but missing optional field that would improve risk assessment.

---

## Complete Upload Examples

### Example 12: Perfect Upload (All Valid)

**Input CSV** (sample_hydro_data.csv):
```csv
source,data_type,location_name,latitude,longitude,measurement_value,measurement_unit,measurement_date,parameter_name
Field Survey,water_quality,Well Alpha,0.3476,32.5825,125,ppm,2024-01-10T10:30:00,TDS
Field Survey,water_quality,Well Alpha,0.3476,32.5825,8.2,pH,2024-01-10T10:30:00,pH
Field Survey,water_quality,Well Beta,0.3512,32.5901,89,ppm,2024-01-10T11:00:00,TDS
```

**Response**:
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "records_imported": 3,
  "records_failed": 0,
  "file_checksum": "sha256:a3c2f1e8b9d4abc123...",
  "storage_used_mb": 0.02,
  "processing_time_ms": 1234
}
```

---

### Example 13: Partial Success (Some Valid, Some Invalid)

**Input CSV**:
```csv
source,latitude,longitude,measurement_value,measurement_unit,measurement_date
Field Survey,0.3476,32.5825,125,ppm,2024-01-10
Field Survey,91.5,32.5901,89,ppm,2024-01-11
Field Survey,0.3550,32.5950,234,,2024-01-12
Field Survey,0.3600,32.6020,15.5,µg/L,2024-01-13
```

**Response**:
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440003",
  "status": "PARTIAL_SUCCESS",
  "summary": {
    "total_rows": 4,
    "imported": 2,
    "failed": 2,
    "warnings": 0
  },
  "errors": [
    {
      "row": 2,
      "field": "latitude",
      "value": "91.5",
      "message": "Invalid latitude: 91.500000. Must be between -90 and 90.",
      "severity": "error",
      "suggestion": "Check if latitude and longitude are swapped."
    },
    {
      "row": 3,
      "field": "measurement_unit",
      "value": null,
      "message": "Missing required field 'measurement_unit'.",
      "severity": "error",
      "suggestion": "Add a unit for this measurement (e.g., 'ppm', 'mg/L', 'µg/L', 'meters', 'pH', 'NTU')."
    }
  ],
  "file_checksum": "sha256:b4d3e2f9c8a1def456...",
  "storage_used_mb": 0.01
}
```

**Result**: Rows 1 and 4 imported successfully. Rows 2 and 3 failed validation.

---

### Example 14: Complete Failure (All Rows Invalid)

**Input CSV**:
```csv
source,latitude,longitude,measurement_value,measurement_unit,measurement_date
Field Survey,91.5,200.0,invalid,ppm,bad-date
Field Survey,,,-50,,
```

**Response**:
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440004",
  "status": "PARTIAL_SUCCESS",
  "summary": {
    "total_rows": 2,
    "imported": 0,
    "failed": 2,
    "warnings": 0
  },
  "errors": [
    {
      "row": 1,
      "field": "latitude",
      "value": "91.5",
      "message": "Invalid latitude: 91.500000. Must be between -90 and 90.",
      "severity": "error",
      "suggestion": "Check if latitude and longitude are swapped."
    },
    {
      "row": 1,
      "field": "longitude",
      "value": "200.0",
      "message": "Invalid longitude: 200.000000. Must be between -180 and 180.",
      "severity": "error",
      "suggestion": "Check if latitude and longitude are swapped."
    },
    {
      "row": 1,
      "field": "measurement_value",
      "value": "invalid",
      "message": "Measurement value must be a number.",
      "severity": "error",
      "suggestion": "Ensure the value is numeric (e.g., 125.5, not 'high' or 'N/A')."
    },
    {
      "row": 1,
      "field": "measurement_date",
      "value": "bad-date",
      "message": "Invalid date format: 'bad-date'. Use ISO 8601 format: YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS",
      "severity": "error",
      "suggestion": "Example valid dates: 2024-01-15 or 2024-01-15T10:30:00"
    },
    {
      "row": 2,
      "field": "latitude",
      "value": null,
      "message": "Missing required field 'latitude'.",
      "severity": "error",
      "suggestion": "Add latitude coordinate in decimal degrees."
    },
    {
      "row": 2,
      "field": "longitude",
      "value": null,
      "message": "Missing required field 'longitude'.",
      "severity": "error",
      "suggestion": "Add longitude coordinate in decimal degrees."
    },
    {
      "row": 2,
      "field": "measurement_value",
      "value": "-50",
      "message": "Measurement value must be a number.",
      "severity": "error",
      "suggestion": "Ensure the value is numeric (e.g., 125.5, not 'high' or 'N/A')."
    },
    {
      "row": 2,
      "field": "measurement_unit",
      "value": null,
      "message": "Missing required field 'measurement_unit'.",
      "severity": "error",
      "suggestion": "Add a unit for this measurement (e.g., 'ppm', 'mg/L', 'µg/L', 'meters', 'pH', 'NTU')."
    },
    {
      "row": 2,
      "field": "measurement_date",
      "value": null,
      "message": "Missing required field 'measurement_date'.",
      "severity": "error",
      "suggestion": "Use ISO 8601 format: YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS"
    }
  ]
}
```

**Fix**: Download error report CSV, fix all errors, and re-upload.

---

## Validation Severity Levels

### ERROR (❌ Prevents Import)
- Missing required fields
- Invalid data types (non-numeric for numeric fields)
- Out-of-range values (lat/lon, dates)
- Invalid enum values (water_access_level, operational_status)

### WARNING (⚠️ Import but Flag)
- Unusual values (population/household ratio outside 2-10)
- Missing optional fields that would improve accuracy
- Future dates (should be historical)
- Unknown parameter names

### INFO (ℹ️ Informational Only)
- WHO guideline exceedances (high arsenic, fluoride, etc.)
- These are still imported but flagged for attention

---

## Testing Your CSV

### Quick Validation Checklist

Before uploading, check:

1. [X]**File Size**: < 10 MB
2. [X]**Encoding**: UTF-8
3. [X]**Required Columns**: All present (see schema for your data type)
4. [X]**Coordinates**: Latitude -90 to 90, Longitude -180 to 180
5. [X]**Dates**: ISO 8601 format (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS)
6. [X]**Enum Values**: Use valid options (see documentation)
7. [X]**Numeric Fields**: No text in numeric columns

### Common Mistakes

| Mistake | Example | Fix |
|---------|---------|-----|
| Lat/Lon swapped | lat: 32.5825, lon: 0.3476 | Swap them: lat: 0.3476, lon: 32.5825 |
| Wrong date format | 1/15/2024 | Use 2024-01-15 |
| Wrong encoding | File shows � characters | Save as UTF-8 |
| Text in numeric field | "high" instead of 75.5 | Use numeric value |
| Invalid enum | "good" for water_access_level | Use "basic" or "safely_managed" |

---

## Support

For issues with validation:
- Review error messages carefully - they include suggestions
- Download error report CSV for offline review
- Check sample data in `sample_data/` for correct format
- Consult docs/DATA_INGESTION_GUIDE.md for full specifications
- Report bugs at https://github.com/anthropics/wateraccessoptimizer/issues
