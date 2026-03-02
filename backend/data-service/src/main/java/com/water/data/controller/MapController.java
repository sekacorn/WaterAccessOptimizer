package com.water.data.controller;

import com.water.data.dto.GeoJsonFeatureCollection;
import com.water.data.service.MapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for map-related endpoints.
 * Provides spatial queries and GeoJSON exports for map visualization.
 */
@RestController
@RequestMapping("/api/v1/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    /**
     * Find communities within radius of a point.
     *
     * GET /api/v1/map/communities/nearby
     * Query Parameters:
     *   - longitude: Center longitude (required)
     *   - latitude: Center latitude (required)
     *   - radius: Radius in kilometers (default 10)
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param radius Radius in kilometers
     * @return GeoJSON FeatureCollection
     */
    @GetMapping("/communities/nearby")
    public ResponseEntity<GeoJsonFeatureCollection> findCommunitiesNearby(
        @RequestParam double longitude,
        @RequestParam double latitude,
        @RequestParam(defaultValue = "10") double radius
    ) {
        GeoJsonFeatureCollection result = mapService.findCommunitiesNearby(longitude, latitude, radius);
        return ResponseEntity.ok(result);
    }

    /**
     * Find facilities within radius of a point.
     *
     * GET /api/v1/map/facilities/nearby
     * Query Parameters:
     *   - longitude: Center longitude (required)
     *   - latitude: Center latitude (required)
     *   - radius: Radius in kilometers (default 10)
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param radius Radius in kilometers
     * @return GeoJSON FeatureCollection
     */
    @GetMapping("/facilities/nearby")
    public ResponseEntity<GeoJsonFeatureCollection> findFacilitiesNearby(
        @RequestParam double longitude,
        @RequestParam double latitude,
        @RequestParam(defaultValue = "10") double radius
    ) {
        GeoJsonFeatureCollection result = mapService.findFacilitiesNearby(longitude, latitude, radius);
        return ResponseEntity.ok(result);
    }

    /**
     * Find measurements within radius of a point.
     *
     * GET /api/v1/map/measurements/nearby
     * Query Parameters:
     *   - longitude: Center longitude (required)
     *   - latitude: Center latitude (required)
     *   - radius: Radius in kilometers (default 10)
     *   - parameter: Optional filter by parameter name (e.g., "arsenic")
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param radius Radius in kilometers
     * @param parameter Optional parameter filter
     * @return GeoJSON FeatureCollection
     */
    @GetMapping("/measurements/nearby")
    public ResponseEntity<GeoJsonFeatureCollection> findMeasurementsNearby(
        @RequestParam double longitude,
        @RequestParam double latitude,
        @RequestParam(defaultValue = "10") double radius,
        @RequestParam(required = false) String parameter
    ) {
        GeoJsonFeatureCollection result = mapService.findMeasurementsNearby(
            longitude, latitude, radius, parameter
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Get all communities as GeoJSON.
     *
     * GET /api/v1/map/communities
     *
     * @return GeoJSON FeatureCollection
     */
    @GetMapping("/communities")
    public ResponseEntity<GeoJsonFeatureCollection> getAllCommunities() {
        GeoJsonFeatureCollection result = mapService.getAllCommunities();
        return ResponseEntity.ok(result);
    }

    /**
     * Get all facilities as GeoJSON.
     *
     * GET /api/v1/map/facilities
     * Query Parameters:
     *   - operational: If true, only return operational facilities (default false)
     *
     * @param operational Filter by operational status
     * @return GeoJSON FeatureCollection
     */
    @GetMapping("/facilities")
    public ResponseEntity<GeoJsonFeatureCollection> getAllFacilities(
        @RequestParam(defaultValue = "false") boolean operational
    ) {
        GeoJsonFeatureCollection result = mapService.getAllFacilities(operational);
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint for map service.
     *
     * GET /api/v1/map/health
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, String>> health() {
        return ResponseEntity.ok(java.util.Map.of(
            "status", "ok",
            "service", "map-service"
        ));
    }
}
