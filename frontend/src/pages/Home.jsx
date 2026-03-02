/**
 * Home Page Component
 *
 * Landing page with welcome message, statistics,
 * and quick actions tailored to MBTI types.
 */

import React from 'react'
import { Link } from 'react-router-dom'
import { Droplet, TrendingUp, Users, Globe } from 'lucide-react'
import './Home.css'

const Home = ({ mbtiType }) => {
  // MBTI-specific welcome messages
  const mbtiWelcomeMessages = {
    ENTJ: "Welcome, Commander! Ready to strategize water management solutions?",
    INFP: "Welcome, Mediator! Let's create meaningful water solutions together.",
    INFJ: "Welcome, Advocate! Your insights can transform water access.",
    ESTP: "Welcome, Entrepreneur! Time for action-oriented water solutions!",
    INTJ: "Welcome, Mastermind! Analyze and optimize water resources.",
    INTP: "Welcome, Thinker! Explore the logic behind water management.",
    ISTJ: "Welcome, Logistician! Structured water management awaits.",
    ESFJ: "Welcome, Consul! Support communities with better water access.",
    ISFP: "Welcome, Adventurer! Discover creative water solutions.",
    ENTP: "Welcome, Debater! Innovate water management strategies.",
    ISFJ: "Welcome, Defender! Nurture communities with clean water.",
    ESFP: "Welcome, Entertainer! Energize water access initiatives!",
    ENFJ: "Welcome, Protagonist! Lead transformative water projects.",
    ESTJ: "Welcome, Executive! Execute efficient water management.",
    ISTP: "Welcome, Virtuoso! Hands-on water problem solving awaits."
  }

  const welcomeMessage = mbtiWelcomeMessages[mbtiType] || mbtiWelcomeMessages.ENTJ

  return (
    <div className="home-page">
      <div className="container">
        {/* Hero Section */}
        <section className="hero">
          <div className="hero-content">
            <h1>WaterAccessOptimizer</h1>
            <p className="hero-subtitle">{welcomeMessage}</p>
            <p className="hero-description">
              AI-powered water management platform improving access to clean water
              for 2.2 billion people worldwide
            </p>
            <div className="hero-actions">
              <Link to="/analyze" className="button button-primary">
                Start Analysis
              </Link>
              <Link to="/explore" className="button button-secondary">
                Explore 3D Maps
              </Link>
            </div>
          </div>
        </section>

        {/* Statistics Section */}
        <section className="statistics">
          <div className="stat-card">
            <Droplet size={48} className="stat-icon" />
            <h3>2.2 Billion</h3>
            <p>People lack safely managed drinking water</p>
          </div>
          <div className="stat-card">
            <Globe size={48} className="stat-icon" />
            <h3>Global Impact</h3>
            <p>Operating in Sub-Saharan Africa & South Asia</p>
          </div>
          <div className="stat-card">
            <TrendingUp size={48} className="stat-icon" />
            <h3>AI-Powered</h3>
            <p>Predictive water availability analysis</p>
          </div>
          <div className="stat-card">
            <Users size={48} className="stat-icon" />
            <h3>16 MBTI Types</h3>
            <p>Personalized for every personality</p>
          </div>
        </section>

        {/* Features Section */}
        <section className="features">
          <h2>Key Features</h2>
          <div className="feature-grid">
            <div className="feature-item card">
              <h3>Data Integration</h3>
              <p>
                Upload CSV, JSON, and GeoJSON files from USGS, WHO, OpenStreetMap,
                and local sources for comprehensive water data analysis.
              </p>
            </div>
            <div className="feature-item card">
              <h3>3D Visualization</h3>
              <p>
                Interactive Three.js visualizations of hydrological maps, aquifer
                levels, and water distribution networks.
              </p>
            </div>
            <div className="feature-item card">
              <h3>AI Predictions</h3>
              <p>
                PyTorch-powered predictions for water availability, quality risks,
                and management strategies.
              </p>
            </div>
            <div className="feature-item card">
              <h3>Real-Time Collaboration</h3>
              <p>
                WebSocket-based collaboration for communities, NGOs, and
                policymakers to work together.
              </p>
            </div>
            <div className="feature-item card">
              <h3>LLM Assistant</h3>
              <p>
                Natural language queries for water management guidance tailored
                to your MBTI type.
              </p>
            </div>
            <div className="feature-item card">
              <h3>Open Source</h3>
              <p>
                Fully open-source solution ensuring accessibility in low-resource
                settings worldwide.
              </p>
            </div>
          </div>
        </section>

        {/* Call to Action */}
        <section className="cta">
          <div className="card">
            <h2>Start Optimizing Water Access Today</h2>
            <p>
              Upload your water data and get AI-powered recommendations
              for improving water access in your community.
            </p>
            <Link to="/analyze" className="button button-large">
              Get Started
            </Link>
          </div>
        </section>
      </div>
    </div>
  )
}

export default Home
