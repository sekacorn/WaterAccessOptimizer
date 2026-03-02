# Data Validation Rules - MVP v0.1.0

**Based on**: Agent 02 (Data Sources) + Agent 03 (Domain Model/DB Schema)
**Last Updated**: 2026-02-02 (Iteration 4)

---

## Purpose

This document defines all validation rules for CSV and GeoJSON file uploads in the Water Access Optimizer MVP. These rules ensure data quality, prevent invalid data from entering the database, and provide helpful error messages to users.

---

## File-Level Validation

### 1. File Size

**Rule**: Maximum file size is 10MB

**Validation**:
```
if (fileSize > 10 * 1024 * 1024) {
    throw new ValidationException("File size exceeds 10MB limit");
}
```

**Error Response**:
```http
HTTP 413 Payload Too Large
{
  "error": "File too large",
  "message": "Maximum file size is 10MB. Your file is 15.2MB.",
  "code": "FILE_TOO_LARGE"
}
```

---

### 2. File Type

**Rule**: Only CSV and GeoJSON files are accepted

**Validation**:
```java
String fileExtension = getFileExtension(filename);
String mimeType = request.getContentType();

if (!isValidFileType(fileExtension, mimeType)) {
    throw new ValidationException("Invalid file type");
}
```

**Valid Combinations**:
- `.csv` with `text/csv` or `application/vnd.ms-excel`
- `.geojson` or `.json` with `application/geo+json` or `application/json`

**Error Response**:
```http
HTTP 400 Bad Request
{
  "error": "Invalid file type",
  "message": "Only CSV and GeoJSON files are supported. Received: .xlsx",
  "code": "INVALID_FILE_TYPE"
}
```

---

### 3. Storage Quota

**Rule**: User's total storage must not exceed their quota (default: 100MB)

**Validation**:
```java
User user = getUserById(userId);
long newStorageUsed = user.getStorageUsedMb() + (fileSize / (1024.0 * 1024.0));

if (newStorageUsed > user.getStorageQuotaMb()) {
    throw new QuotaExceededException("Storage quota exceeded");
}
```

**Error Response**:
```http
HTTP 507 Insufficient Storage
{
  "error": "Storage quota exceeded",
  "message": "You have used 98MB of your 100MB quota. This file is 5MB.",
  "quota_mb": 100,
  "used_mb": 98.0,
  "available_mb": 2.0,
  "requested_mb": 5.0,
  "code": "QUOTA_EXCEEDED"
}
```

---

## CSV Validation

### Common Rules (All CSV Types)

#### 1. File Encoding

**Rule**: CSV files must be UTF-8 encoded

**Validation**: Attempt to decode as UTF-8, throw error if invalid characters

**Error Response**:
```json
{
  "error": "Invalid file encoding",
  "message": "CSV file must be UTF-8 encoded. Found invalid character at byte 245.",
  "code": "INVALID_ENCODING"
}
```

---

#### 2. CSV Structure

**Rule**: CSV files must have a header row and at least one data row

**Validation**:
```java
CSVParser parser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withHeader());

if (parser.getRecords().isEmpty()) {
    throw new ValidationException("CSV file is empty");
}

if (parser.getHeaderMap().isEmpty()) {
    throw new ValidationException("CSV file missing header row");
}
```

**Error Response**:
```json
{
  "error": "Empty CSV file",
  "message": "CSV file must contain a header row and at least one data row.",
  "code": "EMPTY_FILE"
}
```

---

#### 3. Required Columns

**Rule**: CSV must contain all required columns for its data type

**Validation**: Check header map contains all required column names (case-insensitive)

**Error Response**:
```json
{
  "error": "Missing required columns",
  "message": "CSV file missing required columns: latitude, longitude",
  "required_columns": ["source", "latitude", "longitude", "measurement_value", "measurement_unit", "measurement_date"],
  "found_columns": ["source", "measurement_value", "measurement_unit", "measurement_date"],
  "missing_columns": ["latitude", "longitude"],
  "code": "MISSING_COLUMNS"
}
```

---

### Hydrological Data CSV Validation

**Data Type**: `hydro`

**Required Columns**:
- `source`
- `latitude`
- `longitude`
- `measurement_value`
- `measurement_unit`
- `measurement_date`

**Optional Columns**:
- `data_type`
- `location_name`
- `depth_meters`
- `parameter_name`
- `notes`

#### Field Validation Rules

