import React from 'react'
import { Link } from 'react-router-dom'
import { Droplet, Globe, BarChart3, Users, Brain, Shield, Zap, TrendingUp, Map, Award } from 'lucide-react'
import './Landing.css'

/**
 * Landing Page Component
 *
 * Public landing page for WaterAccessOptimizer
 * Shows features, benefits, and calls to action for new users
 */
function Landing() {
  return (
    <div className="landing">
      {/* Hero Section */}
      <section className="hero">
        <div className="hero-content">
          <div className="hero-badge">
            <Droplet size={24} />
            <span>Advancing Water Access Worldwide</span>
          </div>

          <h1 className="hero-title">
            Optimize Water Access with
            <span className="gradient-text"> AI-Powered Insights</span>
          </h1>

          <p className="hero-subtitle">
            Analyze hydrological data, visualize water infrastructure in 3D, and get AI-driven predictions
            to improve water access for communities worldwide.
          </p>

          <div className="hero-cta">
            <Link to="/register" className="btn btn-primary-large">
              Get Started Free
            </Link>
            <Link to="/login" className="btn btn-secondary-large">
              Sign In
            </Link>
          </div>

          <div className="hero-stats">
            <div className="stat">
              <div className="stat-value">1M+</div>
              <div className="stat-label">Data Points Analyzed</div>
            </div>
            <div className="stat">
              <div className="stat-value">50+</div>
              <div className="stat-label">Countries Served</div>
            </div>
            <div className="stat">
              <div className="stat-value">99.9%</div>
              <div className="stat-label">Prediction Accuracy</div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="features">
        <div className="container">
          <div className="section-header">
            <h2>Powerful Features for Water Management</h2>
            <p>Everything you need to analyze, visualize, and optimize water access</p>
          </div>

          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon">
                <Globe size={32} />
              </div>
              <h3>3D Visualization</h3>
              <p>Interactive 3D maps powered by Three.js to visualize hydrological data, infrastructure, and water networks in stunning detail.</p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <Brain size={32} />
              </div>
              <h3>AI Predictions</h3>
              <p>Machine learning models predict water availability, quality issues, and optimal management strategies for your region.</p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <BarChart3 size={32} />
              </div>
              <h3>Data Analysis</h3>
              <p>Analyze hydrological data from USGS, WHO, and custom sources. Upload CSV, JSON, or GeoJSON files for instant insights.</p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <Users size={32} />
              </div>
              <h3>Collaboration</h3>
              <p>Real-time collaboration with team members. Share visualizations, annotations, and insights across your organization.</p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <Shield size={32} />
              </div>
              <h3>Enterprise Security</h3>
              <p>SSO integration, MFA, role-based access control, and audit logs. Bank-level security for your sensitive water data.</p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">
                <Zap size={32} />
              </div>
              <h3>MBTI Personalization</h3>
              <p>Tailored experience based on your personality type. Different workflows for analytical thinkers vs. creative problem-solvers.</p>
            </div>
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="how-it-works">
        <div className="container">
          <div className="section-header">
            <h2>How It Works</h2>
            <p>Get started in minutes with our simple workflow</p>
          </div>

          <div className="steps">
            <div className="step">
              <div className="step-number">1</div>
              <div className="step-content">
                <h3>Upload Your Data</h3>
                <p>Import hydrological data, community information, or infrastructure details from multiple sources.</p>
              </div>
            </div>

            <div className="step">
              <div className="step-number">2</div>
              <div className="step-content">
                <h3>Analyze & Visualize</h3>
                <p>Create stunning 3D visualizations and get AI-powered insights about water availability and quality.</p>
              </div>
            </div>

            <div className="step">
              <div className="step-number">3</div>
              <div className="step-content">
                <h3>Collaborate & Act</h3>
                <p>Share findings with your team, export reports, and implement data-driven water management strategies.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* User Roles Section */}
      <section className="roles">
        <div className="container">
          <div className="section-header">
            <h2>Built for Everyone</h2>
            <p>From individual researchers to large enterprises</p>
          </div>

          <div className="roles-grid">
            <div className="role-card">
              <Award size={48} />
              <h3>Researchers</h3>
              <p>Free tier with all core features. Analyze data, create visualizations, and collaborate with peers.</p>
              <Link to="/register" className="role-link">Start Free →</Link>
            </div>

            <div className="role-card">
              <Users size={48} />
              <h3>Organizations</h3>
              <p>Team collaboration, advanced analytics, priority support, and custom integrations.</p>
              <Link to="/register" className="role-link">Contact Sales →</Link>
            </div>

            <div className="role-card">
              <TrendingUp size={48} />
              <h3>Enterprises</h3>
              <p>SSO, unlimited users, dedicated support, SLA guarantees, and custom AI models.</p>
              <Link to="/register" className="role-link">Get Enterprise →</Link>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta">
        <div className="container">
          <div className="cta-content">
            <Map size={64} className="cta-icon" />
            <h2>Ready to Transform Water Access?</h2>
            <p>Join thousands of researchers, NGOs, and governments using WaterAccessOptimizer to improve water security worldwide.</p>
            <Link to="/register" className="btn btn-primary-large">
              Create Free Account
            </Link>
            <p className="cta-note">No credit card required • Free tier available forever</p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing-footer">
        <div className="container">
          <div className="footer-content">
            <div className="footer-section">
              <div className="footer-brand">
                <Droplet size={32} />
                <h3>WaterAccessOptimizer</h3>
              </div>
              <p>Open-source solution for advancing water security and access worldwide.</p>
            </div>

            <div className="footer-section">
              <h4>Product</h4>
              <Link to="/features">Features</Link>
              <Link to="/pricing">Pricing</Link>
              <Link to="/docs">Documentation</Link>
            </div>

            <div className="footer-section">
              <h4>Company</h4>
              <Link to="/about">About Us</Link>
              <Link to="/contact">Contact</Link>
              <Link to="/careers">Careers</Link>
            </div>

            <div className="footer-section">
              <h4>Resources</h4>
              <Link to="/blog">Blog</Link>
              <Link to="/guides">Guides</Link>
              <Link to="/api">API Docs</Link>
            </div>
          </div>

          <div className="footer-bottom">
            <p>&copy; 2024 WaterAccessOptimizer. All rights reserved.</p>
            <div className="footer-links">
              <Link to="/privacy">Privacy Policy</Link>
              <Link to="/terms">Terms of Service</Link>
              <Link to="/security">Security</Link>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default Landing
