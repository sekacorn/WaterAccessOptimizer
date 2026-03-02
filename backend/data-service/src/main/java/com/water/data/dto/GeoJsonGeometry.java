package com.water.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GeoJSON Geometry object (RFC 7946).
 * Supports Point geometry for water data locations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonGeometry {

    private String type;  // "Point"
    private List<Double> coordinates;  // [longitude, latitude]

    /**
     * Creates Point geometry from latitude and longitude.
     * Note: GeoJSON uses [longitude, latitude] order!
     *
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @return GeoJsonGeometry Point
     */
    public static GeoJsonGeometry createPoint(double latitude, double longitude) {
        return new GeoJsonGeometry("Point", List.of(longitude, latitude));
    }
}