| Field | Type | Constraints | Example | Error Code |
|-------|------|-------------|---------|------------|
| `source` | String | Non-empty, max 100 chars | "USGS" | `INVALID_SOURCE` |
| `latitude` | Float | -90 to 90 | 39.7392 | `INVALID_LATITUDE` |
| `longitude` | Float | -180 to 180 | -104.9903 | `INVALID_LONGITUDE` |
| `measurement_value` | Numeric | Any valid number | 15.5 | `INVALID_MEASUREMENT` |
| `measurement_unit` | String | Non-empty, max 50 chars | "ppm" | `INVALID_UNIT` |
| `measurement_date` | DateTime | ISO 8601 format | "2024-01-15T10:30:00" or "2024-01-15" | `INVALID_DATE` |
| `data_type` | Enum | water_quality, aquifer_level, stream_flow, precipitation, groundwater_level | "water_quality" | `INVALID_DATA_TYPE` |
| `depth_meters` | Numeric | ≥ 0 | 45.0 | `INVALID_DEPTH` |
| `parameter_name` | String | Max 100 chars | "arsenic" | `INVALID_PARAMETER` |

**Example Valid Row**:
```csv
source,data_type,location_name,latitude,longitude,measurement_value,measurement_unit,measurement_date,parameter_name
USGS,water_quality,Colorado River Site 301,39.7392,-104.9903,15.5,ppm,2024-01-15T10:30:00,arsenic
```

**Example Validation Errors**:
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "completed",
  "records_imported": 127,
  "records_failed": 3,
  "errors": [
    {
      "row": 5,
      "column": "latitude",
      "error": "Invalid latitude: 91.5 (must be between -90 and 90)",
      "provided_value": "91.5",
      "error_code": "INVALID_LATITUDE"
    },
    {
      "row": 12,
      "column": "measurement_date",
      "error": "Invalid date format: 01/15/2024 (expected ISO 8601: YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS)",
      "provided_value": "01/15/2024",
      "error_code": "INVALID_DATE"
    },
    {
      "row": 23,
      "column": "data_type",
      "error": "Invalid data_type: 'water_level' (must be one of: water_quality, aquifer_level, stream_flow, precipitation, groundwater_level)",
      "provided_value": "water_level",
      "error_code": "INVALID_DATA_TYPE"
    }
  ]
}
```

---

### Community Data CSV Validation

**Data Type**: `community`

**Required Columns**:
- `community_name`
- `latitude`
- `longitude`
- `population`

**Optional Columns**:
- `water_access_level`
- `source`
- `household_count`
- `primary_water_source`
- `collection_date`
- `notes`

#### Field Validation Rules

| Field | Type | Constraints | Example | Error Code |
|-------|------|-------------|---------|------------|
| `community_name` | String | Non-empty, max 200 chars | "Kikuyu Village" | `INVALID_COMMUNITY_NAME` |
| `latitude` | Float | -90 to 90 | 1.2921 | `INVALID_LATITUDE` |
| `longitude` | Float | -180 to 180 | 36.8219 | `INVALID_LONGITUDE` |
| `population` | Integer | > 0 | 4500 | `INVALID_POPULATION` |
| `household_count` | Integer | > 0 | 850 | `INVALID_HOUSEHOLD_COUNT` |
| `water_access_level` | Enum | none, limited, basic, safely_managed | "limited" | `INVALID_ACCESS_LEVEL` |
| `primary_water_source` | Enum | well, borehole, piped_water, surface_water, rainwater, vendor, other | "well" | `INVALID_WATER_SOURCE` |
| `collection_date` | Date | ISO 8601 (YYYY-MM-DD) | "2024-01-05" | `INVALID_DATE` |

**WHO JMP Service Ladder Definitions** (for `water_access_level`):
- **none**: Surface water collection, >30 min round trip
- **limited**: Improved source, >30 min round trip or unreliable
- **basic**: Improved source, ≤30 min round trip
- **safely_managed**: Accessible on premises, available when needed, free of contamination

**Example Valid Row**:
```csv
community_name,latitude,longitude,population,water_access_level,primary_water_source,collection_date
Kikuyu Village,1.2921,36.8219,4500,limited,well,2024-01-05
```

---

### Infrastructure Data CSV Validation

**Data Type**: `infrastructure`

**Required Columns**:
- `facility_type`
- `facility_name`
- `latitude`
- `longitude`
- `operational_status`

**Optional Columns**:
- `capacity`
- `capacity_unit`
- `installation_date`
- `last_maintenance_date`
- `population_served`
- `notes`

#### Field Validation Rules

| Field | Type | Constraints | Example | Error Code |
|-------|------|-------------|---------|------------|
| `facility_type` | Enum | well, borehole, treatment_plant, reservoir, distribution_point, pump_station, water_tower, spring_protection, other | "borehole" | `INVALID_FACILITY_TYPE` |
| `facility_name` | String | Non-empty, max 200 chars | "Village Well 7" | `INVALID_FACILITY_NAME` |
| `latitude` | Float | -90 to 90 | 0.5143 | `INVALID_LATITUDE` |
| `longitude` | Float | -180 to 180 | 35.2698 | `INVALID_LONGITUDE` |
| `operational_status` | Enum | operational, non_operational, under_maintenance, planned, abandoned | "operational" | `INVALID_STATUS` |
| `capacity` | Numeric | > 0 | 5000 | `INVALID_CAPACITY` |
| `capacity_unit` | Enum | liters_per_day, liters_per_hour, liters, cubic_meters | "liters_per_day" | `INVALID_CAPACITY_UNIT` |
| `installation_date` | Date | ISO 8601 (YYYY-MM-DD), not future | "2018-03-15" | `INVALID_DATE` |
| `last_maintenance_date` | Date | ISO 8601 (YYYY-MM-DD), not future | "2023-12-01" | `INVALID_DATE` |
| `population_served` | Integer | > 0 | 800 | `INVALID_POPULATION_SERVED` |

**Additional Validation Rules**:
- If `capacity` is provided, `capacity_unit` must also be provided
- `last_maintenance_date` must be >= `installation_date` (if both provided)
- `installation_date` must not be in the future

**Example Valid Row**:
```csv
facility_type,facility_name,latitude,longitude,operational_status,capacity,capacity_unit,population_served
borehole,Village Well 7,0.5143,35.2698,operational,5000,liters_per_day,800
```

---

## GeoJSON Validation

### GeoJSON Structure Validation

**Rule**: GeoJSON must follow the RFC 7946 specification

**Required Fields**:
- `type`: Must be "FeatureCollection"
- `features`: Array of Feature objects

**Feature Object Requirements**:
- `type`: Must be "Feature"
- `geometry`: Valid geometry object
- `properties`: Object with data attributes

**Validation**:
```java
JsonNode geojson = objectMapper.readTree(file);

