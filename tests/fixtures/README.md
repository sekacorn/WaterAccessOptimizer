# Test Fixtures

This directory contains golden fixtures for deterministic testing of the Water Access Optimizer platform.

## Purpose

Golden fixtures ensure that:
1. **Deterministic outputs**: Same input always produces same output
2. **Regression detection**: Changes to algorithms are immediately detected
3. **Documentation**: Fixtures serve as examples of expected behavior
4. **Test reliability**: Tests don't depend on external services or random data

## Directory Structure

```
fixtures/
├── auth/                    # Authentication & authorization fixtures
│   ├── valid-registration.json
│   ├── invalid-registration.json
│   ├── valid-login.json
│   └── mfa-setup.json
│
├── data/                    # Data upload fixtures
│   ├── community-data-valid.csv
│   ├── community-data-invalid.csv
│   ├── hydro-data-valid.csv
│   ├── infrastructure-data.geojson
│   └── water-quality-measurements.csv
│
├── risk-assessment/         # Risk assessment fixtures
│   ├── input-high-risk.json
│   ├── expected-high-risk.json
│   ├── input-medium-risk.json
│   ├── expected-medium-risk.json
│   ├── input-low-risk.json
│   └── expected-low-risk.json
│
├── llm/                     # LLM service fixtures
│   ├── recommendation-context.json
│   ├── expected-recommendation.json
│   └── mock-responses/
│
└── gis/                     # GIS/visualization fixtures
    ├── sample-communities.geojson
    ├── expected-voronoi.json
    └── expected-heatmap.json
```

## Usage

### Unit Tests

```java
@Test
void testRiskAssessmentCalculation() {
    // Given
    RiskInput input = loadFixture("risk-assessment/input-high-risk.json", RiskInput.class);
    RiskOutput expected = loadFixture("risk-assessment/expected-high-risk.json", RiskOutput.class);

    // When
    RiskOutput actual = riskService.calculateRisk(input);

    // Then
    assertEquals(expected.getCompositeScore(), actual.getCompositeScore(), 0.1);
    assertEquals(expected.getRiskLevel(), actual.getRiskLevel());
}
```

### Integration Tests

```java
@Test
void testDataUpload() {
    // Given
    File csvFile = getFixtureFile("data/community-data-valid.csv");

    // When
    UploadResponse response = uploadFile(csvFile);

    // Then
    assertEquals(2, response.getRecordsImported());
    assertEquals("valid", response.getValidationStatus());
}
```

## Updating Fixtures

When updating fixtures:
1. **Document changes** in commit message
2. **Update expected outputs** for all related tests
3. **Run full test suite** to catch regressions
4. **Review with team** if changing critical fixtures

## Guidelines

### DO:
- [X]Use realistic data values
- [X]Include edge cases (min, max, null values)
- [X]Version control all fixtures
- [X]Keep fixtures focused and minimal
- [X]Add comments explaining non-obvious values

### DON'T:
- ❌ Use production data (PII/sensitive info)
- ❌ Create huge fixtures (use minimal examples)
- ❌ Hardcode timestamps (use relative or mocked times)
- ❌ Include generated IDs (use deterministic IDs)

## Example Test Pattern

```java
@Test
void testDeterministicRiskCalculation() {
    // 1. Load input fixture
    RiskInput input = loadFixture("risk-assessment/input-high-risk.json");

    // 2. Mock any external dependencies
    when(timeProvider.now()).thenReturn(FIXED_TIME);

    // 3. Execute system under test
    RiskOutput actual = riskService.calculateRisk(input);

    // 4. Load expected output fixture
    RiskOutput expected = loadFixture("risk-assessment/expected-high-risk.json");

    // 5. Assert equality (use custom matchers for floating point)
    assertRiskOutputEquals(expected, actual);
}
```

## Maintenance

Fixtures should be reviewed and updated:
- **Quarterly**: Check for outdated data or assumptions
- **On algorithm changes**: Update expected outputs
- **On breaking changes**: Create new fixture versions

---

Last updated: 2024-01-26
