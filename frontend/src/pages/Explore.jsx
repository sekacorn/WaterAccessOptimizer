/**
 * Explore Page with 3D Water Visualizations
 *
 * Features:
 * - Interactive 3D hydrological maps using Three.js
 * - Water quality zones visualization
 * - Aquifer level displays
 * - Export functionality (PNG, SVG, STL)
 * - MBTI-tailored visual styles
 */

import React, { useState, useEffect } from 'react'
import { Canvas } from '@react-three/fiber'
import { OrbitControls, PerspectiveCamera, Environment } from '@react-three/drei'
import { Download, Layers, ZoomIn, ZoomOut } from 'lucide-react'
import { getHydroData, getCommunityData, getInfrastructureData } from '../services/api'
import './Explore.css'

/**
 * 3D Water Point Component
 * Renders individual water data points in 3D space
 */
const WaterPoint = ({ position, color, size, data }) => {
  const [hovered, setHovered] = useState(false)

  return (
    <mesh
      position={position}
      onPointerOver={() => setHovered(true)}
      onPointerOut={() => setHovered(false)}
      scale={hovered ? size * 1.5 : size}
    >
      {/* Sphere geometry for water points */}
      <sphereGeometry args={[0.1, 16, 16]} />
      {/* Material with color based on water quality */}
      <meshStandardMaterial
        color={color}
        emissive={hovered ? color : '#000000'}
        emissiveIntensity={hovered ? 0.5 : 0}
        metalness={0.3}
        roughness={0.7}
      />
      {hovered && (
        // Tooltip information when hovered
        <Html distanceFactor={10}>
          <div className="tooltip-3d">
            <p><strong>{data.locationName}</strong></p>
            <p>Value: {data.measurementValue} {data.measurementUnit}</p>
            <p>Type: {data.dataType}</p>
          </div>
        </Html>
      )}
    </mesh>
  )
}

/**
 * 3D Terrain/Water Layer Component
 */
const WaterLayer = ({ elevation }) => {
  return (
    <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, elevation, 0]}>
      {/* Plane geometry for water surface */}
      <planeGeometry args={[50, 50, 32, 32]} />
      {/* Water-like material */}
      <meshStandardMaterial
        color="#2196F3"
        transparent
        opacity={0.6}
        metalness={0.9}
        roughness={0.1}
      />
    </mesh>
  )
}

/**
 * Main 3D Scene Component
 */
const WaterScene = ({ hydroData, mbtiType }) => {
  // MBTI-specific color schemes for visualization
  const mbtiColorSchemes = {
    ENTJ: { primary: '#FF6B35', secondary: '#004E89' }, // Strategic bold colors
    INFP: { primary: '#9B59B6', secondary: '#3498DB' }, // Creative, artistic colors
    INFJ: { primary: '#16A085', secondary: '#8E44AD' }, // Intuitive, deep colors
    ESTP: { primary: '#E74C3C', secondary: '#F39C12' }, // Energetic, action colors
    INTJ: { primary: '#2C3E50', secondary: '#7F8C8D' }, // Analytical, structured colors
    INTP: { primary: '#34495E', secondary: '#95A5A6' }, // Logical, neutral colors
    ISTJ: { primary: '#2980B9', secondary: '#27AE60' }, // Organized, reliable colors
    ESFJ: { primary: '#E67E22', secondary: '#1ABC9C' }, // Warm, supportive colors
    ISFP: { primary: '#F39C12', secondary: '#9B59B6' }, // Creative, sensory colors
    ENTP: { primary: '#E74C3C', secondary: '#3498DB' }, // Innovative, dynamic colors
    ISFJ: { primary: '#16A085', secondary: '#D35400' }, // Nurturing, stable colors
    ESFP: { primary: '#F39C12', secondary: '#E74C3C' }, // Vibrant, energetic colors
    ENFJ: { primary: '#9B59B6', secondary: '#E74C3C' }, // Inspirational, warm colors
    ESTJ: { primary: '#2980B9', secondary: '#C0392B' }, // Authoritative, strong colors
    ISTP: { primary: '#7F8C8D', secondary: '#2ECC71' }  // Practical, hands-on colors
  }

  const colorScheme = mbtiColorSchemes[mbtiType] || mbtiColorSchemes.ENTJ

  // Function to determine point color based on water quality
  const getPointColor = (value) => {
    if (value > 80) return colorScheme.primary  // Good quality
    if (value > 50) return '#FFA500'            // Medium quality
    return '#FF0000'                            // Poor quality
  }

  return (
    <>
      {/* Camera setup */}
      <PerspectiveCamera makeDefault position={[10, 10, 10]} />

      {/* Lighting */}
      <ambientLight intensity={0.5} />
      <directionalLight position={[10, 10, 5]} intensity={1} />
      <pointLight position={[-10, -10, -5]} intensity={0.5} />

      {/* Environment for realistic reflections */}
      <Environment preset="sunset" />

      {/* Water layer */}
      <WaterLayer elevation={0} />

      {/* Render water data points */}
      {hydroData.map((point, index) => {
        // Convert lat/long to 3D coordinates (simplified projection)
        const x = (point.latitude - 0) * 0.1
        const z = (point.longitude - 0) * 0.1
        const y = (point.measurementValue || 0) * 0.05

        return (
          <WaterPoint
            key={index}
            position={[x, y, z]}
            color={getPointColor(point.measurementValue || 50)}
            size={1}
            data={point}
          />
        )
      })}

      {/* Grid helper for reference */}
      <gridHelper args={[50, 50, '#888888', '#444444']} />

      {/* Orbit controls for user interaction */}
      <OrbitControls
        enableDamping
        dampingFactor={0.05}
        minDistance={5}
        maxDistance={50}
      />
    </>
  )
}

