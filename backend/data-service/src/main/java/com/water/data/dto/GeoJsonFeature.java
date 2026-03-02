package com.water.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * GeoJSON Feature object (RFC 7946).
 * Represents a single geographic feature with geometry and properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonFeature {

    private String type = "Feature";  // Always "Feature"
    private GeoJsonGeometry geometry;
    private Map<String, Object> properties;

    /**
     * Creates a GeoJSON Feature.
     *
     * @param geometry Feature geometry (Point)
     * @param properties Feature properties (arbitrary key-value pairs)
     * @return GeoJsonFeature
     */
    public static GeoJsonFeature create(GeoJsonGeometry geometry, Map<String, Object> properties) {
        return new GeoJsonFeature("Feature", geometry, properties);
    }
}
