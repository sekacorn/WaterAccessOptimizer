/**
 * WaterAccessOptimizer - TypeScript Type Definitions
 * Version: 1.0.0
 * Last Updated: 2026-02-03
 * Status: MVP (Iteration 7)
 *
 * Reference: Agent 09 (API_CONTRACTS_TYPES.md)
 * Usage: Shared types for frontend (React) and backend (FastAPI with Pydantic)
 *
 * Generation: Auto-generate from OpenAPI specs in future (orval, openapi-typescript)
 * For now: Manual type definitions for MVP
 */

// =============================================================================
// AUTH TYPES
// =============================================================================

export interface RegisterRequest {
  email: string;                    // Format: email, Required
  password: string;                 // Min length: 8, Required
  full_name: string;                // Min: 2, Max: 100, Required
  organization_name?: string;       // Optional
}

export interface LoginRequest {
  email: string;                    // Format: email, Required
  password: string;                 // Required
}

export interface AuthResponse {
  access_token: string;             // JWT token (24h expiry for MVP)
  refresh_token: string;            // Refresh token (7 days, Sprint 2)
  token_type: 'bearer';             // Always 'bearer'
  expires_in: number;               // Seconds until expiration
  user: User;                       // User profile
}

export interface User {
  id: string;                       // UUID
  email: string;                    // User email
  full_name: string;                // Display name
  role: 'USER' | 'ADMIN';          // MVP roles only (MODERATOR removed)
  organization_id: string | null;   // UUID or null
  organization_name: string | null; // Org name or null
  storage_quota_bytes: number;      // Storage quota (100MB MVP)
  storage_used_bytes: number;       // Current usage
  created_at: string;               // ISO 8601 timestamp
}

// =============================================================================
// DATA TYPES
// =============================================================================

// Community Data

export interface Community {
  id: string;                       // UUID
  community_name: string;           // Community name
  latitude: number;                 // -90 to 90
  longitude: number;                // -180 to 180
  population: number;               // > 0
  region: string | null;            // Optional
  district: string | null;          // Optional
  elevation_m: number | null;       // Elevation in meters
  created_at: string;               // ISO 8601
  updated_at: string;               // ISO 8601
}

// Infrastructure Data

export type FacilityType =
  | 'borehole'
  | 'protected_well'
  | 'unprotected_well'
  | 'surface_water'
  | 'piped_network';

export type FunctionalityStatus =
  | 'functional'
  | 'needs_repair'
  | 'non_functional';

export interface Infrastructure {
  id: string;                       // UUID
  facility_name: string;            // Facility name
  facility_type: FacilityType;      // Type of facility
  latitude: number;                 // -90 to 90
  longitude: number;                // -180 to 180
  functionality: FunctionalityStatus; // Operational status
  installation_date: string | null; // ISO 8601 date
  last_maintenance: string | null;  // ISO 8601 date
  capacity_l_per_day: number | null; // Liters per day
  population_served: number | null; // People served
  created_at: string;               // ISO 8601
  updated_at: string;               // ISO 8601
}

// Water Quality Data

export type WaterQualityParameter =
  | 'arsenic'
  | 'fluoride'
  | 'nitrate'
  | 'lead'
  | 'mercury'
  | 'chromium'
  | 'e_coli'
  | 'total_coliform'
  | 'turbidity'
  | 'ph';

export type MeasurementSource =
  | 'field_survey'
  | 'lab_analysis'
  | 'usgs'
  | 'other';

export interface WaterQualityMeasurement {
  id: string;                       // UUID
  location_name: string;            // Measurement location
  latitude: number;                 // -90 to 90
  longitude: number;                // -180 to 180
  measurement_date: string;         // ISO 8601 date
  parameter_name: WaterQualityParameter; // WHO parameter
  measurement_value: number;        // Measured value
  measurement_unit: string;         // Unit (mg/L, CFU/100mL, NTU, pH)
  source: MeasurementSource;        // Data source
  data_quality_score: number;       // 0.0 to 1.0
  created_at: string;               // ISO 8601
}

