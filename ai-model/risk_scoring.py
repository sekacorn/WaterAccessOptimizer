"""
Risk Scoring Algorithm - Version 1.0.0
Implements rule-based water access risk assessment with WHO guidelines
Iteration 5 - Agents 04 (Optimization Engine) + 06 (Risk Scoring & Alerts)
"""

from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass
from enum import Enum
import math


# WHO Guidelines for Water Quality Parameters (mg/L except where noted)
WHO_GUIDELINES = {
    "arsenic": {
        "unit": "mg/L",
        "guideline_value": 0.01,
        "health_impact": "Chronic exposure causes cancer, skin lesions, cardiovascular disease",
        "severity": "HIGH"
    },
    "fluoride": {
        "unit": "mg/L",
        "guideline_value": 1.5,
        "health_impact": "Excess causes dental and skeletal fluorosis",
        "severity": "MEDIUM"
    },
    "nitrate": {
        "unit": "mg/L",
        "guideline_value": 50.0,
        "health_impact": "Causes methemoglobinemia (blue baby syndrome) in infants",
        "severity": "HIGH"
    },
    "lead": {
        "unit": "mg/L",
        "guideline_value": 0.01,
        "health_impact": "Neurotoxic, affects brain development in children",
        "severity": "HIGH"
    },
    "mercury": {
        "unit": "mg/L",
        "guideline_value": 0.006,
        "health_impact": "Damages nervous system, kidneys",
        "severity": "HIGH"
    },
    "chromium": {
        "unit": "mg/L",
        "guideline_value": 0.05,
        "health_impact": "Hexavalent form is carcinogenic",
        "severity": "MEDIUM"
    },
    "e_coli": {
        "unit": "CFU/100mL",
        "guideline_value": 0,
        "health_impact": "Indicates fecal contamination, causes diarrheal disease",
        "severity": "HIGH"
    },
    "total_coliform": {
        "unit": "CFU/100mL",
        "guideline_value": 0,
        "health_impact": "Indicates potential pathogen presence",
        "severity": "MEDIUM"
    },
    "turbidity": {
        "unit": "NTU",
        "guideline_value": 5.0,
        "health_impact": "High turbidity shields pathogens from disinfection",
        "severity": "MEDIUM"
    },
    "ph": {
        "unit": "pH units",
        "guideline_value": (6.5, 8.5),  # Range
        "health_impact": "Extreme pH affects taste and pipe corrosion",
        "severity": "LOW"
    }
}


# JMP Service Ladder Levels
class ServiceLevel(Enum):
    NONE = "none"
    LIMITED = "limited"
    BASIC = "basic"
    SAFELY_MANAGED = "safely_managed"


# Risk Levels
class RiskLevel(Enum):
    LOW = "LOW"          # 0-33
    MEDIUM = "MEDIUM"    # 34-66
    HIGH = "HIGH"        # 67-100


# Confidence Levels
class ConfidenceLevel(Enum):
    NONE = "NONE"        # 0 samples
    LOW = "LOW"          # 1-9 samples
    MEDIUM = "MEDIUM"    # 10-30 samples
    HIGH = "HIGH"        # >30 samples


@dataclass
class RiskFactor:
    """Individual risk factor contributing to overall score"""
    component: str
    measured_value: float
    guideline_value: float
    impact_description: str
    contribution_percent: float
    severity: str


@dataclass
class RiskAssessment:
    """Complete risk assessment result"""
    overall_risk_score: float
    risk_level: RiskLevel
    confidence_level: ConfidenceLevel
    component_scores: Dict[str, float]
    top_factors: List[RiskFactor]
    explanation: str
    algorithm_version: str
    sample_size: int


