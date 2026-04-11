# ML/LLM Integration Roadmap
> Status: future roadmap, not shipped functionality.

**Version:** 1.0.0
**Last Updated:** 2026-02-03
**Status:** POST-MVP (V1/V2 Features)
**Iteration:** 6 - AI/ML & LLM Planning

---

## Executive Summary

This document outlines the roadmap for integrating Machine Learning and Large Language Model capabilities into the WaterAccessOptimizer platform. These features are **POST-MVP** enhancements that build on the deterministic risk scoring engine delivered in the MVP.

**Key Principle**: AI/ML enhances the platform but is never required. The deterministic optimization engine (risk scoring, facility location optimization) works completely standalone and degrades gracefully if ML services are unavailable.

---

## MVP vs V1 vs V2 Feature Breakdown

### [X]MVP (Current - Iteration 5 Complete)

**Deterministic Optimization Engine** (ai-model/risk_scoring.py):
- Rule-based risk scoring with WHO guidelines
- 4-component weighted model (water_quality 35%, access_distance 30%, infrastructure 25%, population_pressure 10%)
- Top-3 factor explainability
- Confidence levels based on sample size
- Template-based summary generation (ai-model/risk_summary_generator.py)

**Why Deterministic First**:
- Explainable and auditable for regulatory compliance
- Works in offline/field environments
- No training data required
- Transparent to domain experts
- Fast inference (<100ms)

### 🔄 V1 (Sprint 2-4: Q2 2026)

**ML Service** (new: ml-service/):
- **Model 1: Water Availability Forecasting** (Facebook Prophet)
  - Predict water availability 3, 6, 12 months ahead
  - Uses historical flow rate data (minimum: 6 months)
  - Provides confidence intervals
  - Fallback: Historical averages

- **Model 2: Water Quality Anomaly Detection** (Isolation Forest)
  - Flag unusual water quality measurements
  - Detects contamination events
  - Fast inference (<10ms)
  - Fallback: WHO guideline threshold checks

- **Model 3: Community Demand Prediction** (Linear Regression)
  - Estimate future water demand
  - Uses population growth + historical consumption
  - Explainable coefficients
  - Fallback: WHO guideline (20 L/person/day) × projected population

**Infrastructure**:
- MLflow for model registry
- MinIO/S3 for model storage
- Prometheus metrics for monitoring
- Automated retraining pipeline (Airflow)

**Why V1**:
- Unsupervised models require no labeled data
- Provides actionable forecasts for planning
- Enhances but doesn't replace deterministic engine

###  V2 (Sprint 5+: Q3-Q4 2026)

**LLM Service** (new: llm-service/):
- Natural language query interface ("Why is Community A high risk?")
- Risk assessment explanations with citations
- Data troubleshooting assistance
- Multi-language support

**Advanced ML Models**:
- Image classification for infrastructure condition assessment
- Spatial interpolation for unmeasured areas
- Hybrid rule-ML risk models

**Why V2**:
- Requires comprehensive safety mechanisms (Agent 18)
- LLM API costs and rate limits need business model validation
- Needs vector database (pgvector) and embedding infrastructure
- Regulatory review for LLM-generated content

---

## Architecture Evolution

### MVP Architecture (Current)

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (Spring Boot)              │
│                  JWT Auth, Rate Limiting                    │
└────────────┬────────────────────────────────┬───────────────┘
             │                                │
             ▼                                ▼
┌────────────────────────┐       ┌────────────────────────────┐
│  Data Service          │       │  Worker Service (MVP)      │
│  (FastAPI - Future)    │       │  (ai-model/ - Current)     │
│                        │       │                            │
│  • Data Upload         │       │  • Risk Scoring ✓          │
│  • Validation          │       │  • Summary Generation ✓    │
│  • Storage             │       │                            │
└────────────┬───────────┘       └────────────┬───────────────┘
             │                                │
             └────────────────┬───────────────┘
                              ▼
                 ┌────────────────────────────┐
                 │  PostgreSQL + PostGIS      │
                 └────────────────────────────┘