// Upload Response

export type DatasetType = 'hydro' | 'community' | 'infrastructure';

export type ValidationStatus = 'VALID' | 'WARNINGS' | 'ERRORS';

export interface UploadResponse {
  dataset_id: string;               // UUID of uploaded dataset
  filename: string;                 // Original filename
  dataset_type: DatasetType;        // Dataset type
  records_imported: number;         // Successfully imported records
  records_failed: number;           // Failed records
  validation_status: ValidationStatus; // Overall validation status
  validation_warnings: ValidationIssue[];
  validation_errors: ValidationIssue[];
  upload_size_bytes: number;        // File size
  created_at: string;               // Upload timestamp
}

export type ValidationSeverity = 'ERROR' | 'WARNING';

export interface ValidationIssue {
  row: number;                      // Row number (1-indexed)
  column: string;                   // Column name
  value: string | null;             // Invalid value
  error_code: string;               // Machine-readable code (e.g., INVALID_LATITUDE)
  message: string;                  // Human-readable message
  severity: ValidationSeverity;     // Severity level
}

// Dataset List

export interface Dataset {
  id: string;                       // UUID
  filename: string;                 // Original filename
  dataset_type: DatasetType;        // Dataset type
  records_count: number;            // Total records
  upload_size_bytes: number;        // File size
  validation_status: ValidationStatus;
  uploaded_by: string;              // User ID (UUID)
  created_at: string;               // Upload timestamp
}

// =============================================================================
// RISK ASSESSMENT TYPES
// =============================================================================

export interface AssessmentRequest {
  name: string;                     // Assessment name
  water_quality_dataset_ids: string[]; // UUID array
  community_dataset_ids: string[];  // UUID array
  infrastructure_dataset_ids: string[]; // UUID array
  parameters_to_check?: string[];   // Optional: specific WHO parameters
}

export type AssessmentStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export type ConfidenceLevel = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH';

export interface AssessmentResponse {
  assessment_id: string;            // UUID
  name: string;                     // Assessment name
  status: AssessmentStatus;         // Current status
  algorithm_version: string;        // e.g., "1.0.0"
  created_at: string;               // ISO 8601
  completed_at: string | null;      // ISO 8601 or null
  overall_risk_score: number;       // 0-100 (when completed)
  risk_level: RiskLevel | null;     // When completed
  confidence_level: ConfidenceLevel | null;
  sample_size: number;              // Total data points used
  component_scores: ComponentScores | null;
  top_factors: RiskFactor[] | null; // Top 3 factors
  error_message: string | null;     // Error if failed
}

export interface ComponentScores {
  water_quality: number;            // 0-100
  access_distance: number;          // 0-100
  infrastructure: number;           // 0-100
  population_pressure: number;      // 0-100
}

export type RiskComponent =
  | 'water_quality'
  | 'access_distance'
  | 'infrastructure'
  | 'population_pressure';

export type Severity = 'HIGH' | 'MEDIUM' | 'LOW';

export interface RiskFactor {
  component: RiskComponent;         // Risk component
  measured_value: number;           // Actual measured value
  guideline_value: number | string; // WHO guideline (or range for pH)
  impact_description: string;       // Health impact explanation
  contribution_percent: number;     // % contribution to overall risk
  severity: Severity;               // Severity level
}

// Summary Response

export interface AssessmentSummary {
  full_text: string;                // Full markdown summary
  short_text: string;               // Short summary (<100 words)
  json_summary: JsonSummary;        // Structured JSON
}

