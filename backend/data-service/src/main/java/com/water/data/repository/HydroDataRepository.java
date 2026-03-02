package com.water.data.repository;

import com.water.data.model.HydroData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for HydroData entity.
 * Provides database operations for hydrological measurements with spatial queries.
 */
@Repository
public interface HydroDataRepository extends JpaRepository<HydroData, Long> {

    /**
     * Find all hydro data by upload ID.
     *
     * @param uploadId Upload ID
     * @return List of hydro data records
     */
    List<HydroData> findByUploadId(UUID uploadId);

    /**
     * Find all hydro data by user ID.
     *
     * @param userId User ID
     * @return List of hydro data records
     */
    List<HydroData> findByUserId(UUID userId);

    /**
     * Find hydro data by parameter name.
     *
     * @param parameterName Parameter name (e.g., "arsenic", "pH")
     * @return List of matching measurements
     */
    List<HydroData> findByParameterName(String parameterName);

    /**
     * Find measurements within date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of measurements in date range
     */
    @Query("SELECT h FROM HydroData h WHERE h.measurementDate BETWEEN :startDate AND :endDate ORDER BY h.measurementDate DESC")
    List<HydroData> findByMeasurementDateBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Find measurements by parameter and date range.
     *
     * @param parameterName Parameter name
     * @param startDate Start date
     * @param endDate End date
     * @return List of matching measurements
     */
    @Query("SELECT h FROM HydroData h WHERE h.parameterName = :parameterName AND h.measurementDate BETWEEN :startDate AND :endDate ORDER BY h.measurementDate DESC")
    List<HydroData> findByParameterAndDateRange(@Param("parameterName") String parameterName,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Count measurements by upload ID.
     *
     * @param uploadId Upload ID
     * @return Count of measurements
     */
    Long countByUploadId(UUID uploadId);

    /**
     * Delete all measurements by upload ID (for soft delete cleanup).
     *
     * @param uploadId Upload ID
     */
    void deleteByUploadId(UUID uploadId);

    /**
     * Find measurements by external source ID.
     *
     * @param externalSourceId External source ID
     * @return List of measurements from external source
     */
    List<HydroData> findByExternalSourceId(String externalSourceId);

    /**
     * Find measurements within radius of a point (spatial query).
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param radiusMeters Radius in meters
     * @return List of measurements within radius
     */
    @Query(value = "SELECT * FROM hydro_data WHERE ST_DWithin(coordinates, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters) ORDER BY ST_Distance(coordinates, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)", nativeQuery = true)
    List<HydroData> findWithinRadius(@Param("longitude") double longitude,
                                      @Param("latitude") double latitude,
                                      @Param("radiusMeters") double radiusMeters);

    /**
     * Find K nearest measurements to a point (spatial query).
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param limit Number of results
     * @return List of nearest measurements
     */
    @Query(value = "SELECT * FROM hydro_data ORDER BY ST_Distance(coordinates, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) LIMIT :limit", nativeQuery = true)
    List<HydroData> findNearest(@Param("longitude") double longitude,
                                 @Param("latitude") double latitude,
                                 @Param("limit") int limit);
}
