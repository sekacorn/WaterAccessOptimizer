/**
 * Main App Component for WaterAccessOptimizer
 *
 * Provides routing and layout structure for the application.
 * Water access risk assessment and data management platform.
 * With lazy loading for optimal performance.
 */

import { Suspense, lazy } from 'react'
import { BrowserRouter as Router, Routes, Route, Link, useNavigate, useLocation } from 'react-router-dom'
import {
  AlertCircle,
  CheckCircle2,
  Droplet,
  FileDown,
  Info,
  LogOut,
  Map,
  Upload,
  User as UserIcon,
  XCircle,
  BarChart3
} from 'lucide-react'

// Lazy load page components for code splitting
const Landing = lazy(() => import('./pages/Landing'))
const Login = lazy(() => import('./pages/Login'))
const Register = lazy(() => import('./pages/Register'))
const Dashboard = lazy(() => import('./pages/Dashboard'))
const DataUpload = lazy(() => import('./pages/DataUpload'))
const MapView = lazy(() => import('./pages/MapView'))
const RiskAssessment = lazy(() => import('./pages/RiskAssessment'))
const AssessmentResults = lazy(() => import('./pages/AssessmentResults'))
const PrivacyNotice = lazy(() => import('./pages/PrivacyNotice'))
const AccessibilityStatement = lazy(() => import('./pages/AccessibilityStatement'))
const SecurityCompliance = lazy(() => import('./pages/SecurityCompliance'))

// Components (not lazy loaded - small and frequently used)
import ProtectedRoute from './components/ProtectedRoute'

// Store
import useStore from './store/useStore'

// Styles
import './App.css'

// Loading component for Suspense fallback
function PageLoader() {
  return (
    <div className="page-loader">
      <div className="loader-spinner"></div>
      <p>Loading...</p>
    </div>
  )
}

function Navigation() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, isAuthenticated, logout } = useStore()

  // Don't show navigation on login/register/landing pages
  if (location.pathname === '/login' || location.pathname === '/register' || location.pathname === '/') {
    return null
  }

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="app-header">
      <div className="container">
        <nav className="nav">
          <div className="nav-brand">
            <Droplet size={32} className="nav-icon" />
            <h1>WaterAccessOptimizer</h1>
          </div>

          {isAuthenticated && (
            <>
              <ul className="nav-links">
                <li>
                  <Link to="/dashboard" className={location.pathname === '/dashboard' ? 'active' : ''}>
                    <BarChart3 size={18} />
                    Dashboard
                  </Link>
                </li>
                <li>
                  <Link to="/upload" className={location.pathname === '/upload' ? 'active' : ''}>
                    <Upload size={18} />
                    Upload Data
                  </Link>
                </li>
                <li>
                  <Link to="/map" className={location.pathname === '/map' ? 'active' : ''}>
                    <Map size={18} />
                    Map View
                  </Link>
                </li>
                <li>
                  <Link to="/assessment" className={location.pathname.startsWith('/assessment') ? 'active' : ''}>
                    <FileDown size={18} />
                    Risk Assessment
                  </Link>
                </li>
              </ul>

              <div className="nav-actions">
                {/* User Menu */}
                <div className="user-menu">
                  <span className="user-name">
                    <UserIcon size={18} />
                    {user?.email || 'User'}
                  </span>
                  <button onClick={handleLogout} className="logout-btn">
                    <LogOut size={18} />
                    Logout
                  </button>
                </div>
              </div>
            </>
          )}

          {!isAuthenticated && (
            <div className="nav-actions">
              <Link to="/login" className="btn-secondary">Login</Link>
              <Link to="/register" className="btn-primary">Sign Up</Link>
            </div>
          )}
        </nav>
      </div>
    </header>
  )
}

function NotificationRegion() {
  const { notifications, removeNotification } = useStore()

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'success':
        return <CheckCircle2 size={18} aria-hidden="true" />
      case 'error':
        return <XCircle size={18} aria-hidden="true" />
      case 'warning':
        return <AlertCircle size={18} aria-hidden="true" />
      default:
        return <Info size={18} aria-hidden="true" />
    }
  }

  if (notifications.length === 0) {
    return null
  }

  return (
    <div className="notification-region" aria-live="polite" aria-atomic="false">
      {notifications.map((notification) => (
        <div
          key={notification.id}
          className={`notification-toast ${notification.type || 'info'}`}
          role={notification.type === 'error' ? 'alert' : 'status'}
        >
          <div className="notification-content">
            {getNotificationIcon(notification.type)}
            <span>{notification.message}</span>
          </div>
          <button
            type="button"
            className="notification-dismiss"
            aria-label="Dismiss notification"
            onClick={() => removeNotification(notification.id)}
          >
            ×
          </button>
        </div>
      ))}
    </div>
  )
}

function App() {
  return (
    <Router>
      <div className="app">
        <a className="skip-link" href="#main-content">Skip to main content</a>
        <Navigation />
        <NotificationRegion />

        {/* Main Content with Suspense for lazy-loaded routes */}
        <main id="main-content" className="app-main" tabIndex="-1">
          <Suspense fallback={<PageLoader />}>
            <Routes>
              {/* Public routes */}
              <Route path="/" element={<Landing />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/privacy" element={<PrivacyNotice />} />
              <Route path="/accessibility" element={<AccessibilityStatement />} />
              <Route path="/security" element={<SecurityCompliance />} />

              {/* Protected routes - require authentication */}
              <Route path="/dashboard" element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              } />
              <Route path="/upload" element={
                <ProtectedRoute>
                  <DataUpload />
                </ProtectedRoute>
              } />
              <Route path="/map" element={
                <ProtectedRoute>
                  <MapView />
                </ProtectedRoute>
              } />
              <Route path="/assessment" element={
                <ProtectedRoute>
                  <RiskAssessment />
                </ProtectedRoute>
              } />
              <Route path="/assessment/:id" element={
                <ProtectedRoute>
                  <AssessmentResults />
                </ProtectedRoute>
              } />
            </Routes>
          </Suspense>
        </main>

        {/* Footer */}
        <footer className="app-footer">
          <div className="container">
            <p>&copy; 2026 WaterAccessOptimizer - Data-driven water access risk assessment</p>
            <p className="footer-disclaimer">
              Multi-criteria analysis for water security planning
            </p>
            <nav className="footer-links" aria-label="Compliance and policy links">
              <Link to="/privacy">Privacy Notice</Link>
              <Link to="/accessibility">Accessibility Statement</Link>
              <Link to="/security">Security and Compliance</Link>
            </nav>
          </div>
        </footer>
      </div>
    </Router>
  )
}

export default App