def calculate_water_quality_risk(
    water_quality_data: List[Dict[str, float]],
    parameters_to_check: Optional[List[str]] = None
) -> Tuple[float, List[RiskFactor]]:
    """
    Calculate water quality risk score (0-100) based on WHO guidelines.
    Weight: 35% of overall risk score

    Args:
        water_quality_data: List of water quality measurements
        parameters_to_check: Specific parameters to check (default: all WHO parameters)

    Returns:
        Tuple of (risk_score, risk_factors)
    """
    if not water_quality_data:
        return 50.0, []  # Default medium risk if no data

    if parameters_to_check is None:
        parameters_to_check = list(WHO_GUIDELINES.keys())

    risk_factors = []
    parameter_risks = []

    for data_point in water_quality_data:
        for param in parameters_to_check:
            if param not in data_point:
                continue

            measured = data_point[param]
            guideline = WHO_GUIDELINES[param]
            guideline_value = guideline["guideline_value"]

            # Calculate exceedance ratio
            if param == "ph":
                # pH is a range
                min_ph, max_ph = guideline_value
                if measured < min_ph:
                    exceedance = (min_ph - measured) / min_ph
                elif measured > max_ph:
                    exceedance = (measured - max_ph) / max_ph
                else:
                    exceedance = 0.0
            elif guideline_value == 0:
                # E. coli and total coliform: any detection is high risk
                # Scale: 0 CFU = 0 risk, 1 CFU = 50 risk, 10+ CFU = 100 risk
                exceedance = min(10, measured)
            else:
                # Other parameters are maximum values
                exceedance = max(0, (measured - guideline_value) / guideline_value)

            # Convert exceedance to risk score (0-100)
            # 0% exceedance = 0 risk
            # 100% exceedance = 50 risk
            # 500% exceedance = 100 risk
            param_risk = min(100, exceedance * 20)
            parameter_risks.append(param_risk)

            # Track significant risk factors (risk > 30)
            if param_risk > 30:
                risk_factors.append(RiskFactor(
                    component="water_quality",
                    measured_value=measured,
                    guideline_value=guideline_value if param != "ph" else f"{guideline_value[0]}-{guideline_value[1]}",
                    impact_description=guideline["health_impact"],
                    contribution_percent=param_risk * 0.35,  # Apply 35% weight
                    severity=guideline["severity"]
                ))

    # Average risk across all parameters
    avg_risk = sum(parameter_risks) / len(parameter_risks) if parameter_risks else 50.0

    # Sort risk factors by contribution
    risk_factors.sort(key=lambda x: x.contribution_percent, reverse=True)

    return avg_risk, risk_factors


def calculate_access_distance_risk(
    community_data: List[Dict],
    infrastructure_data: List[Dict]
) -> Tuple[float, List[RiskFactor]]:
    """
    Calculate access distance risk score (0-100) based on JMP service ladder.
    Weight: 30% of overall risk score

    JMP Service Ladder:
    - Safely Managed: <30 min round trip, water available when needed
    - Basic: <30 min round trip from improved source
    - Limited: >30 min round trip from improved source
    - None: Unimproved source or >30 min from improved source

    Args:
        community_data: List of community location data
        infrastructure_data: List of water infrastructure locations

    Returns:
        Tuple of (risk_score, risk_factors)
    """
    if not community_data or not infrastructure_data:
        return 75.0, []  # High risk if no infrastructure data

    risk_factors = []
    distance_risks = []

    for community in community_data:
        # Find nearest water source
        min_distance = float('inf')
        nearest_source = None

        for infra in infrastructure_data:
            # Calculate haversine distance (simplified)
            lat1, lon1 = community.get('latitude', 0), community.get('longitude', 0)
            lat2, lon2 = infra.get('latitude', 0), infra.get('longitude', 0)

            # Simplified distance calculation (km)
            distance = math.sqrt((lat2 - lat1)**2 + (lon2 - lon1)**2) * 111  # Approx km per degree

            if distance < min_distance:
                min_distance = distance
                nearest_source = infra

        # Convert distance to risk score
        # 0-1 km = 0-10 risk (Safely Managed)
        # 1-3 km = 10-40 risk (Basic)
        # 3-5 km = 40-70 risk (Limited)
        # >5 km = 70-100 risk (None)
        if min_distance < 1:
            distance_risk = min_distance * 10
        elif min_distance < 3:
            distance_risk = 10 + (min_distance - 1) * 15
        elif min_distance < 5:
            distance_risk = 40 + (min_distance - 3) * 15
        else:
            distance_risk = min(100, 70 + (min_distance - 5) * 6)

        distance_risks.append(distance_risk)

        # Track significant risk factors (distance > 3km)
        if min_distance > 3:
            risk_factors.append(RiskFactor(
                component="access_distance",
                measured_value=min_distance,
                guideline_value=1.0,  # <1km ideal
                impact_description=f"Community is {min_distance:.1f}km from nearest water source (JMP: Limited/None)",
                contribution_percent=distance_risk * 0.30,  # Apply 30% weight
                severity="HIGH" if min_distance > 5 else "MEDIUM"
            ))

    # Average risk across all communities
    avg_risk = sum(distance_risks) / len(distance_risks) if distance_risks else 75.0

    # Sort risk factors by contribution
    risk_factors.sort(key=lambda x: x.contribution_percent, reverse=True)

    return avg_risk, risk_factors


