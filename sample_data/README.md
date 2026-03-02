# Sample Data for WaterAccessOptimizer

This directory contains sample CSV files for testing the data ingestion pipeline.

## Overview

These sample datasets represent a realistic water access scenario in Central Uganda (Kampala region) with:
- **12 hydrological measurements** (water quality and groundwater levels)
- **5 communities** with varying water access levels
- **6 infrastructure facilities** (boreholes, wells, treatment plants, pump stations)

## Files

### 1. sample_hydro_data.csv (12 rows)

Water quality and aquifer level measurements from field surveys.

**Parameters measured**:
- **TDS** (Total Dissolved Solids): Indicates mineral content
- **pH**: Water acidity/alkalinity (ideal: 6.5-8.5)
- **Arsenic**: Toxic contaminant (WHO guideline: ≤10 µg/L)
- **Turbidity**: Water clarity (WHO guideline: <5 NTU)
- **Fluoride**: Essential mineral (WHO guideline: ≤1.5 mg/L)
- **Depth to water**: Groundwater table depth

**Key Findings**:
- ⚠️ Well Alpha: Arsenic 75.5 µg/L (exceeds WHO guideline by 7.5x)
- ⚠️ River Site 1: Arsenic 15.5 µg/L (exceeds guideline), High turbidity
- [X]Borehole Gamma: Arsenic 5.2 µg/L (within safe limits)

### 2. sample_community_data.csv (5 rows)

Population centers with water access information based on WHO JMP Service Ladder.

**Water Access Levels**:
- **Safely Managed**: Improved source, on-premises, available, free of contamination (Kololo Estate)
- **Basic**: Improved source ≤30 min round trip (Makerere Community)
- **Limited**: Improved source >30 min round trip (Kasubi Village, Butabika Village)
- **None**: Surface water collection (Nakawa Settlement - HIGH RISK)

**Total Population**: 6,450 people across 5 communities

### 3. sample_infrastructure_data.csv (6 rows)

Water facilities with operational status and capacity.

**Operational Status**:
- [X]**Operational**: 4 facilities (Borehole Alpha, Treatment Plant, Water Tower, Pump Station)
- ❌ **Non-operational**: 1 facility (Borehole Beta - broken pump)
- ⚠️ **Under Maintenance**: 1 facility (Community Well Delta)

**Total Capacity**: 2,086,000 liters/day + 2,000,000 liters storage

## Geographic Coverage

**Bounding Box**:
- Latitude: 0.3350° to 0.3600° N
- Longitude: 32.5800° to 32.6150° E
- Area: ~28 km × ~35 km = ~980 km²

**Coordinate System**: WGS84 (SRID 4326)

## Usage

### Quick Start

1. **Upload to WaterAccessOptimizer**:
   ```bash
   # Via API
   curl -X POST http://localhost:8080/api/data/upload/hydro \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -F "file=@sample_hydro_data.csv"

   curl -X POST http://localhost:8080/api/data/upload/community \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -F "file=@sample_community_data.csv"

   curl -X POST http://localhost:8080/api/data/upload/infrastructure \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -F "file=@sample_infrastructure_data.csv"
   ```

2. **Via Frontend**:
   - Navigate to "Analyze" page
   - Click "Upload Data"
   - Select data type (Hydro / Community / Infrastructure)
   - Choose file and click "Upload"

### Run Risk Assessment

After uploading all 3 datasets:

1. Navigate to "Analyze" page
2. Click "Create Risk Assessment"
3. Select the 3 uploaded datasets
4. Click "Run Analysis"
5. View results on interactive map

**Expected Output**:
- **HIGH Risk**: Nakawa Settlement (no safe water, using surface water)
- **MEDIUM Risk**: Kasubi Village (limited access, nearest facility non-operational)
- **LOW Risk**: Makerere Community, Kololo Estate, Butabika Village (basic/safely managed access)

## Data Quality

This sample dataset provides **MEDIUM confidence** level:
- [X]12 hydro measurements (minimum: 10 for MEDIUM)
- [X]5 communities (minimum: 5 for MEDIUM)
- [X]6 facilities (minimum: 3 for MEDIUM)
- [X]Recent data (2024-01-05 to 2024-01-12)
- [X]Geographic overlap (all within 10km radius)

