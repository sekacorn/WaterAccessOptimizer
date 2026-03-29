package com.water.data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.water.data.model.CommunityData;
import com.water.data.model.HydroData;
import com.water.data.model.InfrastructureData;
import com.water.data.repository.CommunityDataRepository;
import com.water.data.repository.HydroDataRepository;
import com.water.data.repository.InfrastructureDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskScoringServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private HydroDataRepository hydroDataRepository;
    private InfrastructureDataRepository infrastructureDataRepository;
    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        hydroDataRepository = mock(HydroDataRepository.class);
        CommunityDataRepository communityDataRepository = mock(CommunityDataRepository.class);
        infrastructureDataRepository = mock(InfrastructureDataRepository.class);
        riskScoringService = new RiskScoringService(
            hydroDataRepository,
            communityDataRepository,
            infrastructureDataRepository,
            new ObjectMapper()
        );
    }

    @Test
    void testLowRiskCommunityScoresBelow33() {
        CommunityData community = community(point(-1.2921, 36.8219), 200);
        when(hydroDataRepository.findWithinRadius(36.8219, -1.2921, 5000.0)).thenReturn(List.of(hydro("arsenic", 0.001)));
        when(hydroDataRepository.findNearest(36.8219, -1.2921, 5)).thenReturn(List.of(hydro("arsenic", 0.001)));
        when(infrastructureDataRepository.findNearest(36.8219, -1.2921, 5)).thenReturn(List.of(facility(point(-1.2922, 36.8220), true)));
        when(infrastructureDataRepository.findWithinRadius(36.8219, -1.2921, 5000.0)).thenReturn(List.of(facility(point(-1.2922, 36.8220), true)));
        when(infrastructureDataRepository.findWithinRadius(36.8219, -1.2921, 2000.0)).thenReturn(List.of(facility(point(-1.2922, 36.8220), true)));

        var result = riskScoringService.calculateRiskScore(community);

        assertTrue(result.getOverallScore() < 33);
    }

    @Test
    void testHighRiskCommunityScoresAbove67() {
        CommunityData community = community(point(-1.2921, 36.8219), 5000);
        when(hydroDataRepository.findWithinRadius(36.8219, -1.2921, 5000.0)).thenReturn(List.of(hydro("arsenic", 0.05)));
        when(hydroDataRepository.findNearest(36.8219, -1.2921, 5)).thenReturn(List.of());
        when(infrastructureDataRepository.findNearest(36.8219, -1.2921, 5)).thenReturn(List.of());
        when(infrastructureDataRepository.findWithinRadius(36.8219, -1.2921, 5000.0)).thenReturn(List.of());
        when(infrastructureDataRepository.findWithinRadius(36.8219, -1.2921, 2000.0)).thenReturn(List.of());

        var result = riskScoringService.calculateRiskScore(community);

        assertTrue(result.getOverallScore() > 67);
    }

    @Test
    void testNullCoordinatesDoesNotThrowNPE() {
        CommunityData community = community(null, 1000);

        var result = assertDoesNotThrow(() -> riskScoringService.calculateRiskScore(community));

        assertEquals(50, result.getOverallScore());
    }

    @Test
    void testRecommendationsGeneratedForHighRisk() {
        CommunityData community = community(point(-1.2921, 36.8219), 5000);
        when(hydroDataRepository.findWithinRadius(36.8219, -1.2921, 5000.0)).thenReturn(List.of(hydro("arsenic", 0.05)));
        when(hydroDataRepository.findNearest(36.8219, -1.2921, 5)).thenReturn(List.of());
        when(infrastructureDataRepository.findNearest(36.8219, -1.2921, 5)).thenReturn(List.of());
        when(infrastructureDataRepository.findWithinRadius(36.8219, -1.2921, 5000.0)).thenReturn(List.of());
        when(infrastructureDataRepository.findWithinRadius(36.8219, -1.2921, 2000.0)).thenReturn(List.of());

        var result = riskScoringService.calculateRiskScore(community);

        assertTrue(result.getExplanationJson().contains("recommendations"));
    }

    private CommunityData community(Point point, int population) {
        CommunityData community = new CommunityData();
        community.setId(1L);
        community.setCoordinates(point);
        community.setPopulation(population);
        return community;
    }

    private HydroData hydro(String parameter, double value) {
        HydroData hydro = new HydroData();
        hydro.setParameterName(parameter);
        hydro.setMeasurementValue(BigDecimal.valueOf(value));
        hydro.setCoordinates(point(-1.2921, 36.8219));
        return hydro;
    }

    private InfrastructureData facility(Point point, boolean operational) {
        InfrastructureData infrastructure = new InfrastructureData();
        infrastructure.setCoordinates(point);
        infrastructure.setOperationalStatus(
            operational ? InfrastructureData.OperationalStatus.OPERATIONAL : InfrastructureData.OperationalStatus.NON_OPERATIONAL
        );
        return infrastructure;
    }

    private Point point(double lat, double lng) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    }
}
