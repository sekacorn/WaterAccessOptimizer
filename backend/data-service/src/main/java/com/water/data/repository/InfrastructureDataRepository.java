package com.water.data.repository;

import com.water.data.model.InfrastructureData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for InfrastructureData entity.
 * Provides database operations for infrastructure data with spatial queries.
 */
@Repository
public interface InfrastructureDataRepository extends JpaRepository<InfrastructureData, Long> {

    /**
     * Find all infrastructure data by upload ID.
     *
     * @param uploadId Upload ID
     * @return List of infrastructure data records
     */
    List<InfrastructureData> findByUploadId(UUID uploadId);

    /**
     * Find all infrastructure data by user ID.
     *
     * @param userId User ID
     * @return List of infrastructure data records
     */
    List<InfrastructureData> findByUserId(UUID userId);

    /**
     * Find facilities by type.
     *
     * @param facilityType Facility type
     * @return List of facilities of specified type
     */
    List<InfrastructureData> findByFacilityType(InfrastructureData.FacilityType facilityType);

    /**
     * Find facilities by operational status.
     *
     * @param operationalStatus Operational status
     * @return List of facilities with specified status
     */
    List<InfrastructureData> findByOperationalStatus(InfrastructureData.OperationalStatus operationalStatus);

    /**
     * Find operational facilities by type.
     *
     * @param facilityType Facility type
     * @return List of operational facilities
     */
    @Query("SELECT i FROM InfrastructureData i WHERE i.facilityType = :facilityType AND i.operationalStatus = 'OPERATIONAL'")
    List<InfrastructureData> findOperationalByType(@Param("facilityType") InfrastructureData.FacilityType facilityType);

    /**
     * Find facilities by name (case-insensitive).
     *
     * @param facilityName Facility name
     * @return List of matching facilities
     */
    @Query("SELECT i FROM InfrastructureData i WHERE LOWER(i.facilityName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<InfrastructureData> findByFacilityNameContainingIgnoreCase(@Param("name") String facilityName);

    /**
     * Get total population served by all facilities.
     *
     * @return Sum of population served
     */
    @Query("SELECT COALESCE(SUM(i.populationServed), 0) FROM InfrastructureData i WHERE i.operationalStatus = 'OPERATIONAL'")
    Long getTotalPopulationServed();

    /**
     * Count facilities by upload ID.
     *
     * @param uploadId Upload ID
     * @return Count of facilities
     */
    Long countByUploadId(UUID uploadId);

    /**
     * Count operational facilities by type.
     *
     * @param facilityType Facility type
     * @return Count of operational facilities
     */
    @Query("SELECT COUNT(i) FROM InfrastructureData i WHERE i.facilityType = :facilityType AND i.operationalStatus = 'OPERATIONAL'")
    Long countOperationalByType(@Param("facilityType") InfrastructureData.FacilityType facilityType);

    /**
     * Delete all facilities by upload ID (for soft delete cleanup).
     *
     * @param uploadId Upload ID
     */
    void deleteByUploadId(UUID uploadId);

    /**
     * Find facilities by external source ID.
     *
     * @param externalSourceId External source ID
     * @return List of facilities from external source
     */
    List<InfrastructureData> findByExternalSourceId(String externalSourceId);

    /**
     * Find facilities within radius of a point (spatial query).
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param radiusMeters Radius in meters
     * @return List of facilities within radius
     */
    @Query(value = "SELECT * FROM infrastructure_data WHERE ST_DWithin(coordinates, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters) ORDER BY ST_Distance(coordinates, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)", nativeQuery = true)
    List<InfrastructureData> findWithinRadius(@Param("longitude") double longitude,
                                               @Param("latitude") double latitude,
                                               @Param("radiusMeters") double radiusMeters);

    /**
     * Find K nearest facilities to a point (spatial query).
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param limit Number of results
     * @return List of nearest facilities
     */
    @Query(value = "SELECT * FROM infrastructure_data ORDER BY ST_Distance(coordinates, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) LIMIT :limit", nativeQuery = true)
    List<InfrastructureData> findNearest(@Param("longitude") double longitude,
                                          @Param("latitude") double latitude,
                                          @Param("limit") int limit);
}
