"""
WaterAccessOptimizer - AI Model Service

This service uses PyTorch to predict water availability and management strategies
based on hydrological, community, and infrastructure data.

Technologies: FastAPI, PyTorch, NumPy, pandas
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Optional
import torch
import torch.nn as nn
import numpy as np
import uvicorn
import logging
from datetime import datetime

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Water Predictor AI Service",
    description="AI-powered water availability and management predictions",
    version="1.0.0"
)

# Add CORS middleware for cross-origin requests
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify exact origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ==================== DATA MODELS ====================

class HydroInput(BaseModel):
    """Model for hydrological data input"""
    measurement_value: float
    measurement_unit: str
    data_type: str
    latitude: float
    longitude: float


class CommunityInput(BaseModel):
    """Model for community data input"""
    population: int
    water_access_level: str  # no_access, basic, limited, safely_managed
    latitude: float
    longitude: float


class InfrastructureInput(BaseModel):
    """Model for infrastructure data input"""
    facility_type: str  # treatment_plant, pipeline, pump_station, reservoir
    capacity: float
    operational_status: str  # operational, maintenance, non_operational
    latitude: float
    longitude: float


class PredictionRequest(BaseModel):
    """Complete prediction request model"""
    hydro_data: List[HydroInput]
    community_data: List[CommunityInput]
    infrastructure_data: List[InfrastructureInput]
    mbti_type: Optional[str] = "ENTJ"  # For personalized recommendations


class PredictionResponse(BaseModel):
    """Prediction response model"""
    availability_score: float  # 0-1 score for water availability
    management_strategies: List[str]  # Recommended actions
    risk_level: str  # low, medium, high
    confidence: float  # Model confidence 0-1
    recommendations: Dict[str, str]  # MBTI-tailored recommendations


# ==================== NEURAL NETWORK MODEL ====================

class WaterPredictionModel(nn.Module):
    """
    PyTorch neural network for water availability prediction.

    Architecture:
    - Input layer: Combined features from hydro, community, and infrastructure data
    - Hidden layers: 3 fully connected layers with ReLU activation
    - Output layer: Water availability score (0-1)
    """

    def __init__(self, input_size=20):
        super(WaterPredictionModel, self).__init__()

        # Define network layers
        self.fc1 = nn.Linear(input_size, 128)
        self.fc2 = nn.Linear(128, 64)
        self.fc3 = nn.Linear(64, 32)
        self.fc4 = nn.Linear(32, 1)

        # Activation and dropout for regularization
        self.relu = nn.ReLU()
        self.dropout = nn.Dropout(0.3)
        self.sigmoid = nn.Sigmoid()

    def forward(self, x):
        """Forward pass through the network"""
        x = self.relu(self.fc1(x))
        x = self.dropout(x)
        x = self.relu(self.fc2(x))
        x = self.dropout(x)
        x = self.relu(self.fc3(x))
        x = self.sigmoid(self.fc4(x))
        return x


# ==================== MODEL INITIALIZATION ====================

# Initialize the model
model = WaterPredictionModel(input_size=20)

# Try to load pre-trained weights if available
try:
    model.load_state_dict(torch.load('model.pt', map_location=torch.device('cpu')))
    model.eval()
    logger.info("Loaded pre-trained model successfully")
except FileNotFoundError:
    logger.warning("No pre-trained model found. Using random initialization.")
    logger.info("In production, train the model on real water data first.")


# ==================== HELPER FUNCTIONS ====================

def preprocess_data(request: PredictionRequest) -> torch.Tensor:
    """
    Preprocess input data into tensor format for the model.

    Combines hydrological, community, and infrastructure data into
    a fixed-size feature vector.

    Args:
        request: PredictionRequest with all input data

    Returns:
        torch.Tensor: Preprocessed feature vector
    """
    features = []

    # Extract hydro features (average measurements)
    if request.hydro_data:
        avg_measurement = np.mean([h.measurement_value for h in request.hydro_data])
        features.extend([
            avg_measurement,
            len(request.hydro_data),
            request.hydro_data[0].latitude,
            request.hydro_data[0].longitude
        ])
    else:
        features.extend([0, 0, 0, 0])

    # Extract community features
    if request.community_data:
        total_population = sum([c.population for c in request.community_data])
        # Map access levels to numeric values
        access_map = {"no_access": 0, "basic": 0.33, "limited": 0.66, "safely_managed": 1.0}
        avg_access = np.mean([access_map.get(c.water_access_level, 0) for c in request.community_data])
        features.extend([
            total_population,
            len(request.community_data),
            avg_access,
            request.community_data[0].latitude,
            request.community_data[0].longitude
        ])
    else:
        features.extend([0, 0, 0, 0, 0])

    # Extract infrastructure features
    if request.infrastructure_data:
        total_capacity = sum([i.capacity for i in request.infrastructure_data])
        # Map operational status to numeric values
        status_map = {"non_operational": 0, "maintenance": 0.5, "operational": 1.0}
        avg_status = np.mean([status_map.get(i.operational_status, 0) for i in request.infrastructure_data])
        features.extend([
            total_capacity,
            len(request.infrastructure_data),
            avg_status,
            request.infrastructure_data[0].latitude,
            request.infrastructure_data[0].longitude
        ])
    else:
        features.extend([0, 0, 0, 0, 0])

    # Pad or truncate to match input size
    while len(features) < 20:
        features.append(0)
    features = features[:20]

    return torch.tensor(features, dtype=torch.float32).unsqueeze(0)


def generate_management_strategies(availability_score: float, request: PredictionRequest) -> List[str]:
    """
    Generate water management strategies based on prediction results.

    Args:
        availability_score: Predicted water availability (0-1)
        request: Original prediction request

    Returns:
        List of recommended management strategies
    """
    strategies = []

    if availability_score < 0.3:
        strategies.append("URGENT: Implement emergency water conservation measures")
        strategies.append("Install rainwater harvesting systems for immediate relief")
        strategies.append("Establish emergency water distribution points")
        strategies.append("Repair non-operational infrastructure immediately")
    elif availability_score < 0.6:
        strategies.append("Expand water treatment capacity to meet growing demand")
        strategies.append("Upgrade existing infrastructure for efficiency")
        strategies.append("Implement community-level water conservation programs")
        strategies.append("Monitor aquifer levels regularly")
    else:
        strategies.append("Maintain current water management practices")
        strategies.append("Invest in preventive infrastructure maintenance")
        strategies.append("Develop long-term sustainability plans")
        strategies.append("Share water management best practices with neighboring communities")

    # Add infrastructure-specific recommendations
    if request.infrastructure_data:
        non_op_count = sum(1 for i in request.infrastructure_data if i.operational_status == "non_operational")
        if non_op_count > 0:
            strategies.append(f"Priority: Repair {non_op_count} non-operational facilities")

    return strategies


def get_mbti_tailored_recommendations(mbti_type: str, strategies: List[str]) -> Dict[str, str]:
    """
    Tailor recommendations based on MBTI personality type.

    Args:
        mbti_type: User's MBTI personality type
        strategies: List of management strategies

    Returns:
        Dictionary with MBTI-tailored recommendations
    """
    mbti_styles = {
        "ENTJ": {
            "style": "strategic",
            "message": "Here's your strategic action plan for water management optimization:"
        },
        "INFP": {
            "style": "creative",
            "message": "Explore these sustainable and value-aligned water solutions:"
        },
        "INFJ": {
            "style": "empathetic",
            "message": "These holistic approaches will support your community's water needs:"
        },
        "ESTP": {
            "style": "actionable",
            "message": "Quick action items for immediate water management improvement:"
        },
        "INTJ": {
            "style": "analytical",
            "message": "Detailed strategic analysis for water resource optimization:"
        },
        "INTP": {
            "style": "logical",
            "message": "Logical framework for addressing water management challenges:"
        },
        "ISTJ": {
            "style": "structured",
            "message": "Step-by-step water management implementation plan:"
        },
        "ESFJ": {
            "style": "supportive",
            "message": "Community-focused water solutions that benefit everyone:"
        },
        "ISFP": {
            "style": "creative",
            "message": "Gentle, sustainable approaches to water management:"
        },
        "ENTP": {
            "style": "innovative",
            "message": "Innovative water management strategies to explore:"
        },
        "ISFJ": {
            "style": "nurturing",
            "message": "Practical, caring solutions for your community's water needs:"
        },
        "ESFP": {
            "style": "energetic",
            "message": "Dynamic water management actions you can implement now:"
        },
        "ENFJ": {
            "style": "inspirational",
            "message": "Visionary water management plan for community transformation:"
        },
        "ESTJ": {
            "style": "organized",
            "message": "Structured water management execution plan:"
        },
        "ISTP": {
            "style": "practical",
            "message": "Hands-on water management solutions:"
        }
    }

    style_info = mbti_styles.get(mbti_type, mbti_styles["ENTJ"])

    return {
        "style": style_info["style"],
        "message": style_info["message"],
        "strategies": ", ".join(strategies[:3])  # Top 3 strategies
    }


# ==================== API ENDPOINTS ====================

@app.get("/")
async def root():
    """Root endpoint - service information"""
    return {
        "service": "Water Predictor AI Service",
        "version": "1.0.0",
        "status": "operational",
        "description": "AI-powered water availability and management predictions"
    }


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "model_loaded": model is not None
    }


@app.post("/predict", response_model=PredictionResponse)
async def predict_water_availability(request: PredictionRequest):
    """
    Predict water availability and generate management recommendations.

    This endpoint processes hydrological, community, and infrastructure data
    to provide AI-powered predictions and MBTI-tailored recommendations.

    Args:
        request: PredictionRequest with all input data

    Returns:
        PredictionResponse with availability score, strategies, and recommendations
    """
    try:
        logger.info(f"Received prediction request for MBTI type: {request.mbti_type}")

        # Preprocess input data
        input_tensor = preprocess_data(request)

        # Make prediction
        with torch.no_grad():
            prediction = model(input_tensor)
            availability_score = float(prediction.item())

        # Determine risk level
        if availability_score < 0.3:
            risk_level = "high"
        elif availability_score < 0.6:
            risk_level = "medium"
        else:
            risk_level = "low"

        # Generate management strategies
        strategies = generate_management_strategies(availability_score, request)

        # Get MBTI-tailored recommendations
        mbti_recommendations = get_mbti_tailored_recommendations(
            request.mbti_type or "ENTJ",
            strategies
        )

        # Calculate confidence (simplified - in production, use proper uncertainty estimation)
        confidence = 0.85 if len(request.hydro_data) > 3 else 0.65

        response = PredictionResponse(
            availability_score=availability_score,
            management_strategies=strategies,
            risk_level=risk_level,
            confidence=confidence,
            recommendations=mbti_recommendations
        )

        logger.info(f"Prediction completed: score={availability_score:.2f}, risk={risk_level}")

        return response

    except Exception as e:
        logger.error(f"Prediction error: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@app.get("/resources/check")
async def check_resources():
    """
    Check system resources for multithreading optimization.

    Returns CPU cores and memory info to determine if multithreading
    should be enabled (CPU > 4 cores and memory > 8GB).
    """
    import psutil
    import multiprocessing

    cpu_count = multiprocessing.cpu_count()
    memory_gb = psutil.virtual_memory().total / (1024**3)

    enable_multithreading = cpu_count > 4 and memory_gb > 8

    return {
        "cpu_cores": cpu_count,
        "memory_gb": round(memory_gb, 2),
        "enable_multithreading": enable_multithreading,
        "recommendation": "Enable multiprocessing" if enable_multithreading else "Use single process"
    }


# ==================== MAIN ENTRY POINT ====================

if __name__ == "__main__":
    # Run the FastAPI server
    logger.info("Starting Water Predictor AI Service...")
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info"
    )