/**
 * Main Explore Page Component
 */
const Explore = ({ mbtiType }) => {
  const [hydroData, setHydroData] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [visualizationType, setVisualizationType] = useState('hydro')

  // Load water data on component mount
  useEffect(() => {
    loadData()
  }, [visualizationType])

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)

      // Fetch data based on visualization type
      let data
      if (visualizationType === 'hydro') {
        data = await getHydroData()
      } else if (visualizationType === 'community') {
        data = await getCommunityData()
      } else {
        data = await getInfrastructureData()
      }

      setHydroData(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Error loading data:', err)
      setError('Failed to load visualization data. Please upload data first.')
      setHydroData([])
    } finally {
      setLoading(false)
    }
  }

  const handleExport = (format) => {
    // Export functionality would use the canvas API to export
    console.log(`Exporting as ${format}`)
    alert(`Export as ${format} - Feature coming soon!`)
  }

  return (
    <div className="explore-page">
      <div className="container">
        <h1>3D Water Visualization</h1>
        <p className="page-subtitle">
          Interactive hydrological maps tailored for {mbtiType} personality type
        </p>

        {/* Controls */}
        <div className="visualization-controls card">
          <div className="control-group">
            <label>Visualization Type:</label>
            <select
              value={visualizationType}
              onChange={(e) => setVisualizationType(e.target.value)}
              className="select-input"
            >
              <option value="hydro">Hydrological Data</option>
              <option value="community">Community Access</option>
              <option value="infrastructure">Infrastructure</option>
            </select>
          </div>

          <div className="export-buttons">
            <button
              onClick={() => handleExport('png')}
              className="button button-sm"
            >
              <Download size={16} /> PNG
            </button>
            <button
              onClick={() => handleExport('svg')}
              className="button button-sm"
            >
              <Download size={16} /> SVG
            </button>
            <button
              onClick={() => handleExport('stl')}
              className="button button-sm"
            >
              <Download size={16} /> STL
            </button>
          </div>
        </div>

        {/* 3D Canvas */}
        <div className="canvas-container card">
          {loading ? (
            <div className="loading-state">
              <div className="loading"></div>
              <p>Loading 3D visualization...</p>
            </div>
          ) : error ? (
            <div className="error-state">
              <p>{error}</p>
              <p>Go to the Analyze page to upload water data.</p>
            </div>
          ) : (
            <Canvas className="three-canvas">
              <WaterScene hydroData={hydroData} mbtiType={mbtiType} />
            </Canvas>
          )}
        </div>

        {/* Legend */}
        <div className="legend card">
          <h3>Legend</h3>
          <div className="legend-items">
            <div className="legend-item">
              <div className="legend-color" style={{ backgroundColor: '#00FF00' }}></div>
              <span>High Quality Water ({">"} 80%)</span>
            </div>
            <div className="legend-item">
              <div className="legend-color" style={{ backgroundColor: '#FFA500' }}></div>
              <span>Medium Quality Water (50-80%)</span>
            </div>
            <div className="legend-item">
              <div className="legend-color" style={{ backgroundColor: '#FF0000' }}></div>
              <span>Low Quality Water ({"<"} 50%)</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Explore