```

### V1 Architecture (ML Service Added)

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (Spring Boot)              │
└────────┬────────────────────────────────┬──────────┬────────┘
         │                                │          │
         ▼                                ▼          ▼
   ┌──────────┐              ┌──────────────┐  ┌──────────────┐
   │   Data   │              │    Worker    │  │  ML Service  │
   │  Service │              │   Service    │  │  (FastAPI)   │
   │          │              │              │  │              │
   │          │              │  • Risk ✓    │  │  • Prophet   │
   │          │              │  • Summary ✓ │  │  • Isolation │
   │          │              │              │  │  • Linear    │
   └────┬─────┘              └───────┬──────┘  └──────┬───────┘
        │                            │                │
        └────────────────┬───────────┴────────────────┘
                         ▼
            ┌────────────────────────────┐
            │  PostgreSQL + PostGIS      │
            └────────────┬───────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │  MinIO / S3                │
            │  (Model Artifacts)         │
            └────────────────────────────┘
```

### V2 Architecture (LLM Service Added)

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                            │
└────┬────────────┬───────────────┬────────────┬──────────────┘
     │            │               │            │
     ▼            ▼               ▼            ▼
┌────────┐  ┌──────────┐   ┌──────────┐  ┌────────────────┐
│  Data  │  │  Worker  │   │    ML    │  │  LLM Service   │
│Service │  │ Service  │   │ Service  │  │  (FastAPI)     │
│        │  │          │   │          │  │                │
│        │  │  • Risk  │   │  • Models│  │  • NL Query    │
│        │  │  • Summ  │   │  • Pred  │  │  • Explain     │
└───┬────┘  └────┬─────┘   └────┬─────┘  └───────┬────────┘
    │            │              │                 │
    └────────────┴──────┬───────┴─────────────────┘
                        ▼
            ┌───────────────────────────┐
            │  PostgreSQL + PostGIS     │
            │  + pgvector (embeddings)  │
            └───────────┬───────────────┘
                        │
            ┌───────────┴───────────────┐
            │                           │
            ▼                           ▼
  ┌──────────────────┐       ┌──────────────────┐
  │  MinIO / S3      │       │  Redis Cache     │
  │  (Models)        │       │  (Embeddings)    │
  └──────────────────┘       └──────────────────┘
```

---

## V1 Implementation Plan: ML Service

### Model 1: Water Availability Forecasting

**Algorithm**: Facebook Prophet
**Purpose**: Predict water availability 3, 6, 12 months ahead
**Status**: V1 (Sprint 2)

**Data Requirements**:
```sql
-- Minimum 6 months of flow rate measurements
SELECT
    measurement_date,
    flow_rate,
    measurement_unit
FROM hydro_data
WHERE location_id = :location_id
    AND parameter_name = 'flow_rate'
    AND measurement_date > NOW() - INTERVAL '6 months'
ORDER BY measurement_date
```

**Training Script**: `ml-service/training/train_prophet_model.py`
**Model Storage**: `s3://water-optimizer-models/prophet-water-availability/v1.0.0/`
**Metadata**: Algorithm version, training data hash, hyperparameters, evaluation metrics

**API Endpoint**:
```
POST /api/ml/predict
{
  "model_name": "prophet-water-availability",
  "model_version": "1.0.0",
  "input_data": {
    "location_id": "hydro-123",
    "forecast_horizon_days": 90
  }
}

Response:
{
  "status": "success",
  "predictions": [
    {
      "date": "2026-05-01",
      "predicted_flow_rate": 110.3,
      "lower_bound": 95.2,
      "upper_bound": 125.4
    }
  ],
  "model_version": "1.0.0",
  "confidence_level": 0.8
}
```

**Fallback**: If model unavailable or insufficient data, return historical average with disclaimer

---

### Model 2: Water Quality Anomaly Detection

