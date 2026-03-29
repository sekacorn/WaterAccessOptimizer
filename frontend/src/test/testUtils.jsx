/**
 * Test Utilities
 * Helper functions and custom render for testing React components
 */

import { render } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { vi } from 'vitest'

/**
 * Custom render function that wraps components with necessary providers
 */
export function renderWithRouter(ui, options = {}) {
  const {
    route = '/',
    ...renderOptions
  } = options

  window.history.pushState({}, 'Test page', route)

  return render(
    <BrowserRouter
      future={{
        v7_relativeSplatPath: true,
        v7_startTransition: true,
      }}
    >
      {ui}
    </BrowserRouter>,
    renderOptions
  )
}

/**
 * Mock Zustand store with common test state
 */
export const createMockStore = (overrides = {}) => ({
  // Auth
  user: null,
  token: null,
  isAuthenticated: false,
  setUser: vi.fn(),
  setToken: vi.fn(),
  logout: vi.fn(),

  // UI
  sidebarOpen: true,
  toggleSidebar: vi.fn(),
  mapCenter: [36.8219, -1.2921],
  mapZoom: 10,

  // Data
  uploads: [],
  setUploads: vi.fn(),
  addUpload: vi.fn(),
  assessments: [],
  setAssessments: vi.fn(),
  currentAssessment: null,
  setCurrentAssessment: vi.fn(),
  quotaInfo: null,
  setQuotaInfo: vi.fn(),

  // Notifications
  notifications: [],
  addNotification: vi.fn(),
  removeNotification: vi.fn(),

  ...overrides
})

/**
 * Mock API responses
 */
export const mockApiResponses = {
  uploads: {
    uploads: [
      {
        id: 1,
        filename: 'test-hydro.csv',
        dataType: 'HYDRO',
        rowCount: 100,
        validationStatus: 'SUCCESS',
        uploadedAt: '2026-02-04T10:00:00Z'
      },
      {
        id: 2,
        filename: 'test-community.csv',
        dataType: 'COMMUNITY',
        rowCount: 50,
        validationStatus: 'WARNING',
        uploadedAt: '2026-02-04T11:00:00Z'
      }
    ],
    totalPages: 1
  },

  assessments: [
    {
      id: 1,
      name: 'Test Assessment 1',
      description: 'Test description',
      status: 'COMPLETED',
      createdAt: '2026-02-04T09:00:00Z',
      completedAt: '2026-02-04T09:30:00Z',
      recordCount: 100,
      isPublic: false
    },
    {
      id: 2,
      name: 'Test Assessment 2',
      description: null,
      status: 'PENDING',
      createdAt: '2026-02-04T10:00:00Z',
      completedAt: null,
      recordCount: 0,
      isPublic: true
    }
  ],

  assessmentResults: {
    id: 1,
    name: 'Test Assessment 1',
    description: 'Test description',
    status: 'COMPLETED',
    createdAt: '2026-02-04T09:00:00Z',
    completedAt: '2026-02-04T09:30:00Z',
    isPublic: false,
    summary: {
      totalRecords: 100,
      highRiskCount: 20,
      mediumRiskCount: 50,
      lowRiskCount: 30,
      avgWaterQuality: 75,
      avgDistance: 60,
      avgReliability: 80,
      avgPopulationDensity: 70,
      avgInfrastructure: 65
    },
    records: [
      {
        locationName: 'Location 1',
        region: 'Region A',
        riskLevel: 'HIGH',
        riskScore: 85.5,
        waterQualityScore: 90,
        distanceScore: 85,
        reliabilityScore: 80,
        populationDensityScore: 90,
        infrastructureScore: 75
      },
      {
        locationName: 'Location 2',
        region: 'Region B',
        riskLevel: 'LOW',
        riskScore: 35.2,
        waterQualityScore: 40,
        distanceScore: 30,
        reliabilityScore: 35,
        populationDensityScore: 40,
        infrastructureScore: 30
      }
    ]
  },

  communities: {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        geometry: {
          type: 'Point',
          coordinates: [-1.2921, 36.8219]
        },
        properties: {
          communityName: 'Nairobi Community',
          population: 10000,
          serviceLevel: 'BASIC'
        }
      }
    ]
  },

  facilities: {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        geometry: {
          type: 'Point',
          coordinates: [-1.2821, 36.8319]
        },
        properties: {
          facilityName: 'Water Treatment Plant',
          facilityType: 'TREATMENT',
          operationalStatus: 'OPERATIONAL'
        }
      }
    ]
  },

  quota: {
    storageUsedMb: 150,
    storageQuotaMb: 1000,
    uploadsUsed: 25,
    uploadsQuota: 100,
    assessmentsUsed: 10,
    assessmentsQuota: 50
  }
}

/**
 * Wait for async updates (useful for async state updates)
 */
export const waitForAsync = () => new Promise(resolve => setTimeout(resolve, 0))

/**
 * Mock file creation helper
 */
export const createMockFile = (filename = 'test.csv', content = 'test,data\n1,2', type = 'text/csv') => {
  return new File([content], filename, { type })
}

/**
 * Mock Chart.js for component tests
 */
export const mockChartJS = () => {
  vi.mock('react-chartjs-2', () => ({
    Pie: vi.fn(() => null),
    Bar: vi.fn(() => null),
  }))
  vi.mock('chart.js', () => ({
    Chart: vi.fn(),
    ArcElement: vi.fn(),
    CategoryScale: vi.fn(),
    LinearScale: vi.fn(),
    BarElement: vi.fn(),
    Title: vi.fn(),
    Tooltip: vi.fn(),
    Legend: vi.fn(),
    register: vi.fn()
  }))
}

/**
 * Mock Leaflet for map component tests
 */
export const mockLeaflet = () => {
  vi.mock('react-leaflet', () => ({
    MapContainer: vi.fn(({ children }) => <div data-testid="map-container">{children}</div>),
    TileLayer: vi.fn(() => null),
    Marker: vi.fn(() => null),
    Popup: vi.fn(({ children }) => <div>{children}</div>),
    CircleMarker: vi.fn(() => null),
  }))
  vi.mock('leaflet', () => ({
    default: {
      Icon: {
        Default: {
          prototype: { _getIconUrl: null },
          mergeOptions: vi.fn()
        }
      }
    }
  }))
}