def calculate_infrastructure_risk(
    infrastructure_data: List[Dict]
) -> Tuple[float, List[RiskFactor]]:
    """
    Calculate infrastructure risk score (0-100) based on functionality and age.
    Weight: 25% of overall risk score

    Args:
        infrastructure_data: List of water infrastructure with functionality status

    Returns:
        Tuple of (risk_score, risk_factors)
    """
    if not infrastructure_data:
        return 100.0, []  # Maximum risk if no infrastructure

    risk_factors = []
    functionality_risks = []

    functional_count = 0
    non_functional_count = 0
    needs_repair_count = 0

    for infra in infrastructure_data:
        status = infra.get('functionality', 'unknown').lower()

        if status == 'functional':
            functional_count += 1
            functionality_risks.append(0)
        elif status == 'needs_repair':
            needs_repair_count += 1
            functionality_risks.append(50)
        elif status == 'non_functional':
            non_functional_count += 1
            functionality_risks.append(100)
        else:
            functionality_risks.append(50)  # Unknown status = medium risk

    total_count = len(infrastructure_data)
    non_functional_rate = (non_functional_count + needs_repair_count) / total_count

    # Calculate average risk
    avg_risk = sum(functionality_risks) / len(functionality_risks)

    # Add risk factor if >20% non-functional
    if non_functional_rate > 0.2:
        risk_factors.append(RiskFactor(
            component="infrastructure",
            measured_value=non_functional_rate * 100,
            guideline_value=10.0,  # <10% non-functional is good
            impact_description=f"{non_functional_rate*100:.0f}% of infrastructure is non-functional or needs repair",
            contribution_percent=avg_risk * 0.25,  # Apply 25% weight
            severity="HIGH" if non_functional_rate > 0.5 else "MEDIUM"
        ))

    return avg_risk, risk_factors


def calculate_population_pressure_risk(
    community_data: List[Dict],
    infrastructure_data: List[Dict]
) -> Tuple[float, List[RiskFactor]]:
    """
    Calculate population pressure risk score (0-100) based on people per water point.
    Weight: 10% of overall risk score

    WHO Recommendation: Max 250 people per water point

    Args:
        community_data: List of community data with population
        infrastructure_data: List of functional water infrastructure

    Returns:
        Tuple of (risk_score, risk_factors)
    """
    if not community_data or not infrastructure_data:
        return 80.0, []  # High risk if no data

    risk_factors = []

    # Count functional water points
    functional_count = sum(1 for infra in infrastructure_data
                          if infra.get('functionality', '').lower() == 'functional')

    if functional_count == 0:
        return 100.0, []  # Maximum risk if no functional infrastructure

    # Calculate total population
    total_population = sum(c.get('population', 0) for c in community_data)

    # Calculate people per water point
    people_per_point = total_population / functional_count if functional_count > 0 else float('inf')

    # Convert to risk score
    # 0-250 people = 0-20 risk (Good)
    # 250-500 people = 20-50 risk (Acceptable)
    # 500-1000 people = 50-80 risk (Stressed)
    # >1000 people = 80-100 risk (Critical)
    if people_per_point <= 250:
        pressure_risk = (people_per_point / 250) * 20
    elif people_per_point <= 500:
        pressure_risk = 20 + ((people_per_point - 250) / 250) * 30
    elif people_per_point <= 1000:
        pressure_risk = 50 + ((people_per_point - 500) / 500) * 30
    else:
        pressure_risk = min(100, 80 + ((people_per_point - 1000) / 1000) * 20)

    # Add risk factor if >250 people per point
    if people_per_point > 250:
        risk_factors.append(RiskFactor(
            component="population_pressure",
            measured_value=people_per_point,
            guideline_value=250.0,
            impact_description=f"{people_per_point:.0f} people per functional water point (WHO: max 250)",
            contribution_percent=pressure_risk * 0.10,  # Apply 10% weight
            severity="HIGH" if people_per_point > 1000 else "MEDIUM"
        ))

    return pressure_risk, risk_factors


def calculate_confidence_level(sample_size: int) -> ConfidenceLevel:
    """
    Determine confidence level based on sample size.

    Args:
        sample_size: Number of data points used in assessment

    Returns:
        ConfidenceLevel enum
    """
    if sample_size == 0:
        return ConfidenceLevel.NONE
    elif sample_size < 10:
        return ConfidenceLevel.LOW
    elif sample_size <= 30:
        return ConfidenceLevel.MEDIUM
    else:
        return ConfidenceLevel.HIGH


def determine_risk_level(risk_score: float) -> RiskLevel:
    """
    Map risk score (0-100) to risk level category.

    Args:
        risk_score: Overall risk score

    Returns:
        RiskLevel enum
    """
    if risk_score < 34:
        return RiskLevel.LOW
    elif risk_score < 67:
        return RiskLevel.MEDIUM
    else:
        return RiskLevel.HIGH