**Algorithm**: Isolation Forest (unsupervised)
**Purpose**: Flag unusual water quality measurements
**Status**: V1 (Sprint 3)

**Training Approach**:
- Train on historical "normal" measurements
- Identify outliers in real-time
- No labeled anomalies required

**Features**:
- Arsenic, nitrate, fluoride, lead, TDS, pH, turbidity, E. coli
- Geographic features (lat, lon)
- Temporal features (month, season)

**API Endpoint**:
```
POST /api/ml/detect-anomaly
{
  "measurement_id": "meas-456",
  "parameters": {
    "arsenic": 125.5,
    "nitrate": 45.0,
    "ph": 7.2
  }
}

Response:
{
  "is_anomaly": true,
  "anomaly_score": 0.73,
  "anomaly_reasons": [
    "Arsenic significantly higher than regional baseline",
    "Combination of arsenic + nitrate unusual for this source type"
  ],
  "confidence": 0.85,
  "recommendation": "FLAG_FOR_REVIEW"
}
```

**Fallback**: Simple WHO guideline threshold checks

---

### Model 3: Community Demand Prediction

**Algorithm**: Linear Regression + Seasonal Decomposition
**Purpose**: Estimate future water demand
**Status**: V1 (Sprint 3)

**Features**:
- Current population
- Population growth rate
- Historical consumption (liters/month)
- Seasonal factors (month of year)

**API Endpoint**:
```
POST /api/ml/predict-demand
{
  "community_id": "comm-123",
  "forecast_months": 12
}

Response:
{
  "predictions": [
    {
      "month": "2026-05",
      "predicted_demand_liters_per_day": 110500,
      "per_capita_liters_per_day": 21.2,
      "confidence_interval": [98000, 123000]
    }
  ],
  "factors": {
    "population_growth_contribution": 0.15,
    "seasonal_contribution": 0.05
  }
}
```

**Fallback**: WHO guideline (20 L/person/day) × projected population

---

### Model Versioning & Reproducibility (V1)

**Version Format**: `{algorithm}-v{major}.{minor}.{patch}`
Examples: `prophet-v1.0.0`, `isolation-forest-v1.1.0`

**Metadata Stored**:
```json
{
  "model_name": "prophet-water-availability",
  "version": "1.0.0",
  "algorithm": "Facebook Prophet",
  "created_at": "2026-04-15T14:30:00Z",
  "training_dataset": {
    "date_range": "2024-01-01 to 2026-03-31",
    "row_count": 15423,
    "data_hash": "sha256:a1b2c3d4..."
  },
  "hyperparameters": {
    "yearly_seasonality": true,
    "changepoint_prior_scale": 0.05
  },
  "evaluation_metrics": {
    "mae": 12.5,
    "rmse": 18.3
  }
}
```

**Reproducibility Requirements**:
1. Training data snapshot (SHA-256 hash)
2. requirements.txt with pinned versions
3. Fixed random seed
4. Version-controlled feature engineering code

---

### Monitoring & Fallback (V1)

**Prometheus Metrics**:
```python
ml_predictions_total = Counter('ml_predictions_total', ['model_name', 'status'])
ml_prediction_latency = Histogram('ml_prediction_duration_seconds', ['model_name'])
ml_fallback_triggered = Counter('ml_fallback_triggered_total', ['model_name', 'reason'])
ml_model_drift_score = Gauge('ml_model_drift_score', ['model_name'])
```

**Fallback Hierarchy**:
1. Primary: Latest production model (e.g., prophet-v1.2.0)
2. Fallback 1: Previous stable version (e.g., prophet-v1.1.0)
3. Fallback 2: Simple statistical method (historical average)
4. Fallback 3: Domain heuristics (WHO guidelines)
5. Fallback 4: Return error with explanation

**Grafana Dashboard**:
- Prediction volume (requests/min)
- Latency (p50, p95, p99)
- Error rate
- Fallback rate
- Model drift score

---

