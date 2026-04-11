# WHO Water Quality Guidelines Reference
> Status: standards reference. Use the underlying WHO source and current scoring implementation together when making product or policy decisions.

**Version:** 1.0.0
**Last Updated:** 2026-02-03
**Source:** WHO Guidelines for Drinking-water Quality (4th Edition)
**Iteration:** 5 - Risk Scoring Implementation

---

## Overview

This document provides the WHO (World Health Organization) water quality guidelines used in the WaterAccessOptimizer risk scoring algorithm. These guidelines form the foundation of our water quality risk assessment component (35% weight).

---

## Chemical Parameters

### 1. Arsenic (As)
- **Guideline Value:** 0.01 mg/L (10 μg/L)
- **Unit:** mg/L
- **Health Impact:** Chronic exposure causes cancer (skin, bladder, lung), skin lesions, cardiovascular disease, and developmental effects
- **Severity:** HIGH
- **Sources:** Natural geological deposits, mining activities, industrial discharge, agricultural chemicals
- **Notes:** Long-term exposure even at low levels is carcinogenic. WHO provisional guideline recognizes 0.01 mg/L may be difficult to achieve in some settings.

### 2. Fluoride (F-)
- **Guideline Value:** 1.5 mg/L
- **Unit:** mg/L
- **Health Impact:** Excess causes dental fluorosis (mottling of teeth) and skeletal fluorosis (bone disease)
- **Severity:** MEDIUM
- **Sources:** Natural geological deposits, industrial discharge, dental products
- **Notes:** Both deficiency (<0.5 mg/L) and excess (>1.5 mg/L) pose health risks. Optimal range is 0.5-1.0 mg/L for dental health.

### 3. Nitrate (NO3-)
- **Guideline Value:** 50 mg/L (as nitrate)
- **Unit:** mg/L
- **Health Impact:** Causes methemoglobinemia (blue baby syndrome) in infants, potential carcinogenic effects
- **Severity:** HIGH
- **Sources:** Agricultural fertilizers, animal waste, sewage, natural deposits
- **Notes:** Alternative guideline: 11 mg/L as nitrate-nitrogen (NO3-N). Most critical for infants under 3 months.

### 4. Lead (Pb)
- **Guideline Value:** 0.01 mg/L (10 μg/L)
- **Unit:** mg/L
- **Health Impact:** Neurotoxic, particularly affects brain development in children, cardiovascular effects in adults
- **Severity:** HIGH
- **Sources:** Lead pipes, solder in plumbing, industrial discharge, natural deposits
- **Notes:** No known safe level; guideline is practical limit. Children are most vulnerable. WHO recommends regular monitoring in areas with lead plumbing.

### 5. Mercury (Hg)
- **Guideline Value:** 0.006 mg/L (6 μg/L, for inorganic mercury)
- **Unit:** mg/L
- **Health Impact:** Damages nervous system, kidneys; methylmercury is highly toxic
- **Severity:** HIGH
- **Sources:** Industrial discharge, mining, natural deposits
- **Notes:** Methylmercury guideline is 0.001 mg/L. Bioaccumulates in fish.

### 6. Chromium (Cr)
- **Guideline Value:** 0.05 mg/L (50 μg/L, for total chromium)
- **Unit:** mg/L
- **Health Impact:** Hexavalent chromium (Cr-VI) is carcinogenic; Cr-III is less toxic
- **Severity:** MEDIUM
- **Sources:** Industrial discharge (electroplating, leather tanning), natural deposits
- **Notes:** Hexavalent form is of most concern. Guideline applies to total chromium.

---

## Microbiological Parameters

### 7. E. coli (Escherichia coli)
- **Guideline Value:** 0 CFU/100mL
- **Unit:** Colony Forming Units per 100mL (CFU/100mL)
- **Health Impact:** Indicates fecal contamination; causes diarrheal disease, can be fatal in young children
- **Severity:** HIGH
- **Sources:** Human and animal feces, sewage contamination
- **Notes:** Most specific indicator of fecal contamination. Detection of any E. coli indicates unsafe water.

