#!/bin/bash

# WaterAccessOptimizer - Stop All Services

echo "Stopping all WaterAccessOptimizer services..."

# Kill Java processes (backend services)
pkill -f "water-integrator"
pkill -f "water-visualizer"
pkill -f "user-session"
pkill -f "llm-service"
pkill -f "collaboration-service"
pkill -f "api-gateway"

# Kill Python processes (AI model)
pkill -f "water_predictor.py"

# Kill Node processes (frontend)
pkill -f "vite"

echo "All services stopped."