if (!geojson.has("type") || !"FeatureCollection".equals(geojson.get("type").asText())) {
    throw new ValidationException("GeoJSON must be a FeatureCollection");
}

if (!geojson.has("features") || !geojson.get("features").isArray()) {
    throw new ValidationException("GeoJSON must have a 'features' array");
}
```

**Error Response**:
```json
{
  "error": "Invalid GeoJSON structure",
  "message": "GeoJSON must be a FeatureCollection with a 'features' array",
  "code": "INVALID_GEOJSON"
}
```

---

### Geometry Type Validation

**Rule**: Only Point, LineString, and Polygon geometries are supported

**Supported Types**:
- **Point**: For wells, boreholes, communities
- **LineString**: For pipelines, rivers (V1 feature)
- **Polygon**: For water bodies, service areas (V1 feature)

**MVP**: Only Point geometries are supported

**Validation**:
```java
String geometryType = feature.get("geometry").get("type").asText();

if (!"Point".equals(geometryType)) {
    throw new ValidationException("Only Point geometries are supported in MVP");
}
```

**Error Response**:
```json
{
  "error": "Unsupported geometry type",
  "message": "Only Point geometries are supported. Found: LineString. LineString support coming in V1.",
  "code": "UNSUPPORTED_GEOMETRY"
}
```

---

### Coordinates Validation

**Rule**: Coordinates must be valid longitude/latitude pairs

**Point Geometry Format**:
```json
{
  "type": "Point",
  "coordinates": [longitude, latitude]
}
```

**Validation**:
- Longitude: -180 to 180
- Latitude: -90 to 90
- Order: **[lon, lat]** (GeoJSON standard, opposite of CSV!)

**Common Error**: CSV uses `(lat, lon)` but GeoJSON uses `[lon, lat]`

**Example Valid Point**:
```json
{
  "type": "Point",
  "coordinates": [36.8219, 1.2921]  // [lon, lat]
}
```

---

### Properties Validation

**Rule**: Properties object must contain required fields based on auto-detected data type

**Data Type Auto-Detection Logic**:
```java
if (properties.has("facility_type") || properties.has("type")) {
    dataType = "infrastructure";
} else if (properties.has("population")) {
    dataType = "community";
} else if (properties.has("measurement_value")) {
    dataType = "hydro";
} else {
    dataType = "generic";
}
```

**Required Properties by Data Type**:
- **Infrastructure**: `name` or `facility_name`, `facility_type`, `operational_status`
- **Community**: `name` or `community_name`, `population`
- **Hydro**: `measurement_value`, `measurement_unit`, `measurement_date`

---

## Cross-Field Validation

### 1. Duplicate Detection

**Rule**: Prevent duplicate uploads using SHA-256 file checksum

**Validation**:
```java
String checksum = calculateSHA256(fileBytes);
Upload existingUpload = uploadRepository.findByFileChecksum(checksum);