export interface JsonSummary {
  overall_risk: {
    score: number;                  // 0-100
    level: RiskLevel;               // Risk level
    description: string;            // Risk level description
  };
  confidence: {
    level: ConfidenceLevel;         // Confidence level
    sample_size: number;            // Data points used
    description: string;            // Confidence description
  };
  top_factors: RiskFactor[];        // Top 3 factors
  component_scores: ComponentScores; // 4 component scores
  metadata: {
    algorithm_version: string;      // e.g., "1.0.0"
    generated_at: string;           // ISO 8601
    sample_size: number;            // Total samples
  };
}

// Assessment List

export interface AssessmentListItem {
  assessment_id: string;            // UUID
  name: string;                     // Assessment name
  status: AssessmentStatus;         // Current status
  overall_risk_score: number | null; // 0-100 or null
  risk_level: RiskLevel | null;     // Risk level or null
  created_at: string;               // ISO 8601
  completed_at: string | null;      // ISO 8601 or null
}

// =============================================================================
// MAP / SPATIAL TYPES
// =============================================================================

// GeoJSON Types (aligned with RFC 7946)

export interface GeoJSONFeatureCollection {
  type: 'FeatureCollection';
  features: GeoJSONFeature[];
}

export interface GeoJSONFeature {
  type: 'Feature';
  id?: string | number;             // Optional feature ID
  geometry: GeoJSONGeometry;        // Geometry object
  properties: GeoJSONProperties;    // Properties object
}

export type GeoJSONGeometry =
  | GeoJSONPoint
  | GeoJSONLineString
  | GeoJSONPolygon
  | GeoJSONMultiPoint
  | GeoJSONMultiLineString
  | GeoJSONMultiPolygon;

export interface GeoJSONPoint {
  type: 'Point';
  coordinates: [number, number];    // [longitude, latitude]
}

export interface GeoJSONLineString {
  type: 'LineString';
  coordinates: [number, number][];  // Array of [lon, lat]
}

export interface GeoJSONPolygon {
  type: 'Polygon';
  coordinates: [number, number][][]; // Array of rings
}

export interface GeoJSONMultiPoint {
  type: 'MultiPoint';
  coordinates: [number, number][];
}

export interface GeoJSONMultiLineString {
  type: 'MultiLineString';
  coordinates: [number, number][][];
}

export interface GeoJSONMultiPolygon {
  type: 'MultiPolygon';
  coordinates: [number, number][][][];
}

export interface GeoJSONProperties {
  id: string;                       // Entity UUID
  type: 'community' | 'infrastructure' | 'measurement';
  name: string;                     // Display name
  [key: string]: any;               // Additional properties
}

// Nearby Communities Query

export interface NearbyCommunitiesQuery {
  latitude: number;                 // Center point latitude
  longitude: number;                // Center point longitude
  radius_km: number;                // Radius in kilometers (max 100)
}

// Bounding Box Query

export interface BoundingBoxQuery {
  min_lat: number;                  // Southwest latitude
  min_lon: number;                  // Southwest longitude
  max_lat: number;                  // Northeast latitude
  max_lon: number;                  // Northeast longitude
}

// Risk Heatmap Data

export interface RiskHeatmapData {
  type: 'FeatureCollection';
  features: RiskHeatmapFeature[];
}

export interface RiskHeatmapFeature {
  type: 'Feature';
  geometry: {
    type: 'Point';
    coordinates: [number, number]; // [longitude, latitude]
  };
  properties: {
    community_id: string;           // UUID
    community_name: string;         // Name
    risk_score: number;             // 0-100
    risk_level: RiskLevel;          // Risk level
    intensity: number;              // 0-1 (for heatmap rendering)
  };
}

// =============================================================================
// ERROR TYPES
// =============================================================================

export interface ErrorResponse {
  error: string;                    // Machine-readable error code
  message: string;                  // Human-readable message
  details?: any;                    // Additional details (optional)
  timestamp: string;                // ISO 8601
  request_id?: string;              // Request ID for debugging
}

export interface ValidationErrorResponse extends ErrorResponse {
  validation_errors: ValidationIssue[];
}

