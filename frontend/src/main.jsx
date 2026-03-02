/**
 * Main entry point for WaterAccessOptimizer React application
 *
 * This application provides:
 * - 3D hydrological visualizations using Three.js
 * - Water management recommendations using AI
 * - Real-time collaboration via WebSocket
 * - MBTI-tailored user experiences
 */

import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'

// Render the React application
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