## V2 Implementation Plan: LLM Service

### Natural Language Query Interface

**Status**: V2 (Sprint 5+)
**Dependencies**: V1 ML service, pgvector extension, embedding model

**Allowed Use Cases** (per Agent 18):
1. **Explain Risk Assessments**: "Why is Community A rated high risk?"
2. **Query Data**: "Show me communities with arsenic > 50 µg/L"
3. **Troubleshoot**: "Why did my upload fail?"

**FORBIDDEN Use Cases**:
1. ❌ Making up citations or data
2. ❌ Giving "guaranteed safe" water advice
3. ❌ Inventing statistics or WHO reports
4. ❌ Making safety decisions

### Safety Mechanisms (V2)

**Pre-Generation Safety Checks**:
```python
FORBIDDEN_PATTERNS = [
    r"is (it|the water|this) safe to (drink|consume)",
    r"can (i|we) drink",
    r"guaranteed safe",
    r"tell (me|us) what to do"
]
```

**Post-Generation Output Validation**:
```python
FORBIDDEN_OUTPUT_PHRASES = [
    "the water is safe",
    "it's safe to drink",
    "guaranteed",
    "completely safe"
]
```

**Citation Verification**:
- All cited IDs verified in database
- Factual statements require citations
- No invented WHO reports or studies

### RAG Architecture (V2)

**Vector Embeddings** (stored in PostgreSQL with pgvector):
1. Dataset descriptions (for "find relevant data")
2. Assessment summaries (for "explain risk")
3. WHO guidelines (for "what does WHO say")

**Embedding Model**: `text-embedding-3-small` (OpenAI) or `all-MiniLM-L6-v2` (local)

**Retrieval**:
```sql
SELECT
    id,
    text,
    metadata,
    1 - (embedding <=> query_embedding) AS similarity_score
FROM embeddings
WHERE 1 - (embedding <=> query_embedding) > 0.7
ORDER BY embedding <=> query_embedding
LIMIT 5
```

**Prompt Assembly**:
```
System Prompt: You are a water access data assistant. NEVER invent facts...
Context: [Retrieved data with provenance]
User Query: {query}
Instructions: Cite sources with IDs and dates. Express uncertainty...
```

### API Endpoints (V2)

**POST /api/llm/query**:
```json
Request:
{
  "query": "Why is Community A rated high risk?",
  "context_hints": {
    "assessment_id": "assessment-456"
  }
}

Response:
{
  "status": "success",
  "response": {
    "text": "Community A has HIGH risk (78.3) based on...",
    "citations": [
      {
        "type": "assessment",
        "id": "assessment-456",
        "date": "2024-01-15"
      }
    ]
  },
  "metadata": {
    "model": "claude-3-5-sonnet",
    "tokens_used": 487,
    "safety_checks_passed": true
  }
}
```

**Safety Violation Response**:
```json
{
  "status": "error",
  "error_code": "SAFETY_VIOLATION",
  "message": "Query appears to request medical advice",
  "suggestion": "Consult qualified water quality expert"
}
```

### Audit Logging (V2)

