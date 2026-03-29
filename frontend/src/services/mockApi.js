const wait = (ms = 150) => new Promise((resolve) => setTimeout(resolve, ms))

const uploads = [
  {
    id: 'upload-hydro-001',
    filename: 'hydro-baseline-2026.csv',
    dataType: 'HYDRO',
    rowCount: 184,
    validationStatus: 'SUCCESS',
    uploadedAt: '2026-03-24T10:30:00Z',
  },
  {
    id: 'upload-community-001',
    filename: 'community-survey-region-a.csv',
    dataType: 'COMMUNITY',
    rowCount: 42,
    validationStatus: 'SUCCESS',
    uploadedAt: '2026-03-24T11:15:00Z',
  },
  {
    id: 'upload-infra-001',
    filename: 'infrastructure-audit-q1.csv',
    dataType: 'INFRASTRUCTURE',
    rowCount: 29,
    validationStatus: 'WARNING',
    uploadedAt: '2026-03-24T11:50:00Z',
  },
]

const assessmentResults = {
  'assessment-001': {
    id: 'assessment-001',
    name: 'Northern Corridor Risk Review',
    description: 'Mock assessment for screenshot and demo workflows.',
    summary: {
      totalRecords: 6,
      highRiskCount: 2,
      mediumRiskCount: 2,
      lowRiskCount: 2,
      avgWaterQuality: 61,
      avgDistance: 48,
      avgReliability: 57,
      avgPopulationDensity: 52,
      avgInfrastructure: 45,
    },
    records: [
      { communityName: 'Makuri East', region: 'Region A', riskLevel: 'HIGH', riskScore: 82, waterQuality: 88, distance: 72, reliability: 61, population: 79, infrastructure: 55 },
      { communityName: 'Riverbend', region: 'Region A', riskLevel: 'HIGH', riskScore: 74, waterQuality: 81, distance: 64, reliability: 52, population: 66, infrastructure: 49 },
      { communityName: 'Lakeside Ward', region: 'Region B', riskLevel: 'MEDIUM', riskScore: 59, waterQuality: 51, distance: 62, reliability: 43, population: 58, infrastructure: 47 },
      { communityName: 'Hill Market', region: 'Region B', riskLevel: 'MEDIUM', riskScore: 48, waterQuality: 42, distance: 55, reliability: 40, population: 43, infrastructure: 38 },
      { communityName: 'West Canal', region: 'Region C', riskLevel: 'LOW', riskScore: 28, waterQuality: 19, distance: 31, reliability: 22, population: 34, infrastructure: 25 },
      { communityName: 'Greenpoint', region: 'Region C', riskLevel: 'LOW', riskScore: 17, waterQuality: 13, distance: 20, reliability: 15, population: 18, infrastructure: 16 },
    ],
  },
}

const assessments = [
  {
    id: 'assessment-001',
    name: 'Northern Corridor Risk Review',
    description: 'Mock assessment for screenshot and demo workflows.',
    status: 'COMPLETED',
    createdAt: '2026-03-25T09:00:00Z',
    completedAt: '2026-03-25T09:02:30Z',
    recordCount: 6,
    isPublic: true,
  },
  {
    id: 'assessment-002',
    name: 'Quarterly Infrastructure Stress Scan',
    description: 'Pending re-run after latest field upload.',
    status: 'PENDING',
    createdAt: '2026-03-26T14:30:00Z',
    recordCount: 12,
    isPublic: false,
  },
]

const communitiesGeoJson = {
  type: 'FeatureCollection',
  features: [
    {
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [36.8219, -1.2921] },
      properties: { id: 'c1', communityName: 'Makuri East', population: 5200, serviceLevel: 'limited' },
    },
    {
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [36.842, -1.286] },
      properties: { id: 'c2', communityName: 'Riverbend', population: 3400, serviceLevel: 'basic' },
    },
    {
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [36.805, -1.31] },
      properties: { id: 'c3', communityName: 'Greenpoint', population: 1800, serviceLevel: 'safely_managed' },
    },
  ],
}

const facilitiesGeoJson = {
  type: 'FeatureCollection',
  features: [
    {
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [36.829, -1.289] },
      properties: { id: 'f1', facilityName: 'Makuri Borehole', facilityType: 'borehole', operationalStatus: 'operational' },
    },
    {
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [36.814, -1.301] },
      properties: { id: 'f2', facilityName: 'West Canal Pump', facilityType: 'pump_station', operationalStatus: 'under_maintenance' },
    },
  ],
}

