package com.water.integrator.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.water.integrator.model.CommunityData;
import com.water.integrator.model.HydroData;
import com.water.integrator.model.InfrastructureData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for parsing GeoJSON files containing water data.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GeoJsonParser {

    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public List<HydroData> parseHydroDataFromJson(InputStream inputStream, UUID userId) throws Exception {
        List<HydroData> dataList = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(inputStream);
        JsonNode features = rootNode.get("features");

        if (features != null && features.isArray()) {
            for (JsonNode feature : features) {
                HydroData data = new HydroData();
                data.setUserId(userId);

                JsonNode properties = feature.get("properties");
                if (properties != null) {
                    data.setSource(properties.has("source") ? properties.get("source").asText() : "Unknown");
                    data.setDataType(properties.get("data_type").asText());
                    data.setLocationName(properties.get("location_name").asText());
                    data.setMeasurementValue(new BigDecimal(properties.get("measurement_value").asText()));
                    data.setMeasurementUnit(properties.get("measurement_unit").asText());
                }

                // Parse geometry
                JsonNode geometry = feature.get("geometry");
                if (geometry != null && geometry.get("type").asText().equals("Point")) {
                    JsonNode coordinates = geometry.get("coordinates");
                    double lon = coordinates.get(0).asDouble();
                    double lat = coordinates.get(1).asDouble();
                    Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                    point.setSRID(4326);
                    data.setLocation(point);
                }

                dataList.add(data);
            }
        }

        log.info("Parsed {} hydro data records from GeoJSON", dataList.size());
        return dataList;
    }

    public List<CommunityData> parseCommunityDataFromJson(InputStream inputStream, UUID userId) throws Exception {
        List<CommunityData> dataList = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(inputStream);
        JsonNode features = rootNode.get("features");

        if (features != null && features.isArray()) {
            for (JsonNode feature : features) {
                CommunityData data = new CommunityData();
                data.setUserId(userId);

                JsonNode properties = feature.get("properties");
                if (properties != null) {
                    data.setCommunityName(properties.get("community_name").asText());
                    data.setPopulation(properties.get("population").asInt());
                    data.setWaterAccessLevel(properties.get("water_access_level").asText());
                    data.setSource(properties.has("source") ? properties.get("source").asText() : "OpenStreetMap");
                }

                // Parse geometry
                JsonNode geometry = feature.get("geometry");
                if (geometry != null && geometry.get("type").asText().equals("Point")) {
                    JsonNode coordinates = geometry.get("coordinates");
                    double lon = coordinates.get(0).asDouble();
                    double lat = coordinates.get(1).asDouble();
                    Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                    point.setSRID(4326);
                    data.setLocation(point);
                }

                dataList.add(data);
            }
        }

        log.info("Parsed {} community data records from GeoJSON", dataList.size());
        return dataList;
    }

    public List<InfrastructureData> parseInfrastructureDataFromJson(InputStream inputStream, UUID userId) throws Exception {
        List<InfrastructureData> dataList = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(inputStream);
        JsonNode features = rootNode.get("features");

        if (features != null && features.isArray()) {
            for (JsonNode feature : features) {
                InfrastructureData data = new InfrastructureData();
                data.setUserId(userId);

                JsonNode properties = feature.get("properties");
                if (properties != null) {
                    data.setFacilityType(properties.get("facility_type").asText());
                    data.setFacilityName(properties.get("facility_name").asText());
                    data.setCapacity(new BigDecimal(properties.get("capacity").asText()));
                    data.setCapacityUnit(properties.get("capacity_unit").asText());
                    data.setOperationalStatus(properties.get("operational_status").asText());
                }

                // Parse geometry
                JsonNode geometry = feature.get("geometry");
                if (geometry != null && geometry.get("type").asText().equals("Point")) {
                    JsonNode coordinates = geometry.get("coordinates");
                    double lon = coordinates.get(0).asDouble();
                    double lat = coordinates.get(1).asDouble();
                    Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                    point.setSRID(4326);
                    data.setLocation(point);
                }

                dataList.add(data);
            }
        }

        log.info("Parsed {} infrastructure data records from GeoJSON", dataList.size());
        return dataList;
    }
}