if (existingUpload != null && existingUpload.getUserId().equals(userId)) {
    throw new DuplicateUploadException("This file has already been uploaded");
}
```

**Error Response**:
```http
HTTP 409 Conflict
{
  "error": "Duplicate upload",
  "message": "This file has already been uploaded on 2024-01-10.",
  "existing_upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "uploaded_at": "2024-01-10T14:30:00Z",
  "code": "DUPLICATE_UPLOAD"
}
```

---

### 2. Date Range Validation

**Rule**: Dates must be reasonable (not far future, not too far past)

**Constraints**:
- `measurement_date`: Not more than 100 years in the past, not more than 1 day in the future
- `collection_date`: Not more than 100 years in the past, not more than 1 day in the future
- `installation_date`: Not more than 150 years in the past (some infrastructure is old!), not in the future
- `last_maintenance_date`: Not more than installation_date, not in the future

**Validation**:
```java
LocalDate date = LocalDate.parse(dateStr);
LocalDate now = LocalDate.now();
LocalDate minDate = now.minusYears(100);
LocalDate maxDate = now.plusDays(1);

if (date.isBefore(minDate) || date.isAfter(maxDate)) {
    throw new ValidationException("Date out of reasonable range");
}
```

**Error Response**:
```json
{
  "row": 15,
  "column": "measurement_date",
  "error": "Date out of reasonable range: 2125-01-15 (must be between 1924-01-01 and 2024-02-03)",
  "provided_value": "2125-01-15",
  "error_code": "DATE_OUT_OF_RANGE"
}
```

---

### 3. Spatial Bounding Box

**Rule**: Coordinates must be on Earth's surface (no (0, 0) unless explicitly allowed)

**Validation**:
```java
// Warn if coordinates are (0, 0) - likely data error
if (Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001) {
    warnings.add("Coordinates (0, 0) detected - verify this is correct");
}

// Reject if coordinates are exactly (0, 0) and no location_name provided
if (latitude == 0.0 && longitude == 0.0 && StringUtils.isBlank(locationName)) {
    throw new ValidationException("Coordinates (0, 0) detected with no location name - likely data error");
}
```

---

## Batch Validation Strategy

For large CSV files, validation is performed in batches:

1. **Parse file in chunks** (1000 rows at a time)
2. **Validate each chunk**
3. **Collect errors** (max 100 errors reported)
4. **Continue processing** valid rows even if some rows fail
5. **Return summary** with partial import results

**Example Partial Import Response**:
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "completed",
  "records_total": 1250,
  "records_imported": 1220,
  "records_failed": 30,
  "errors": [
    // First 100 errors (truncated if more)
  ],
  "warnings": [
    "30 rows failed validation and were skipped"
  ],
  "storage_used_mb": 2.3
}
```

---

## Validation Error Response Format

All validation errors follow this structure:

```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "completed" | "failed",
  "records_imported": 127,
  "records_failed": 3,
  "errors": [
    {
      "row": 5,                           // Row number (1-indexed, excluding header)
      "column": "latitude",               // Column name
      "error": "Invalid latitude: ...",   // Human-readable error message
      "provided_value": "91.5",           // Value that failed validation
      "error_code": "INVALID_LATITUDE"    // Machine-readable error code
    }
  ],
  "warnings": [
    "Coordinates (0, 0) detected in row 12 - verify this is correct"
  ]
}
```

---

## Validation Performance

**Target**: Validate and import 10,000 rows in <10 seconds