```sql
CREATE TABLE llm_query_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    query TEXT NOT NULL,
    response TEXT,
    citations JSONB,
    model VARCHAR(100),
    tokens_used INTEGER,
    safety_checks_passed BOOLEAN,
    safety_violations JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## Cost Considerations

### V1 ML Service Costs

**Infrastructure**:
- ML service container: ~$50/month (2 vCPU, 4GB RAM)
- MinIO storage: ~$10/month (10GB models)
- MLflow: Runs on same container

**Training**:
- Minimal compute (<$5/month for quarterly retraining)
- Uses existing PostgreSQL data

**Inference**:
- Fast models (<50ms)
- No external API calls
- **Total V1 Cost**: ~$65/month

### V2 LLM Service Costs

**Infrastructure**:
- LLM service container: ~$50/month
- pgvector storage: ~$10/month
- Redis cache: ~$20/month

**LLM API Costs** (OpenAI/Anthropic):
- ~$0.002 per query (Claude Sonnet)
- Assume 1000 queries/month: ~$2/month (MVP users)
- Assume 10,000 queries/month: ~$20/month (growth)

**Embedding Costs**:
- ~$0.0001 per 1000 tokens
- Initial embedding: ~$5 one-time
- Incremental updates: ~$1/month

**Total V2 Cost**: ~$100-150/month (depending on query volume)

### Cost Optimization Strategies

**V1**:
- Cache predictions for 24 hours (reduce redundant inference)
- Batch training (weekly/monthly instead of daily)

**V2**:
- Rate limiting (10 queries/hour for free tier)
- Cache responses for identical queries (24 hours)
- Consider local LLM (Llama 3.1) for cost reduction
- Implement query complexity tiers

---

## Migration Path

### Phase 1: MVP → V1 (No Breaking Changes)

**Step 1**: Deploy ml-service container
**Step 2**: Train initial models on historical data
**Step 3**: Expose /api/ml/predict endpoints
**Step 4**: Update worker-service to call ML service (optional)
**Step 5**: Gradual rollout with fallback enabled

**User Impact**: None - ML predictions are optional enhancements

### Phase 2: V1 → V2 (Additive)

**Step 1**: Install pgvector extension
**Step 2**: Generate embeddings for existing data
**Step 3**: Deploy llm-service container
**Step 4**: Enable /api/llm/query endpoint (beta)
**Step 5**: Collect user feedback
**Step 6**: General availability after safety validation

**User Impact**: New optional feature - existing workflows unchanged

---

## Success Metrics

### V1 ML Service

**Adoption**:
- % of users who enable ML predictions
- Forecasting requests per week

**Accuracy**:
- MAE for water availability forecasts
- Anomaly detection precision/recall
- Demand prediction error

**Reliability**:
- Model availability (uptime)
- Fallback trigger rate (<5% target)
- Inference latency (p95 <500ms)

### V2 LLM Service

**Adoption**:
- Natural language queries per week
- % of users who try LLM feature

**Quality**:
- User feedback rating (thumbs up/down)
- % responses with valid citations
- Safety violation rate (<0.1% target)

**Engagement**:
- Average queries per user
- Repeat usage rate
- Time saved vs manual data exploration

---

## Risks & Mitigation

### V1 ML Service Risks

**Risk 1**: Model drift (changing data distribution)
**Mitigation**: Automated drift detection, quarterly retraining, fallback to previous version

**Risk 2**: Insufficient training data in new regions
**Mitigation**: Require minimum 6 months data, clear messaging about confidence

**Risk 3**: Model unavailability
**Mitigation**: 4-tier fallback hierarchy, graceful degradation

### V2 LLM Service Risks

**Risk 1**: Hallucinated citations or data
**Mitigation**: Post-generation citation verification, forbidden phrase detection

**Risk 2**: Users relying on LLM for safety decisions
**Mitigation**: Safety disclaimers, forbidden pattern blocking, "consult expert" messaging

**Risk 3**: High API costs
**Mitigation**: Rate limiting, response caching, local LLM fallback option

**Risk 4**: GDPR/privacy violations
**Mitigation**: User data scoping (only access own data), audit logging, data retention policies

---

## Decision Checkpoints

### Go/No-Go for V1 (Before Sprint 2)

[X]**GO Criteria**:
- MVP risk scoring works reliably in production
- ≥100 users with ≥6 months historical data
- ML service infrastructure ready (Docker, MLflow, S3)
- Team has ML expertise for model maintenance

❌ **NO-GO Criteria**:
- MVP still has major bugs
- Insufficient training data
- No ML engineer on team
- Budget constraints

### Go/No-Go for V2 (Before Sprint 5)

[X]**GO Criteria**:
- V1 ML service stable (>99% uptime for 3 months)
- User demand for natural language interface (survey)
- Safety mechanisms tested and validated
- LLM API budget approved
- Legal/compliance review complete

❌ **NO-GO Criteria**:
- V1 ML service unreliable
- Users don't request LLM features
- Safety concerns unresolved
- Budget constraints
- Regulatory blockers

---

## Open Questions

1. **Q**: Should we use OpenAI or Anthropic for LLM service?
   **Status**: TBD - Evaluate after agent document review
   **Factors**: Cost, latency, Claude's citation accuracy

2. **Q**: Local LLM (Llama 3.1) vs Cloud API?
   **Status**: TBD - Test in V2 beta
   **Tradeoff**: Local = lower cost but higher latency; Cloud = higher cost but better quality

3. **Q**: Multi-language support priority?
   **Status**: Deferred to V2.1
   **Reason**: English-first for MVP market (Uganda, Kenya)

4. **Q**: Image classification for infrastructure?
   **Status**: Deferred to V2.2
   **Reason**: Requires image upload feature, model training, significant testing

---

## Implementation Checklist

### V1 ML Service (Sprint 2-4)

**Infrastructure**:
- [ ] Set up ml-service FastAPI application
- [ ] Configure MLflow for model registry
- [ ] Set up MinIO/S3 for model storage
- [ ] Configure Docker containers
- [ ] Set up Prometheus monitoring

**Models**:
- [ ] Implement Prophet water availability forecasting
- [ ] Implement Isolation Forest anomaly detection
- [ ] Implement Linear Regression demand prediction
- [ ] Create training scripts with reproducibility
- [ ] Test fallback behavior

**APIs**:
- [ ] Implement POST /api/ml/predict endpoint
- [ ] Implement GET /api/ml/models endpoint
- [ ] Add request validation (Pydantic)
- [ ] Add API documentation (OpenAPI)

**Testing**:
- [ ] Unit tests for model loading
- [ ] Integration tests for predictions
- [ ] Load testing (100 req/min)
- [ ] Fallback scenario testing

### V2 LLM Service (Sprint 5+)

**Infrastructure**:
- [ ] Install pgvector extension on PostgreSQL
- [ ] Set up llm-service FastAPI application
- [ ] Configure Claude/GPT-4 API
- [ ] Set up Redis for caching
- [ ] Configure embedding model

**Context Retrieval**:
- [ ] Generate embeddings for datasets, assessments, guidelines
- [ ] Implement semantic search (vector similarity)
- [ ] Implement structured data retrieval (SQL)
- [ ] Test context retrieval accuracy

**Safety**:
- [ ] Implement pre-generation safety checks
- [ ] Implement post-generation output validation
- [ ] Implement citation verification
- [ ] Test adversarial inputs
- [ ] Set up audit logging

**APIs**:
- [ ] Implement POST /api/llm/query endpoint
- [ ] Implement GET /api/llm/query/{id} endpoint
- [ ] Implement feedback endpoint
- [ ] Add rate limiting
- [ ] Add error handling

**Testing**:
- [ ] Unit tests for safety checks
- [ ] Integration tests for end-to-end queries
- [ ] Adversarial testing (bypass attempts)
- [ ] User acceptance testing

---

## References

- **Agent 17**: AI_ML_SERVICE_ARCHITECTURE.md
- **Agent 18**: LLM_SERVICE_PROMPTING_SAFETY.md
- **Agent 04**: OPTIMIZATION_ENGINE.md (deterministic baseline)
- **Iteration 5**: Risk scoring implementation
- **WHO Guidelines**: docs/WHO_GUIDELINES_REFERENCE.md

---

## Contact

For questions about ML/LLM roadmap:
- **Technical**: Review Agent 17 & 18 documents
- **Implementation**: ai-model/risk_scoring.py (MVP baseline)
- **Database**: database/postgres/V1_SCHEMA_MVP.sql

**Last Reviewed**: 2026-02-03
**Next Review**: 2026-04-01 (Before V1 Go/No-Go decision)