To achieve **HIGH confidence**, you would need:
- 30+ hydro measurements
- 10+ communities
- 10+ facilities

## File Format Specifications

### Hydro Data Format

**Required Columns**:
- `source` - Data source (e.g., "Field Survey", "USGS")
- `latitude` - Decimal degrees (-90 to 90)
- `longitude` - Decimal degrees (-180 to 180)
- `measurement_value` - Numeric value
- `measurement_unit` - Unit of measurement (e.g., "ppm", "µg/L", "meters")
- `measurement_date` - ISO 8601 format (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS)

**Optional Columns**:
- `data_type` - water_quality, aquifer_level, stream_flow, precipitation
- `location_name` - Human-readable location name
- `parameter_name` - Parameter being measured (e.g., "arsenic", "pH")
- `depth_meters` - Depth of measurement
- `notes` - Additional notes

### Community Data Format

**Required Columns**:
- `community_name` - Community name
- `latitude` - Decimal degrees
- `longitude` - Decimal degrees
- `population` - Integer > 0

**Optional Columns**:
- `water_access_level` - none, limited, basic, safely_managed
- `primary_water_source` - well, borehole, piped_water, surface_water, rainwater, vendor
- `household_count` - Number of households
- `collection_date` - Date data was collected
- `source` - Data source
- `notes` - Additional notes

### Infrastructure Data Format

**Required Columns**:
- `facility_type` - well, borehole, treatment_plant, reservoir, pump_station, water_tower
- `facility_name` - Facility name
- `latitude` - Decimal degrees
- `longitude` - Decimal degrees
- `operational_status` - operational, non_operational, under_maintenance, planned, abandoned

**Optional Columns**:
- `capacity` - Numeric capacity value
- `capacity_unit` - liters_per_day, liters_per_hour, liters, cubic_meters
- `population_served` - Number of people served
- `installation_date` - Date facility was built
- `last_maintenance_date` - Last maintenance date
- `source` - Data source
- `notes` - Additional notes

## Troubleshooting

### Upload Fails with "Invalid latitude"

**Problem**: Latitude or longitude values are out of range or swapped.

**Solution**:
- Latitude must be -90 to 90
- Longitude must be -180 to 180
- Check if lat/lon are swapped (common mistake)

### Upload Fails with "Missing required column"

**Problem**: CSV is missing a required column.

**Solution**: Ensure your CSV has all required columns. Check spelling and case (should be lowercase).

### Upload Succeeds but 0 records imported

**Problem**: Data validation failed for all rows.

**Solution**: Download the error report CSV to see specific validation errors.

## Creating Your Own Dataset

1. **Use sample files as templates**: Copy and modify sample files
2. **Ensure UTF-8 encoding**: Save CSV files as UTF-8 in Excel/LibreOffice
3. **Check coordinates**: Verify lat/lon are correct and not swapped
4. **Validate dates**: Use ISO 8601 format (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS)
5. **Test with small dataset first**: Upload 5-10 rows, fix errors, then upload full dataset

## WHO Water Quality Guidelines (Reference)

| Parameter | Guideline Value | Health Impact if Exceeded |
|-----------|-----------------|---------------------------|
| Arsenic | 10 µg/L | Cancer, skin lesions, cardiovascular disease |
| Fluoride | 1.5 mg/L | Dental/skeletal fluorosis |
| Nitrate | 50 mg/L | Methemoglobinemia (blue baby syndrome) |
| Lead | 10 µg/L | Neurological damage, especially in children |
| pH | 6.5-8.5 | Corrosion or scale formation (not direct health risk) |
| Turbidity | <5 NTU | Indicates contamination (bacteria, pathogens) |
| TDS | <600 mg/L | Taste issues, not direct health risk |

## License

These sample datasets are provided for testing purposes only. Real-world data should be obtained from authoritative sources with proper attribution.

## Support

For issues with sample data or data upload:
- Check validation errors in upload response
- Review data format specifications above
- Consult docs/data-ingestion/ for detailed guides
- Report bugs at https://github.com/anthropics/wateraccessoptimizer/issues
