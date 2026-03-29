/**
 * Main entry point for WaterAccessOptimizer React application
 *
 * This application provides:
 * - 3D hydrological visualizations using Three.js
 * - Water management recommendations using AI
 * - Real-time collaboration via WebSocket
 * - MBTI-tailored user experiences
 */

import ReactDOM from 'react-dom/client'
import { StrictMode } from 'react'
import App from './App.jsx'
import './index.css'
import { initializeDemoSession } from './utils/demoSession'

initializeDemoSession()

// Render the React application
ReactDOM.createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
