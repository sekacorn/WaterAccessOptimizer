#!/bin/bash

# WaterAccessOptimizer - Start All Services (Non-Docker)
# This script starts all backend services and frontend for local development

echo "================================================"
echo "  WaterAccessOptimizer - Starting All Services"
echo "================================================"

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java 17 is required but not found!"
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is required but not found!"
    exit 1
fi

# Check for Node.js
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is required but not found!"
    exit 1
fi

# Check for Python
if ! command -v python &> /dev/null; then
    echo "ERROR: Python is required but not found!"
    exit 1
fi

echo ""
echo "Prerequisites check: PASSED"
echo ""

# Create logs directory
mkdir -p logs

echo "Step 1: Building all backend services..."
echo "----------------------------------------"

cd backend/water-integrator && mvn clean package -DskipTests > ../../logs/water-integrator-build.log 2>&1 &
cd ../..

cd backend/water-visualizer && mvn clean package -DskipTests > ../../logs/water-visualizer-build.log 2>&1 &
cd ../..

cd backend/user-session && mvn clean package -DskipTests > ../../logs/user-session-build.log 2>&1 &
cd ../..

cd backend/llm-service && mvn clean package -DskipTests > ../../logs/llm-service-build.log 2>&1 &
cd ../..

cd backend/collaboration-service && mvn clean package -DskipTests > ../../logs/collaboration-service-build.log 2>&1 &
cd ../..

cd backend/api-gateway && mvn clean package -DskipTests > ../../logs/api-gateway-build.log 2>&1 &
cd ../..

echo "Waiting for builds to complete (this may take a few minutes)..."
wait

echo ""
echo "Step 2: Installing frontend dependencies..."
echo "----------------------------------------"
cd frontend
npm install > ../logs/frontend-install.log 2>&1
cd ..

echo ""
echo "Step 3: Installing AI model dependencies..."
echo "----------------------------------------"
cd ai-model
pip install -r requirements.txt > ../logs/ai-model-install.log 2>&1
cd ..

echo ""
echo "Step 4: Starting all services..."
echo "----------------------------------------"

# Start backend services
echo "Starting water-integrator on port 8081..."
cd backend/water-integrator
java -jar target/*.jar > ../../logs/water-integrator.log 2>&1 &
cd ../..
sleep 2

echo "Starting water-visualizer on port 8082..."
cd backend/water-visualizer
java -jar target/*.jar > ../../logs/water-visualizer.log 2>&1 &
cd ../..
sleep 2

echo "Starting user-session on port 8083..."
cd backend/user-session
java -jar target/*.jar > ../../logs/user-session.log 2>&1 &
cd ../..
sleep 2

echo "Starting llm-service on port 8084..."
cd backend/llm-service
java -jar target/*.jar > ../../logs/llm-service.log 2>&1 &
cd ../..
sleep 2

echo "Starting collaboration-service on port 8085..."
cd backend/collaboration-service
java -jar target/*.jar > ../../logs/collaboration-service.log 2>&1 &
cd ../..
sleep 2

echo "Starting api-gateway on port 8080..."
cd backend/api-gateway
java -jar target/*.jar > ../../logs/api-gateway.log 2>&1 &
cd ../..
sleep 2

echo "Starting AI model service on port 8000..."
cd ai-model
python water_predictor.py > ../logs/ai-model.log 2>&1 &
cd ..
sleep 2

echo "Starting frontend on port 3000..."
cd frontend
npm run dev > ../logs/frontend.log 2>&1 &
cd ..

echo ""
echo "================================================"
echo "  All services started successfully!"
echo "================================================"
echo ""
echo "Service endpoints:"
echo "  - Frontend:            http://localhost:3000"
echo "  - API Gateway:         http://localhost:8080"
echo "  - Water Integrator:    http://localhost:8081"
echo "  - Water Visualizer:    http://localhost:8082"
echo "  - User Session:        http://localhost:8083"
echo "  - LLM Service:         http://localhost:8084"
echo "  - Collaboration:       http://localhost:8085"
echo "  - AI Model:            http://localhost:8000"
echo ""
echo "Logs are available in the 'logs' directory"
echo ""
echo "To stop all services, run: ./stop-services.sh"
echo "================================================"
