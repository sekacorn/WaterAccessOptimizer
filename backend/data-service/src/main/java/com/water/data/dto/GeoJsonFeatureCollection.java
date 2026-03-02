package com.water.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GeoJSON FeatureCollection object (RFC 7946).
 * Top-level object containing multiple geographic features.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonFeatureCollection {

    private String type = "FeatureCollection";  // Always "FeatureCollection"
    private List<GeoJsonFeature> features;

    /**
     * Creates a GeoJSON FeatureCollection.
     *
     * @param features List of features
     * @return GeoJsonFeatureCollection
     */
    public static GeoJsonFeatureCollection create(List<GeoJsonFeature> features) {
        return new GeoJsonFeatureCollection("FeatureCollection", features);
    }
}