// Error Codes Enum

export enum ErrorCode {
  INVALID_REQUEST = 'INVALID_REQUEST',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  UNAUTHORIZED = 'UNAUTHORIZED',
  INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',
  FORBIDDEN = 'FORBIDDEN',
  NOT_FOUND = 'NOT_FOUND',
  CONFLICT = 'CONFLICT',
  PAYLOAD_TOO_LARGE = 'PAYLOAD_TOO_LARGE',
  RATE_LIMIT_EXCEEDED = 'RATE_LIMIT_EXCEEDED',
  INTERNAL_SERVER_ERROR = 'INTERNAL_SERVER_ERROR'
}

// =============================================================================
// PAGINATION TYPES
// =============================================================================

export interface Pagination {
  page: number;                     // Current page (1-indexed)
  limit: number;                    // Items per page
  total: number;                    // Total items
  pages: number;                    // Total pages
}

export interface PaginatedResponse<T> {
  data: T[];                        // Array of items
  pagination: Pagination;           // Pagination metadata
}

// =============================================================================
// WHO GUIDELINES TYPES (from Iteration 5)
// =============================================================================

export interface WHOGuideline {
  unit: string;                     // Unit of measurement
  guideline_value: number | [number, number]; // Single value or range (pH)
  health_impact: string;            // Health impact description
  severity: Severity;               // HIGH, MEDIUM, LOW
}

export interface WHOGuidelinesMap {
  [parameter: string]: WHOGuideline;
}

// Example WHO Guidelines (from ai-model/risk_scoring.py)
export const WHO_GUIDELINES: WHOGuidelinesMap = {
  arsenic: {
    unit: 'mg/L',
    guideline_value: 0.01,
    health_impact: 'Chronic exposure causes cancer, skin lesions, cardiovascular disease',
    severity: 'HIGH'
  },
  fluoride: {
    unit: 'mg/L',
    guideline_value: 1.5,
    health_impact: 'Excess causes dental and skeletal fluorosis',
    severity: 'MEDIUM'
  },
  nitrate: {
    unit: 'mg/L',
    guideline_value: 50.0,
    health_impact: 'Causes methemoglobinemia (blue baby syndrome) in infants',
    severity: 'HIGH'
  },
  lead: {
    unit: 'mg/L',
    guideline_value: 0.01,
    health_impact: 'Neurotoxic, affects brain development in children',
    severity: 'HIGH'
  },
  mercury: {
    unit: 'mg/L',
    guideline_value: 0.006,
    health_impact: 'Damages nervous system, kidneys',
    severity: 'HIGH'
  },
  chromium: {
    unit: 'mg/L',
    guideline_value: 0.05,
    health_impact: 'Hexavalent form is carcinogenic',
    severity: 'MEDIUM'
  },
  e_coli: {
    unit: 'CFU/100mL',
    guideline_value: 0,
    health_impact: 'Indicates fecal contamination, causes diarrheal disease',
    severity: 'HIGH'
  },
  total_coliform: {
    unit: 'CFU/100mL',
    guideline_value: 0,
    health_impact: 'Indicates potential pathogen presence',
    severity: 'MEDIUM'
  },
  turbidity: {
    unit: 'NTU',
    guideline_value: 5.0,
    health_impact: 'High turbidity shields pathogens from disinfection',
    severity: 'MEDIUM'
  },
  ph: {
    unit: 'pH units',
    guideline_value: [6.5, 8.5],  // Range
    health_impact: 'Extreme pH affects taste and pipe corrosion',
    severity: 'LOW'
  }
};

// =============================================================================
// UTILITY TYPES
// =============================================================================

// Make all properties optional
export type Partial<T> = {
  [P in keyof T]?: T[P];
};

// Make all properties required
export type Required<T> = {
  [P in keyof T]-?: T[P];
};

