package com.water.data.service;

import com.water.data.dto.GeoJsonFeature;
import com.water.data.dto.GeoJsonFeatureCollection;
import com.water.data.dto.GeoJsonGeometry;
import com.water.data.model.CommunityData;
import com.water.data.model.HydroData;
import com.water.data.model.InfrastructureData;
import com.water.data.repository.CommunityDataRepository;
import com.water.data.repository.HydroDataRepository;
import com.water.data.repository.InfrastructureDataRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for map-related operations and spatial queries.
 * Converts data to GeoJSON format for map visualization.
 */
@Service
public class MapService {

    private final HydroDataRepository hydroDataRepository;
    private final CommunityDataRepository communityDataRepository;
    private final InfrastructureDataRepository infrastructureDataRepository;

    public MapService(
        HydroDataRepository hydroDataRepository,
        CommunityDataRepository communityDataRepository,
        InfrastructureDataRepository infrastructureDataRepository
    ) {
        this.hydroDataRepository = hydroDataRepository;
        this.communityDataRepository = communityDataRepository;
        this.infrastructureDataRepository = infrastructureDataRepository;
    }

    /**
     * Find communities within radius and return as GeoJSON.
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param radiusKm Radius in kilometers
     * @return GeoJSON FeatureCollection
     */
    public GeoJsonFeatureCollection findCommunitiesNearby(double longitude, double latitude, double radiusKm) {
        double radiusMeters = radiusKm * 1000;
        List<CommunityData> communities = communityDataRepository.findWithinRadius(longitude, latitude, radiusMeters);

        List<GeoJsonFeature> features = communities.stream()
            .map(this::convertCommunityToFeature)
            .collect(Collectors.toList());

        return GeoJsonFeatureCollection.create(features);
    }

    /**
     * Find facilities within radius and return as GeoJSON.
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param radiusKm Radius in kilometers
     * @return GeoJSON FeatureCollection
     */
    public GeoJsonFeatureCollection findFacilitiesNearby(double longitude, double latitude, double radiusKm) {
        double radiusMeters = radiusKm * 1000;
        List<InfrastructureData> facilities = infrastructureDataRepository.findWithinRadius(longitude, latitude, radiusMeters);

        List<GeoJsonFeature> features = facilities.stream()
            .map(this::convertFacilityToFeature)
            .collect(Collectors.toList());

        return GeoJsonFeatureCollection.create(features);
    }

    /**
     * Find measurements within radius and return as GeoJSON.
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param radiusKm Radius in kilometers
     * @param parameterName Optional filter by parameter (e.g., "arsenic")
     * @return GeoJSON FeatureCollection
     */
    public GeoJsonFeatureCollection findMeasurementsNearby(
        double longitude,
        double latitude,
        double radiusKm,
        String parameterName
    ) {
        double radiusMeters = radiusKm * 1000;
        List<HydroData> measurements = hydroDataRepository.findWithinRadius(longitude, latitude, radiusMeters);

        // Filter by parameter if specified
        if (parameterName != null && !parameterName.isEmpty()) {
            measurements = measurements.stream()
                .filter(m -> parameterName.equalsIgnoreCase(m.getParameterName()))
                .collect(Collectors.toList());
        }

        List<GeoJsonFeature> features = measurements.stream()
            .map(this::convertMeasurementToFeature)
            .collect(Collectors.toList());

        return GeoJsonFeatureCollection.create(features);
    }

    /**
     * Get all communities as GeoJSON.
     *
     * @return GeoJSON FeatureCollection
     */
    public GeoJsonFeatureCollection getAllCommunities() {
        List<CommunityData> communities = communityDataRepository.findAll();

        List<GeoJsonFeature> features = communities.stream()
            .map(this::convertCommunityToFeature)
            .collect(Collectors.toList());

        return GeoJsonFeatureCollection.create(features);
    }

    /**
     * Get all facilities as GeoJSON.
     *
     * @param operationalOnly If true, only return operational facilities
     * @return GeoJSON FeatureCollection
     */
    public GeoJsonFeatureCollection getAllFacilities(boolean operationalOnly) {
        List<InfrastructureData> facilities;

        if (operationalOnly) {
            facilities = infrastructureDataRepository.findByOperationalStatus(
                InfrastructureData.OperationalStatus.OPERATIONAL
            );
        } else {
            facilities = infrastructureDataRepository.findAll();
        }

        List<GeoJsonFeature> features = facilities.stream()
            .map(this::convertFacilityToFeature)
            .collect(Collectors.toList());

        return GeoJsonFeatureCollection.create(features);
    }

    /**
     * Converts CommunityData to GeoJSON Feature.
     */
    private GeoJsonFeature convertCommunityToFeature(CommunityData community) {
        GeoJsonGeometry geometry = GeoJsonGeometry.createPoint(
            community.getLatitude(),
            community.getLongitude()
        );

        Map<String, Object> properties = new HashMap<>();
        properties.put("id", community.getId());
        properties.put("name", community.getCommunityName());
        properties.put("population", community.getPopulation());
        properties.put("householdCount", community.getHouseholdCount());
        properties.put("waterAccessLevel", community.getWaterAccessLevel() != null ?
            community.getWaterAccessLevel().name().toLowerCase() : null);
        properties.put("primaryWaterSource", community.getPrimaryWaterSource());
        properties.put("collectionDate", community.getCollectionDate());
        properties.put("type", "community");

        return GeoJsonFeature.create(geometry, properties);
    }

    /**
     * Converts InfrastructureData to GeoJSON Feature.
     */
    private GeoJsonFeature convertFacilityToFeature(InfrastructureData facility) {
        GeoJsonGeometry geometry = GeoJsonGeometry.createPoint(
            facility.getLatitude(),
            facility.getLongitude()
        );

        Map<String, Object> properties = new HashMap<>();
        properties.put("id", facility.getId());
        properties.put("name", facility.getFacilityName());
        properties.put("facilityType", facility.getFacilityType().name().toLowerCase());
        properties.put("operationalStatus", facility.getOperationalStatus().name().toLowerCase());
        properties.put("capacity", facility.getCapacity());
        properties.put("capacityUnit", facility.getCapacityUnit());
        properties.put("populationServed", facility.getPopulationServed());
        properties.put("installationDate", facility.getInstallationDate());
        properties.put("lastMaintenanceDate", facility.getLastMaintenanceDate());
        properties.put("type", "facility");

        return GeoJsonFeature.create(geometry, properties);
    }

    /**
     * Converts HydroData to GeoJSON Feature.
     */
    private GeoJsonFeature convertMeasurementToFeature(HydroData measurement) {
        GeoJsonGeometry geometry = GeoJsonGeometry.createPoint(
            measurement.getLatitude(),
            measurement.getLongitude()
        );

        Map<String, Object> properties = new HashMap<>();
        properties.put("id", measurement.getId());
        properties.put("locationName", measurement.getLocationName());
        properties.put("parameterName", measurement.getParameterName());
        properties.put("measurementValue", measurement.getMeasurementValue());
        properties.put("measurementUnit", measurement.getMeasurementUnit());
        properties.put("measurementDate", measurement.getMeasurementDate());
        properties.put("dataType", measurement.getDataType());
        properties.put("source", measurement.getSource());
        properties.put("type", "measurement");

        return GeoJsonFeature.create(geometry, properties);
    }
}