def generate_full_explanation(
    overall_risk_score: float,
    risk_level: RiskLevel,
    top_factors: List[RiskFactor],
    confidence_level: ConfidenceLevel
) -> str:
    """
    Generate human-readable explanation of risk assessment.

    Args:
        overall_risk_score: Overall risk score (0-100)
        risk_level: Risk level category
        top_factors: Top 3 contributing risk factors
        confidence_level: Assessment confidence level

    Returns:
        Plain-language explanation string
    """
    explanation = f"Overall Risk: {risk_level.value} ({overall_risk_score:.1f}/100)\n"
    explanation += f"Confidence: {confidence_level.value}\n\n"

    if top_factors:
        explanation += "Top Contributing Factors:\n"
        for i, factor in enumerate(top_factors[:3], 1):
            explanation += f"\n{i}. {factor.component.replace('_', ' ').title()} "
            explanation += f"({factor.contribution_percent:.1f}% contribution)\n"
            explanation += f"   Measured: {factor.measured_value:.2f}, "
            explanation += f"Guideline: {factor.guideline_value}\n"
            explanation += f"   Impact: {factor.impact_description}\n"
            explanation += f"   Severity: {factor.severity}\n"
    else:
        explanation += "No significant risk factors identified.\n"

    return explanation


def calculate_risk_score(
    water_quality_data: List[Dict[str, float]],
    community_data: List[Dict],
    infrastructure_data: List[Dict],
    parameters_to_check: Optional[List[str]] = None
) -> RiskAssessment:
    """
    Calculate overall water access risk score with full explainability.

    Risk Formula:
        risk_score = (water_quality * 0.35) + (access_distance * 0.30) +
                     (infrastructure * 0.25) + (population_pressure * 0.10)

    Args:
        water_quality_data: List of water quality measurements
        community_data: List of community location/population data
        infrastructure_data: List of water infrastructure data
        parameters_to_check: Specific WHO parameters to check

    Returns:
        RiskAssessment object with complete analysis
    """
    # Calculate component scores
    wq_risk, wq_factors = calculate_water_quality_risk(
        water_quality_data,
        parameters_to_check
    )

    ad_risk, ad_factors = calculate_access_distance_risk(
        community_data,
        infrastructure_data
    )

    infra_risk, infra_factors = calculate_infrastructure_risk(
        infrastructure_data
    )

    pop_risk, pop_factors = calculate_population_pressure_risk(
        community_data,
        infrastructure_data
    )

    # Calculate weighted overall risk score
    overall_risk = (
        wq_risk * 0.35 +
        ad_risk * 0.30 +
        infra_risk * 0.25 +
        pop_risk * 0.10
    )

    # Combine all risk factors and sort by contribution
    all_factors = wq_factors + ad_factors + infra_factors + pop_factors
    all_factors.sort(key=lambda x: x.contribution_percent, reverse=True)
    top_factors = all_factors[:3]

    # Calculate sample size and confidence
    sample_size = (
        len(water_quality_data) +
        len(community_data) +
        len(infrastructure_data)
    )
    confidence = calculate_confidence_level(sample_size)

    # Determine risk level
    risk_level = determine_risk_level(overall_risk)

    # Generate explanation
    explanation = generate_full_explanation(
        overall_risk,
        risk_level,
        top_factors,
        confidence
    )

    # Create component scores dictionary
    component_scores = {
        "water_quality": wq_risk,
        "access_distance": ad_risk,
        "infrastructure": infra_risk,
        "population_pressure": pop_risk
    }

    return RiskAssessment(
        overall_risk_score=overall_risk,
        risk_level=risk_level,
        confidence_level=confidence,
        component_scores=component_scores,
        top_factors=top_factors,
        explanation=explanation,
        algorithm_version="1.0.0",
        sample_size=sample_size
    )


# Example usage
if __name__ == "__main__":
    # Sample data for testing
    sample_water_quality = [
        {"arsenic": 0.03, "nitrate": 45.0, "e_coli": 5, "ph": 7.2},
        {"arsenic": 0.02, "nitrate": 55.0, "e_coli": 2, "ph": 7.5}
    ]

    sample_community = [
        {"latitude": 34.05, "longitude": -118.25, "population": 500},
        {"latitude": 34.06, "longitude": -118.26, "population": 300}
    ]

    sample_infrastructure = [
        {"latitude": 34.05, "longitude": -118.24, "functionality": "functional"},
        {"latitude": 34.07, "longitude": -118.27, "functionality": "needs_repair"}
    ]

    # Calculate risk assessment
    assessment = calculate_risk_score(
        water_quality_data=sample_water_quality,
        community_data=sample_community,
        infrastructure_data=sample_infrastructure
    )

    print(assessment.explanation)
    print(f"\nComponent Scores:")
    for component, score in assessment.component_scores.items():
        print(f"  {component}: {score:.1f}/100")