// Extract specific properties
export type Pick<T, K extends keyof T> = {
  [P in K]: T[P];
};

// Exclude specific properties
export type Omit<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;

// =============================================================================
// FORM TYPES (for React Hook Form)
// =============================================================================

// Login Form
export type LoginFormData = LoginRequest;

// Registration Form
export type RegisterFormData = RegisterRequest;

// Upload Form
export interface UploadFormData {
  file: File;
  dataset_type: DatasetType;
}

// Assessment Form
export interface AssessmentFormData {
  name: string;
  water_quality_dataset_ids: string[];
  community_dataset_ids: string[];
  infrastructure_dataset_ids: string[];
  parameters_to_check: string[];
}

// =============================================================================
// REACT QUERY KEYS (for cache management)
// =============================================================================

export const QueryKeys = {
  // Auth
  currentUser: ['auth', 'me'] as const,

  // Datasets
  datasets: ['datasets'] as const,
  dataset: (id: string) => ['datasets', id] as const,

  // Assessments
  assessments: ['assessments'] as const,
  assessment: (id: string) => ['assessments', id] as const,
  assessmentSummary: (id: string) => ['assessments', id, 'summary'] as const,

  // Map
  communities: ['map', 'communities'] as const,
  infrastructure: ['map', 'infrastructure'] as const,
  riskHeatmap: ['map', 'risk-heatmap'] as const,
  nearbyCommunities: (lat: number, lon: number, radius: number) =>
    ['map', 'communities', 'nearby', lat, lon, radius] as const,
} as const;

// =============================================================================
// API CLIENT TYPES
// =============================================================================

export interface APIClient {
  // Auth
  register: (data: RegisterRequest) => Promise<AuthResponse>;
  login: (data: LoginRequest) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  getCurrentUser: () => Promise<User>;

  // Data Upload
  uploadCommunityData: (file: File) => Promise<UploadResponse>;
  uploadHydroData: (file: File) => Promise<UploadResponse>;
  uploadInfrastructureData: (file: File) => Promise<UploadResponse>;

  // Datasets
  listDatasets: () => Promise<Dataset[]>;
  getDataset: (id: string) => Promise<Dataset>;
  deleteDataset: (id: string) => Promise<void>;

  // Risk Assessment
  createAssessment: (data: AssessmentRequest) => Promise<AssessmentResponse>;
  listAssessments: () => Promise<AssessmentListItem[]>;
  getAssessment: (id: string) => Promise<AssessmentResponse>;
  getAssessmentSummary: (id: string, format?: 'json' | 'markdown' | 'short') => Promise<AssessmentSummary>;

  // Map
  getCommunities: (bbox?: BoundingBoxQuery) => Promise<GeoJSONFeatureCollection>;
  getNearbyCommunities: (query: NearbyCommunitiesQuery) => Promise<GeoJSONFeatureCollection>;
  getInfrastructure: (bbox?: BoundingBoxQuery) => Promise<GeoJSONFeatureCollection>;
  getRiskHeatmap: (bbox?: BoundingBoxQuery) => Promise<RiskHeatmapData>;
  exportGeoJSON: (bbox: BoundingBoxQuery) => Promise<GeoJSONFeatureCollection>;
}

// =============================================================================
// ZUSTAND STORE TYPES (Client State Management)
// =============================================================================

export interface AuthStore {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  setUser: (user: User) => void;
  setToken: (token: string) => void;
  logout: () => void;
}

export interface MapStore {
  selectedCommunity: Community | null;
  selectedInfrastructure: Infrastructure | null;
  mapBounds: BoundingBoxQuery | null;
  setSelectedCommunity: (community: Community | null) => void;
  setSelectedInfrastructure: (infrastructure: Infrastructure | null) => void;
  setMapBounds: (bounds: BoundingBoxQuery) => void;
}

// =============================================================================
// EXPORT ALL TYPES
// =============================================================================

export default {
  // Export all for convenience
};