### 8. Total Coliform Bacteria
- **Guideline Value:** 0 CFU/100mL (for treated water)
- **Unit:** CFU/100mL
- **Health Impact:** Indicates potential presence of pathogens
- **Severity:** MEDIUM
- **Sources:** Soil, vegetation, animal feces
- **Notes:** Less specific than E. coli but useful indicator. Presence suggests treatment failure or contamination.

---

## Physical Parameters

### 9. Turbidity
- **Guideline Value:** 5 NTU (ideally <1 NTU for effective disinfection)
- **Unit:** Nephelometric Turbidity Units (NTU)
- **Health Impact:** High turbidity shields pathogens from disinfection, indicates potential contamination
- **Severity:** MEDIUM
- **Sources:** Suspended particles (clay, silt, algae, microorganisms)
- **Notes:** For effective chlorine disinfection, turbidity should be <1 NTU. Aesthetic acceptability threshold is 5 NTU.

### 10. pH
- **Guideline Value:** 6.5 - 8.5 (operational range)
- **Unit:** pH units (logarithmic scale)
- **Health Impact:** Extreme pH affects taste, causes pipe corrosion (releasing metals), reduces disinfection effectiveness
- **Severity:** LOW
- **Sources:** Natural water chemistry, industrial discharge, treatment processes
- **Notes:** No direct health impact at normal levels, but affects other water quality parameters. Low pH (<6.5) increases corrosion; high pH (>8.5) reduces chlorine effectiveness.

---

## Risk Score Calculation

### Component Weight: 35%
Water quality risk is the most heavily weighted component in the overall risk score due to its direct health impact.

### Calculation Method

For each parameter (except pH):
```
exceedance_ratio = max(0, (measured_value - guideline_value) / guideline_value)
parameter_risk = min(100, exceedance_ratio * 20)
```

For pH (range parameter):
```
if measured < 6.5:
    exceedance_ratio = (6.5 - measured) / 6.5
elif measured > 8.5:
    exceedance_ratio = (measured - 8.5) / 8.5
else:
    exceedance_ratio = 0

parameter_risk = min(100, exceedance_ratio * 20)
```

### Risk Interpretation

| Exceedance | Parameter Risk | Interpretation |
|------------|---------------|----------------|
| 0% | 0 | Meets WHO guideline |
| 50% | 10 | Slight exceedance |
| 100% | 20 | Double the guideline |
| 250% | 50 | Significant exceedance |
| 500% | 100 | Critical exceedance |

### Overall Water Quality Risk
```
water_quality_risk = average(all_parameter_risks)
```

---

## JMP Service Ladder

The WHO/UNICEF Joint Monitoring Programme (JMP) defines service levels for drinking water access:

### Service Levels

#### Safely Managed
- **Definition:** Drinking water from an improved source located on premises, available when needed, and free from contamination
- **Distance:** On-premises or <30 min round trip
- **Quality:** Meets WHO guidelines
- **Availability:** Water available when needed
- **Risk Score:** 0-10

#### Basic
- **Definition:** Drinking water from an improved source with collection time not exceeding 30 minutes for a round trip
- **Distance:** <30 min round trip
- **Quality:** May not be tested
- **Availability:** May have intermittent service
- **Risk Score:** 10-40

#### Limited
- **Definition:** Drinking water from an improved source where collection time exceeds 30 minutes for a round trip
- **Distance:** >30 min round trip (typically 1-5 km)
- **Quality:** Unknown or poor
- **Availability:** May be seasonal
- **Risk Score:** 40-70

#### Unimproved
- **Definition:** Drinking water from an unprotected well or spring
- **Distance:** Variable
- **Quality:** High contamination risk
- **Availability:** Often seasonal
- **Risk Score:** 70-90

#### No Service
- **Definition:** Drinking water from surface water sources
- **Distance:** Often >5 km
- **Quality:** Very high contamination risk
- **Availability:** Seasonal, climate-dependent
- **Risk Score:** 90-100