**Optimization Strategies**:
1. **Batch inserts**: Insert rows in batches of 1000
2. **Parallel validation**: Validate chunks in parallel using CompletableFuture
3. **Early termination**: Stop reading file if >1000 errors detected
4. **Streaming**: Use streaming parsers (Jackson for JSON, Apache Commons CSV)
5. **Database constraints**: Let PostgreSQL constraints catch edge cases

**Example Batch Insert**:
```java
// Batch insert 1000 rows at a time
jdbcTemplate.batchUpdate(
    "INSERT INTO hydro_data (upload_id, user_id, source, coordinates, ...) VALUES (?, ?, ?, ST_GeogFromText(?), ...)",
    hydroDataList,
    1000,  // Batch size
    (ps, hydroData) -> {
        ps.setObject(1, hydroData.getUploadId());
        ps.setObject(2, hydroData.getUserId());
        ps.setString(3, hydroData.getSource());
        ps.setString(4, "POINT(" + hydroData.getLongitude() + " " + hydroData.getLatitude() + ")");
        // ... set other fields
    }
);
```

---

## Error Code Reference

| Code | Meaning | HTTP Status |
|------|---------|-------------|
| `FILE_TOO_LARGE` | File size > 10MB | 413 |
| `INVALID_FILE_TYPE` | Not CSV or GeoJSON | 400 |
| `QUOTA_EXCEEDED` | Storage quota exceeded | 507 |
| `INVALID_ENCODING` | Not UTF-8 encoded | 400 |
| `EMPTY_FILE` | No data rows | 400 |
| `MISSING_COLUMNS` | Required columns missing | 400 |
| `INVALID_SOURCE` | Invalid source field | 400 |
| `INVALID_LATITUDE` | Latitude not -90 to 90 | 400 |
| `INVALID_LONGITUDE` | Longitude not -180 to 180 | 400 |
| `INVALID_MEASUREMENT` | Invalid numeric value | 400 |
| `INVALID_UNIT` | Invalid or missing unit | 400 |
| `INVALID_DATE` | Invalid or incorrectly formatted date | 400 |
| `INVALID_DATA_TYPE` | Invalid enum value for data_type | 400 |
| `INVALID_DEPTH` | Depth < 0 | 400 |
| `INVALID_COMMUNITY_NAME` | Missing or too long | 400 |
| `INVALID_POPULATION` | Population ≤ 0 | 400 |
| `INVALID_ACCESS_LEVEL` | Invalid WHO JMP level | 400 |
| `INVALID_WATER_SOURCE` | Invalid water source type | 400 |
| `INVALID_FACILITY_TYPE` | Invalid facility type | 400 |
| `INVALID_FACILITY_NAME` | Missing or too long | 400 |
| `INVALID_STATUS` | Invalid operational status | 400 |
| `INVALID_CAPACITY` | Capacity ≤ 0 | 400 |
| `INVALID_CAPACITY_UNIT` | Invalid capacity unit | 400 |
| `DATE_OUT_OF_RANGE` | Date too far past or future | 400 |
| `DUPLICATE_UPLOAD` | File already uploaded | 409 |
| `INVALID_GEOJSON` | Malformed GeoJSON | 400 |
| `UNSUPPORTED_GEOMETRY` | Geometry type not supported | 400 |

---

## Testing Validation Rules

**Unit Tests**: Test each validation rule individually

**Example Test**:
```java
@Test
void testLatitudeValidation_OutOfRange_ThrowsException() {
    CSVRecord record = createRecord("source", "USGS", "latitude", "91.5", "longitude", "-104.99");

    ValidationException exception = assertThrows(
        ValidationException.class,
        () -> hydroDataValidator.validateRow(record, 5)
    );

    assertThat(exception.getMessage()).contains("Invalid latitude: 91.5");
    assertThat(exception.getErrorCode()).isEqualTo("INVALID_LATITUDE");
    assertThat(exception.getRowNumber()).isEqualTo(5);
}
```

**Integration Tests**: Test with full CSV files

**Example Test Files**:
- `tests/fixtures/data/hydro-valid.csv` - All valid rows
- `tests/fixtures/data/hydro-invalid-latitude.csv` - Contains invalid latitude
- `tests/fixtures/data/community-missing-columns.csv` - Missing required columns
- `tests/fixtures/data/infrastructure-capacity-unit-mismatch.csv` - Capacity without unit

---

**Document Status**: [X]Complete
**Last Updated**: 2026-02-02 (Iteration 4)
**Next Review**: After Sprint 3 (data upload implementation complete)
