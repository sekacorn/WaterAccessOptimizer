/**
 * Map View Page
 * Interactive map with Leaflet showing communities and facilities
 */

import React, { useState, useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Popup, CircleMarker } from 'react-leaflet'
import { Map as MapIcon, Layers } from 'lucide-react'
import { getAllCommunities, getAllFacilities } from '../services/api'
import useStore from '../store/useStore'
import 'leaflet/dist/leaflet.css'

// Fix Leaflet default icon issue
import L from 'leaflet'
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

function MapView() {
  const { mapCenter, mapZoom } = useStore()
  const [communities, setCommunities] = useState(null)
  const [facilities, setFacilities] = useState(null)
  const [showCommunities, setShowCommunities] = useState(true)
  const [showFacilities, setShowFacilities] = useState(true)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadMapData()
  }, [])

  const loadMapData = async () => {
    try {
      const [communitiesData, facilitiesData] = await Promise.all([
        getAllCommunities(),
        getAllFacilities()
      ])

      setCommunities(communitiesData)
      setFacilities(facilitiesData)
    } catch (error) {
      console.error('Failed to load map data:', error)
    } finally {
      setLoading(false)
    }
  }

  const getRiskColor = (riskLevel) => {
    switch (riskLevel) {
      case 'HIGH': return '#e74c3c'
      case 'MEDIUM': return '#f39c12'
      case 'LOW': return '#27ae60'
      default: return '#3498db'
    }
  }

  if (loading) {
    return (
      <div className="page">
        <div className="container">
          <h1><MapIcon size={32} /> Map View</h1>
          <p>Loading map data...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="page map-page">
      <div className="container-fluid">
        <div className="map-header">
          <h1><MapIcon size={32} /> Map View</h1>
          <div className="map-controls">
            <label>
              <input
                type="checkbox"
                checked={showCommunities}
                onChange={(e) => setShowCommunities(e.target.checked)}
              />
              Communities
            </label>
            <label>
              <input
                type="checkbox"
                checked={showFacilities}
                onChange={(e) => setShowFacilities(e.target.checked)}
              />
              Facilities
            </label>
          </div>
        </div>

        <div className="map-container">
          <MapContainer
            center={mapCenter}
            zoom={mapZoom}
            style={{ height: '600px', width: '100%' }}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {/* Communities */}
            {showCommunities && communities && communities.features && communities.features.map((feature, idx) => {
              const [lng, lat] = feature.geometry.coordinates
              const props = feature.properties

              return (
                <CircleMarker
                  key={`community-${idx}`}
                  center={[lat, lng]}
                  radius={8}
                  fillColor="#3498db"
                  color="#fff"
                  weight={2}
                  fillOpacity={0.7}
                >
                  <Popup>
                    <strong>{props.communityName || 'Community'}</strong><br />
                    Population: {props.population || 'N/A'}<br />
                    Service Level: {props.serviceLevel || 'N/A'}
                  </Popup>
                </CircleMarker>
              )
            })}

            {/* Facilities */}
            {showFacilities && facilities && facilities.features && facilities.features.map((feature, idx) => {
              const [lng, lat] = feature.geometry.coordinates
              const props = feature.properties

              return (
                <Marker key={`facility-${idx}`} position={[lat, lng]}>
                  <Popup>
                    <strong>{props.facilityName || 'Facility'}</strong><br />
                    Type: {props.facilityType || 'N/A'}<br />
                    Status: {props.operationalStatus || 'N/A'}
                  </Popup>
                </Marker>
              )
            })}
          </MapContainer>
        </div>

        <div className="map-legend">
          <h3><Layers size={18} /> Legend</h3>
          <div className="legend-item">
            <div className="legend-marker circle" style={{ backgroundColor: '#3498db' }}></div>
            <span>Communities</span>
          </div>
          <div className="legend-item">
            <div className="legend-marker pin"></div>
            <span>Water Facilities</span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default MapView