---

## Improved vs Unimproved Water Sources

### Improved Sources
- Piped water into dwelling, yard, or plot
- Public taps or standpipes
- Tube wells or boreholes
- Protected dug wells
- Protected springs
- Rainwater collection
- Packaged or delivered water

### Unimproved Sources
- Unprotected dug wells
- Unprotected springs
- Surface water (rivers, lakes, ponds, streams, canals)
- Tanker trucks (small volumes)

---

## Data Quality Requirements

### Sample Size Guidelines
- **Minimum:** 3 samples per monitoring point
- **Recommended:** 12 samples per year (monthly)
- **Confidence Levels:**
  - HIGH: >30 samples
  - MEDIUM: 10-30 samples
  - LOW: 1-9 samples
  - NONE: 0 samples

### Sampling Protocols
1. **Water Quality Testing:**
   - Collect samples in sterile containers
   - Analyze within 24 hours (microbiological)
   - Chemical parameters can be preserved and analyzed within 7 days
   - Follow ISO 19458 or equivalent standards

2. **Spatial Data:**
   - Record GPS coordinates (WGS84, EPSG:4326)
   - Accuracy: ±10 meters minimum
   - Verify location on map

3. **Infrastructure Assessment:**
   - Document functionality status
   - Record infrastructure type
   - Note date of last maintenance

---

## Algorithm Version

**Current Version:** 1.0.0
**Release Date:** 2026-02-03
**Implementation:** ai-model/risk_scoring.py

### Version History
- **1.0.0 (2026-02-03):** Initial implementation with 10 WHO parameters, JMP service ladder, 4-component risk model

### Future Enhancements (V1.1.0+)
- Add seasonal variation analysis
- Include trend detection (improving/worsening)
- Add climate vulnerability factors
- Machine learning enhancement (post-MVP)

---

## References

1. **WHO Guidelines for Drinking-water Quality, 4th Edition (2017)**
   - https://www.who.int/publications/i/item/9789241549950

2. **WHO/UNICEF Joint Monitoring Programme (JMP) for Water Supply, Sanitation and Hygiene**
   - https://washdata.org/

3. **WHO Water Quality and Health Strategy 2013-2020**
   - https://www.who.int/water_sanitation_health/publications/2013/water_quality_strategy/en/

4. **ISO 19458:2006 - Water quality sampling for microbiological analysis**

---

## Ethical Communication Guidelines

When presenting risk scores to users:

1. **Use Plain Language:** Avoid technical jargon; explain health impacts clearly
2. **Provide Context:** Compare measured values to guidelines with units
3. **Be Actionable:** Suggest remediation steps (e.g., "Install filtration system")
4. **Acknowledge Uncertainty:** Display confidence levels prominently
5. **Avoid Panic:** Frame HIGH risk as "requires immediate attention" not "catastrophic"
6. **Empower Users:** Provide resources for testing, treatment, and advocacy

### Example Communication

**BAD:**
```
Risk Score: 87 (HIGH)
Arsenic: 0.05 mg/L
```

**GOOD:**
```
Risk Level: HIGH - Requires Immediate Attention (87/100)
Confidence: MEDIUM (15 samples)

Top Risk Factor: Arsenic Contamination
- Measured: 0.05 mg/L (50 μg/L)
- WHO Guideline: 0.01 mg/L (10 μg/L)
- Your water has 5 times the safe level of arsenic
- Health Impact: Long-term exposure increases cancer risk
- Action: Use certified arsenic filter or alternative water source immediately
```

---

## Contact and Updates

For questions about WHO guidelines or risk scoring:
- Technical Documentation: docs/RISK_SCORING_ALGORITHM.md (to be created)
- Implementation: ai-model/risk_scoring.py
- Database Schema: database/postgres/V1_SCHEMA_MVP.sql

**Last Reviewed:** 2026-02-03
**Next Review:** 2026-08-03 (6 months)