const quotaInfo = {
  storageUsedMb: 36.4,
  storageQuotaMb: 100,
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

export async function uploadHydroData(file) {
  await wait()
  uploads.unshift({
    id: `upload-hydro-${Date.now()}`,
    filename: file?.name || 'mock-hydro.csv',
    dataType: 'HYDRO',
    rowCount: 120,
    validationStatus: 'SUCCESS',
    uploadedAt: new Date().toISOString(),
  })
  return {
    status: 'SUCCESS',
    rowsProcessed: 120,
    errorCount: 0,
    warningCount: 0,
  }
}

export async function uploadCommunityData(file) {
  await wait()
  uploads.unshift({
    id: `upload-community-${Date.now()}`,
    filename: file?.name || 'mock-community.csv',
    dataType: 'COMMUNITY',
    rowCount: 36,
    validationStatus: 'SUCCESS',
    uploadedAt: new Date().toISOString(),
  })
  return {
    status: 'SUCCESS',
    rowsProcessed: 36,
    errorCount: 0,
    warningCount: 1,
    warnings: [{ message: '1 row used a fallback source label.' }],
  }
}

export async function uploadInfrastructureData(file) {
  await wait()
  uploads.unshift({
    id: `upload-infrastructure-${Date.now()}`,
    filename: file?.name || 'mock-infrastructure.csv',
    dataType: 'INFRASTRUCTURE',
    rowCount: 18,
    validationStatus: 'WARNING',
    uploadedAt: new Date().toISOString(),
  })
  return {
    status: 'WARNING',
    rowsProcessed: 18,
    errorCount: 0,
    warningCount: 2,
    warnings: [
      { message: '2 facilities are marked as under maintenance.' },
    ],
  }
}

export async function getUploads() {
  await wait()
  return { uploads: clone(uploads) }
}

export async function deleteUpload(uploadId) {
  await wait()
  const index = uploads.findIndex((upload) => upload.id === uploadId)
  if (index >= 0) {
    uploads.splice(index, 1)
  }
  return { status: 'success' }
}

export async function getQuotaInfo() {
  await wait()
  return clone(quotaInfo)
}

export async function createAssessment(name, description, isPublic = false) {
  await wait()
  const assessment = {
    id: `assessment-${Date.now()}`,
    name,
    description,
    status: 'PENDING',
    createdAt: new Date().toISOString(),
    recordCount: 0,
    isPublic,
  }
  assessments.unshift(assessment)
  return clone(assessment)
}

export async function getAssessments() {
  await wait()
  return clone(assessments)
}

export async function getPublicAssessments() {
  await wait()
  return clone(assessments.filter((assessment) => assessment.isPublic))
}

export async function getAssessment(assessmentId) {
  await wait()
  return clone(assessments.find((assessment) => assessment.id === assessmentId) || null)
}

export async function getAssessmentResults(assessmentId, riskLevel = null) {
  await wait()
  const result = clone(assessmentResults[assessmentId] || assessmentResults['assessment-001'])
  if (riskLevel) {
    result.records = result.records.filter((record) => record.riskLevel === riskLevel)
  }
  return result
}

export async function getAssessmentSummary(assessmentId) {
  await wait()
  return clone((assessmentResults[assessmentId] || assessmentResults['assessment-001']).summary)
}

export async function deleteAssessment(assessmentId) {
  await wait()
  const index = assessments.findIndex((assessment) => assessment.id === assessmentId)
  if (index >= 0) {
    assessments.splice(index, 1)
  }
  return { status: 'success' }
}

export async function runAssessment(assessmentId) {
  await wait(350)
  const assessment = assessments.find((item) => item.id === assessmentId)
  if (assessment) {
    assessment.status = 'COMPLETED'
    assessment.completedAt = new Date().toISOString()
    assessment.recordCount = 6
  }
  assessmentResults[assessmentId] = clone(assessmentResults['assessment-001'])
  assessmentResults[assessmentId].id = assessmentId
  assessmentResults[assessmentId].name = assessment?.name || 'Generated Assessment'
  return {
    status: 'COMPLETED',
    summary: clone(assessmentResults[assessmentId].summary),
  }
}

export async function getCommunitiesNearby() {
  await wait()
  return clone(communitiesGeoJson)
}

export async function getFacilitiesNearby() {
  await wait()
  return clone(facilitiesGeoJson)
}

export async function getMeasurementsNearby() {
  await wait()
  return {
    type: 'FeatureCollection',
    features: [],
  }
}

export async function getAllCommunities() {
  await wait()
  return clone(communitiesGeoJson)
}

export async function getAllFacilities() {
  await wait()
  return clone(facilitiesGeoJson)
}

export async function exportToExcel() {
  await wait()
  return new Blob(['Mock Excel export'], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
}

export async function exportToPDF() {
  await wait()
  return new Blob(['Mock PDF export'], { type: 'application/pdf' })
}

export async function checkHealth() {
  await wait()
  return { status: 'ok', mode: 'mock' }
}
