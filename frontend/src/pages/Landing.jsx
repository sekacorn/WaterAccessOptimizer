import { Link } from 'react-router-dom'
import { Droplet, Globe, BarChart3, Users, Shield, Zap, TrendingUp, Map, Award } from 'lucide-react'
import './Landing.css'

/**
 * Landing Page Component
 *
 * Public landing page for WaterAccessOptimizer.
 * Highlights the current MVP workflow and demo-ready experience.
 */
function Landing() {
  return (
    <div className="landing">
      <section className="hero">
        <div className="hero-content">
          <div className="hero-badge">
            <Droplet size={24} />
            <span>Water access planning with practical analytics</span>
          </div>

          <h1 className="hero-title">
            Optimize Water Access with
            <span className="gradient-text"> Data-Driven Insights</span>
          </h1>

          <p className="hero-subtitle">
            Upload datasets, visualize communities and facilities on an interactive map, and run
            explainable risk assessments from one web application.
          </p>

          <div className="hero-cta">
            <Link to="/register" className="btn btn-primary-large">
              Create Account
            </Link>
            <Link to="/login" className="btn btn-secondary-large">
              Sign In
            </Link>
          </div>

          <div className="hero-stats">
            <div className="stat">
              <div className="stat-value">CSV</div>
              <div className="stat-label">Hydro, community, and infrastructure uploads</div>
            </div>
            <div className="stat">
              <div className="stat-value">Map</div>
              <div className="stat-label">Interactive facility and community layers</div>
            </div>
            <div className="stat">
              <div className="stat-value">Risk</div>
              <div className="stat-label">Assessment results and downloadable exports</div>
            </div>
          </div>
        </div>
      </section>

      <section className="features">
        <div className="container">
          <div className="section-header">
            <h2>Current Release Features</h2>
            <p>The shipped app is focused on a clear MVP workflow that is easy to demo and extend.</p>
          </div>

          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon">
                <Globe size={32} />
              </div>
              <h3>Interactive Mapping</h3>
              <p>
                Explore uploaded communities and facilities on a Leaflet-powered map with
                popups and layer toggles.
              </p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <BarChart3 size={32} />
              </div>
              <h3>Risk Assessment</h3>
              <p>
                Create assessments, review score breakdowns, and inspect filtered result
                tables with chart summaries.
              </p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <Zap size={32} />
              </div>
              <h3>Demo Mode</h3>
              <p>
                Run the frontend with seeded mock data for screenshots, walkthroughs, and
                release previews without logging in.
              </p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <Users size={32} />
              </div>
              <h3>Dashboard Overview</h3>
              <p>
                See recent uploads, assessments, and storage quota usage immediately after
                sign-in.
              </p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <Shield size={32} />
              </div>
              <h3>Secure Access</h3>
              <p>
                Email and password authentication, token-based sessions, and protected routes
                cover the current MVP flow.
              </p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <TrendingUp size={32} />
              </div>
              <h3>Export Workflow</h3>
              <p>
                Download assessment outputs and use them in stakeholder reporting, review
                sessions, and planning documents.
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="how-it-works">
        <div className="container">
          <div className="section-header">
            <h2>How It Works</h2>
            <p>The core workflow is intentionally short and demo-friendly.</p>
          </div>

          <div className="steps">
            <div className="step">
              <div className="step-number">1</div>
              <div className="step-content">
                <h3>Upload Data</h3>
                <p>
                  Import hydrological, community, or infrastructure CSV files and review
                  validation feedback.
                </p>
              </div>
            </div>

            <div className="step">
              <div className="step-number">2</div>
              <div className="step-content">
                <h3>Assess and Visualize</h3>
                <p>
                  Review uploaded records on the dashboard, inspect them on the map, and run a
                  risk assessment.
                </p>
              </div>
            </div>

            <div className="step">
              <div className="step-number">3</div>
              <div className="step-content">
                <h3>Review and Export</h3>
                <p>
                  Filter the results, inspect the charts, and export the outputs for planning
                  and stakeholder communication.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="roles">
        <div className="container">
          <div className="section-header">
            <h2>Built for Teams That Need a Working Baseline</h2>
            <p>
              The release is useful as both a demo environment and an extensible starting
              point.
            </p>
          </div>

          <div className="roles-grid">
            <div className="role-card">
              <Award size={48} />
              <h3>Analysts</h3>
              <p>
                Upload datasets, run assessments, and inspect current results from a single
                browser-based workflow.
              </p>
              <Link to="/register" className="role-link">Get started</Link>
            </div>

            <div className="role-card">
              <Users size={48} />
              <h3>Demo Owners</h3>
              <p>
                Use the seeded frontend demo mode to capture screenshots and walk stakeholders
                through the product quickly.
              </p>
              <Link to="/login" className="role-link">Open app</Link>
            </div>

            <div className="role-card">
              <TrendingUp size={48} />
              <h3>Builders</h3>
              <p>
                Fork the codebase and evolve the services, data model, and deployment stack
                for your release roadmap.
              </p>
              <Link to="/register" className="role-link">Review source</Link>
            </div>
          </div>
        </div>
      </section>

      <section className="cta">
        <div className="container">
          <div className="cta-content">
            <Map size={64} className="cta-icon" />
            <h2>Ready to Prepare the Release?</h2>
            <p>
              Start from the MVP workflow for uploads, mapping, and risk assessment, then
              harden the backend services as needed.
            </p>
            <Link to="/register" className="btn btn-primary-large">
              Create Free Account
            </Link>
            <p className="cta-note">Open source and ready for local demo mode</p>
          </div>
        </div>
      </section>

      <footer className="landing-footer">
        <div className="container">
          <div className="footer-content">
            <div className="footer-section">
              <div className="footer-brand">
                <Droplet size={32} />
                <h3>WaterAccessOptimizer</h3>
              </div>
              <p>
                Open-source tooling for water access data uploads, mapping, and assessment
                workflows.
              </p>
            </div>

            <div className="footer-section">
              <h4>App</h4>
              <Link to="/register">Register</Link>
              <Link to="/login">Login</Link>
              <Link to="/dashboard">Dashboard</Link>
            </div>

            <div className="footer-section">
              <h4>Workflows</h4>
              <Link to="/upload">Uploads</Link>
              <Link to="/map">Map</Link>
              <Link to="/assessment">Assessments</Link>
            </div>

            <div className="footer-section">
              <h4>Release</h4>
              <Link to="/">Overview</Link>
              <Link to="/login">Sign in</Link>
              <Link to="/assessment">Results</Link>
            </div>
          </div>

          <div className="footer-bottom">
            <p>&copy; 2026 WaterAccessOptimizer.</p>
            <div className="footer-links">
              <Link to="/">Home</Link>
              <Link to="/login">Login</Link>
              <Link to="/register">Register</Link>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default Landing
