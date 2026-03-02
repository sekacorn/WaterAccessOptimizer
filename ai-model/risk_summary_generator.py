"""
Risk Summary Generator - Version 1.0.0
Generates human-readable risk assessment summaries WITHOUT LLM (template-based)
Iteration 6 - Agents 17 (AI/ML) + 18 (LLM Safety)

NOTE: This is a template-based system for MVP. LLM-powered natural language
generation will be added in V1 (Agent 18), but safety-first approach requires
deterministic, auditable summaries for MVP.
"""

from typing import Dict, List, Optional
from dataclasses import dataclass
from datetime import datetime
import risk_scoring


@dataclass
class RiskSummaryOptions:
    """Options for customizing risk summary generation"""
    include_citations: bool = True
    include_recommendations: bool = True
    include_confidence: bool = True
    include_data_sources: bool = True
    language: str = "en"  # Future: support multiple languages
    audience: str = "general"  # general, technical, or policy


class RiskSummaryGenerator:
    """
    Template-based risk summary generator (MVP approach).

    Safety principles from Agent 18:
    - Never invent data or citations
    - Always cite sources with IDs and dates
    - Express uncertainty when data quality is low
    - Recommend expert consultation for decisions
    - NEVER give "guaranteed safe" advice
    """

    def __init__(self):
        self.who_guidelines = risk_scoring.WHO_GUIDELINES

    def generate_summary(
        self,
        assessment: risk_scoring.RiskAssessment,
        options: Optional[RiskSummaryOptions] = None
    ) -> Dict[str, str]:
        """
        Generate plain-language summary of risk assessment.

        Args:
            assessment: RiskAssessment object from risk_scoring.py
            options: Customization options

        Returns:
            Dictionary with summary sections
        """
        if options is None:
            options = RiskSummaryOptions()

        # Generate each section
        summary = {
            "title": self._generate_title(assessment),
            "overview": self._generate_overview(assessment),
            "top_factors": self._generate_top_factors(assessment, options),
            "component_breakdown": self._generate_component_breakdown(assessment),
            "confidence_statement": self._generate_confidence_statement(assessment),
            "recommendations": self._generate_recommendations(assessment) if options.include_recommendations else "",
            "disclaimer": self._generate_disclaimer(),
            "metadata": self._generate_metadata(assessment)
        }

        # Combine into full text
        summary["full_text"] = self._combine_sections(summary, options)

        return summary

    def _generate_title(self, assessment: risk_scoring.RiskAssessment) -> str:
        """Generate title for summary."""
        risk_level = assessment.risk_level.value
        score = assessment.overall_risk_score

        return f"Water Access Risk Assessment: {risk_level} ({score:.1f}/100)"

    def _generate_overview(self, assessment: risk_scoring.RiskAssessment) -> str:
        """Generate overview paragraph."""
        risk_level = assessment.risk_level.value
        score = assessment.overall_risk_score
        confidence = assessment.confidence_level.value

        # Risk level descriptions
        risk_descriptions = {
            "LOW": "Water access conditions are generally acceptable, though some areas for improvement may exist.",
            "MEDIUM": "Water access requires monitoring and improvements to reduce health risks and improve reliability.",
            "HIGH": "Water access requires immediate attention due to significant health risks or access challenges."
        }

        description = risk_descriptions.get(risk_level, "")

        overview = f"This assessment indicates a **{risk_level} risk level** (score: {score:.1f}/100). "
        overview += description
        overview += f"\n\n**Assessment Confidence**: {confidence}"

        if confidence in ["LOW", "NONE"]:
            overview += " - Limited data available. Additional monitoring recommended for more accurate assessment."
        elif confidence == "MEDIUM":
            overview += " - Moderate data available. Results provide reasonable indication of conditions."
        else:  # HIGH
            overview += " - Comprehensive data available. Results are reliable for decision-making support."

        return overview

    def _generate_top_factors(
        self,
        assessment: risk_scoring.RiskAssessment,
        options: RiskSummaryOptions
    ) -> str:
        """Generate top contributing factors section."""
        if not assessment.top_factors:
            return "No significant risk factors identified in this assessment."

        text = "### Top Contributing Risk Factors\n\n"
        text += "The following factors contribute most to the overall risk score:\n\n"

        for i, factor in enumerate(assessment.top_factors[:3], 1):
            text += f"**{i}. {factor.component.replace('_', ' ').title()}** "
            text += f"({factor.contribution_percent:.1f}% of overall risk)\n"

            # Measured vs guideline
            if isinstance(factor.guideline_value, str):
                # pH range
                text += f"- **Measured value**: {factor.measured_value:.2f}\n"
                text += f"- **Guideline/Standard**: {factor.guideline_value}\n"
            else:
                text += f"- **Measured value**: {factor.measured_value:.2f}\n"
                text += f"- **WHO Guideline**: {factor.guideline_value}\n"

                # Calculate exceedance
                if factor.guideline_value > 0:
                    exceedance = ((factor.measured_value - factor.guideline_value) / factor.guideline_value) * 100
                    if exceedance > 0:
                        text += f"- **Exceedance**: {exceedance:.0f}% above guideline\n"

            # Health impact
            text += f"- **Impact**: {factor.impact_description}\n"
            text += f"- **Severity**: {factor.severity}\n"
            text += "\n"

        return text

    def _generate_component_breakdown(
        self,
        assessment: risk_scoring.RiskAssessment
    ) -> str:
        """Generate component scores breakdown."""
        text = "### Component Risk Scores\n\n"
        text += "The overall risk score is calculated from four weighted components:\n\n"

        components = [
            ("water_quality", "Water Quality", 35, "Based on WHO water quality guidelines"),
            ("access_distance", "Access Distance", 30, "Based on JMP service ladder (distance to water source)"),
            ("infrastructure", "Infrastructure", 25, "Based on functionality of water facilities"),
            ("population_pressure", "Population Pressure", 10, "Based on people per functional water point")
        ]

        for comp_key, comp_name, weight, description in components:
            score = assessment.component_scores.get(comp_key, 0)

            # Determine component risk level
            if score < 34:
                comp_level = "LOW"
            elif score < 67:
                comp_level = "MEDIUM"
            else:
                comp_level = "HIGH"

            text += f"**{comp_name}** ({weight}% weight): {score:.1f}/100 - {comp_level}\n"
            text += f"  - {description}\n\n"

        return text

    def _generate_confidence_statement(
        self,
        assessment: risk_scoring.RiskAssessment
    ) -> str:
        """Generate confidence level explanation."""
        confidence = assessment.confidence_level.value
        samples = assessment.sample_size

        text = "### Data Confidence\n\n"
        text += f"**Confidence Level**: {confidence}\n"
        text += f"**Sample Size**: {samples} data points\n\n"

        confidence_explanations = {
            "NONE": (
                "No data available for assessment. This score uses default risk assumptions. "
                "**Action Required**: Collect baseline data through field surveys or water quality testing."
            ),
            "LOW": (
                f"Limited data ({samples} samples) provides preliminary indication only. "
                "Results should be validated with additional monitoring. "
                "**Recommendation**: Conduct comprehensive water quality and infrastructure surveys."
            ),
            "MEDIUM": (
                f"Moderate data ({samples} samples) provides reasonable assessment. "
                "Results can guide preliminary planning, but additional data would improve accuracy. "
                "**Recommendation**: Continue regular monitoring and expand coverage area."
            ),
            "HIGH": (
                f"Comprehensive data ({samples} samples) provides reliable assessment. "
                "Results can support decision-making when combined with expert review. "
                "**Recommendation**: Maintain regular monitoring schedule to track changes."
            )
        }

        text += confidence_explanations.get(confidence, "")

        return text

    def _generate_recommendations(
        self,
        assessment: risk_scoring.RiskAssessment
    ) -> str:
        """Generate recommendations based on risk level and factors."""
        risk_level = assessment.risk_level.value

        text = "### Recommended Actions\n\n"

        if risk_level == "HIGH":
            text += "**Priority Level**: URGENT - Immediate action recommended\n\n"
            text += "High-risk conditions require prompt intervention to protect public health and improve water access. Consider the following actions:\n\n"
        elif risk_level == "MEDIUM":
            text += "**Priority Level**: MODERATE - Action recommended within 3-6 months\n\n"
            text += "Medium-risk conditions indicate need for improvements to prevent escalation. Consider the following actions:\n\n"
        else:  # LOW
            text += "**Priority Level**: LOW - Monitor and maintain current conditions\n\n"
            text += "Low-risk conditions indicate acceptable standards, though continuous improvement is always beneficial. Consider the following actions:\n\n"

        # Generate specific recommendations based on top factors
        if assessment.top_factors:
            for i, factor in enumerate(assessment.top_factors[:3], 1):
                component = factor.component

                if component == "water_quality":
                    text += f"{i}. **Address Water Quality Issues**\n"
                    text += "   - Conduct detailed water quality testing to identify all contaminants\n"
                    text += "   - Install appropriate water treatment systems (filtration, UV treatment, etc.)\n"
                    text += "   - Identify and eliminate contamination sources if possible\n"
                    text += "   - Provide alternative water sources if treatment is not feasible\n\n"

                elif component == "access_distance":
                    text += f"{i}. **Improve Water Access**\n"
                    text += "   - Construct new water points closer to communities\n"
                    text += "   - Rehabilitate non-functional water sources\n"
                    text += "   - Consider piped water systems for high-population areas\n"
                    text += "   - Improve access roads to existing water sources\n\n"

                elif component == "infrastructure":
                    text += f"{i}. **Rehabilitate Water Infrastructure**\n"
                    text += "   - Assess and repair non-functional water facilities\n"
                    text += "   - Establish regular maintenance schedules\n"
                    text += "   - Train local communities in basic maintenance\n"
                    text += "   - Replace aging infrastructure where repair is not cost-effective\n\n"

                elif component == "population_pressure":
                    text += f"{i}. **Expand Water Supply Capacity**\n"
                    text += "   - Construct additional water points to reduce overcrowding\n"
                    text += "   - Increase capacity of existing water sources\n"
                    text += "   - Implement water demand management strategies\n"
                    text += "   - Consider seasonal variation in planning\n\n"

        return text

    def _generate_disclaimer(self) -> str:
        """Generate safety disclaimer (per Agent 18)."""
        text = "### Important Disclaimer\n\n"
        text += "**This assessment is a data analysis tool, not a substitute for expert judgment.**\n\n"
        text += "**WARNING - Safety Notice**:\n"
        text += "- This analysis provides risk indicators based on available data and WHO guidelines\n"
        text += "- It does NOT constitute medical or public health advice\n"
        text += "- Water safety decisions require review by qualified water quality professionals\n"
        text += "- Local regulatory standards and vulnerable populations must be considered\n"
        text += "- No guarantee of water safety can be made based on this assessment alone\n\n"
        text += "**Recommended Next Steps**:\n"
        text += "1. Share this assessment with certified water quality experts\n"
        text += "2. Consult local health authorities and regulatory bodies\n"
        text += "3. Conduct comprehensive testing if not already done\n"
        text += "4. Develop intervention plans with community participation\n"
        text += "5. Monitor conditions regularly after interventions\n"

        return text

    def _generate_metadata(
        self,
        assessment: risk_scoring.RiskAssessment
    ) -> str:
        """Generate metadata section."""
        text = "### Assessment Metadata\n\n"
        text += f"- **Assessment Date**: {datetime.now().strftime('%Y-%m-%d %H:%M UTC')}\n"
        text += f"- **Algorithm Version**: {assessment.algorithm_version}\n"
        text += f"- **Sample Size**: {assessment.sample_size} data points\n"
        text += f"- **Confidence Level**: {assessment.confidence_level.value}\n"
        text += "- **Methodology**: Rule-based risk scoring with WHO guidelines and JMP service ladder\n"
        text += "- **Reference**: docs/WHO_GUIDELINES_REFERENCE.md\n"

        return text

    def _combine_sections(
        self,
        summary: Dict[str, str],
        options: RiskSummaryOptions
    ) -> str:
        """Combine all sections into full text summary."""
        full_text = f"# {summary['title']}\n\n"
        full_text += f"{summary['overview']}\n\n"
        full_text += f"{summary['top_factors']}\n"
        full_text += f"{summary['component_breakdown']}\n"

        if options.include_confidence:
            full_text += f"{summary['confidence_statement']}\n"

        if options.include_recommendations:
            full_text += f"{summary['recommendations']}\n"

        full_text += f"{summary['disclaimer']}\n"

        if options.include_data_sources:
            full_text += f"{summary['metadata']}\n"

        return full_text

    def generate_short_summary(
        self,
        assessment: risk_scoring.RiskAssessment,
        max_words: int = 100
    ) -> str:
        """
        Generate short summary suitable for dashboards or notifications.

        Args:
            assessment: RiskAssessment object
            max_words: Maximum word count

        Returns:
            Short summary string
        """
        risk_level = assessment.risk_level.value
        score = assessment.overall_risk_score
        confidence = assessment.confidence_level.value

        # Start with overview
        summary = f"{risk_level} risk ({score:.0f}/100). "

        # Add top factor if available
        if assessment.top_factors:
            top_factor = assessment.top_factors[0]
            component = top_factor.component.replace('_', ' ')
            summary += f"Primary concern: {component}. "

        # Add confidence
        summary += f"Confidence: {confidence}"

        if confidence in ["LOW", "NONE"]:
            summary += " - Limited data, additional monitoring recommended."
        elif confidence == "MEDIUM":
            summary += " - Moderate data available."
        else:
            summary += " - Comprehensive data."

        return summary

    def generate_json_summary(
        self,
        assessment: risk_scoring.RiskAssessment
    ) -> Dict:
        """
        Generate structured JSON summary for API responses.

        Args:
            assessment: RiskAssessment object

        Returns:
            Dictionary suitable for JSON serialization
        """
        return {
            "overall_risk": {
                "score": round(assessment.overall_risk_score, 1),
                "level": assessment.risk_level.value,
                "description": self._get_risk_description(assessment.risk_level)
            },
            "confidence": {
                "level": assessment.confidence_level.value,
                "sample_size": assessment.sample_size,
                "description": self._get_confidence_description(assessment.confidence_level)
            },
            "top_factors": [
                {
                    "rank": i + 1,
                    "component": factor.component,
                    "contribution_percent": round(factor.contribution_percent, 1),
                    "measured_value": round(factor.measured_value, 2),
                    "guideline_value": factor.guideline_value if isinstance(factor.guideline_value, str) else round(factor.guideline_value, 2),
                    "impact_description": factor.impact_description,
                    "severity": factor.severity
                }
                for i, factor in enumerate(assessment.top_factors[:3])
            ],
            "component_scores": {
                k: round(v, 1)
                for k, v in assessment.component_scores.items()
            },
            "metadata": {
                "algorithm_version": assessment.algorithm_version,
                "generated_at": datetime.now().isoformat(),
                "sample_size": assessment.sample_size
            }
        }

    def _get_risk_description(self, risk_level: risk_scoring.RiskLevel) -> str:
        """Get description for risk level."""
        descriptions = {
            risk_scoring.RiskLevel.LOW: "Water access conditions are generally acceptable",
            risk_scoring.RiskLevel.MEDIUM: "Water access requires monitoring and improvements",
            risk_scoring.RiskLevel.HIGH: "Water access requires immediate attention"
        }
        return descriptions.get(risk_level, "")

    def _get_confidence_description(
        self,
        confidence: risk_scoring.ConfidenceLevel
    ) -> str:
        """Get description for confidence level."""
        descriptions = {
            risk_scoring.ConfidenceLevel.NONE: "No data available",
            risk_scoring.ConfidenceLevel.LOW: "Limited data, preliminary indication only",
            risk_scoring.ConfidenceLevel.MEDIUM: "Moderate data, reasonable assessment",
            risk_scoring.ConfidenceLevel.HIGH: "Comprehensive data, reliable assessment"
        }
        return descriptions.get(confidence, "")


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
    assessment = risk_scoring.calculate_risk_score(
        water_quality_data=sample_water_quality,
        community_data=sample_community,
        infrastructure_data=sample_infrastructure
    )

    # Generate summaries
    generator = RiskSummaryGenerator()

    print("=" * 80)
    print("FULL SUMMARY")
    print("=" * 80)
    summary = generator.generate_summary(assessment)
    print(summary["full_text"])

    print("\n" + "=" * 80)
    print("SHORT SUMMARY (for dashboards)")
    print("=" * 80)
    short = generator.generate_short_summary(assessment)
    print(short)

    print("\n" + "=" * 80)
    print("JSON SUMMARY (for API)")
    print("=" * 80)
    import json
    json_summary = generator.generate_json_summary(assessment)
    print(json.dumps(json_summary, indent=2))
