package com.water.data.repository;

import com.water.data.model.CommunityData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for CommunityData entity.
 * Provides database operations for community data with spatial queries.
 */
@Repository
public interface CommunityDataRepository extends JpaRepository<CommunityData, Long> {

    /**
     * Find all community data by upload ID.
     *
     * @param uploadId Upload ID
     * @return List of community data records
     */
    List<CommunityData> findByUploadId(UUID uploadId);

    /**
     * Find all community data by user ID.
     *
     * @param userId User ID
     * @return List of community data records
     */
    List<CommunityData> findByUserId(UUID userId);

    /**
     * Find communities by water access level.
     *
     * @param waterAccessLevel Water access level
     * @return List of communities with specified access level
     */
    List<CommunityData> findByWaterAccessLevel(CommunityData.WaterAccessLevel waterAccessLevel);

    /**
     * Find communities by name (case-insensitive).
     *
     * @param communityName Community name
     * @return List of matching communities
     */
    @Query("SELECT c FROM CommunityData c WHERE LOWER(c.communityName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<CommunityData> findByCommunityNameContainingIgnoreCase(@Param("name") String communityName);

    /**
     * Find communities with population greater than specified value.
     *
     * @param minPopulation Minimum population
     * @return List of communities with population >= minPopulation
     */
    @Query("SELECT c FROM CommunityData c WHERE c.population >= :minPopulation ORDER BY c.population DESC")
    List<CommunityData> findByPopulationGreaterThanEqual(@Param("minPopulation") Integer minPopulation);

    /**
     * Get total population from all communities.
     *
     * @return Sum of all community populations
     */
    @Query("SELECT COALESCE(SUM(c.population), 0) FROM CommunityData c")
    Long getTotalPopulation();

    /**
     * Count communities by upload ID.
     *
     * @param uploadId Upload ID
     * @return Count of communities
     */
    Long countByUploadId(UUID uploadId);

    /**
     * Delete all communities by upload ID (for soft delete cleanup).
     *
     * @param uploadId Upload ID
     */
    void deleteByUploadId(UUID uploadId);

    /**
     * Find communities by external source ID.
     *
     * @param externalSourceId External source ID
     * @return List of communities from external source
     */
    List<CommunityData> findByExternalSourceId(String externalSourceId);

    /**
     * Find communities within radius of a point (spatial query).
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param radiusMeters Radius in meters
     * @return List of communities within radius
     */
    @Query(value = "SELECT * FROM community_data WHERE ST_DWithin(coordinates, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters) ORDER BY ST_Distance(coordinates, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)", nativeQuery = true)
    List<CommunityData> findWithinRadius(@Param("longitude") double longitude,
                                          @Param("latitude") double latitude,
                                          @Param("radiusMeters") double radiusMeters);

    /**
     * Find K nearest communities to a point (spatial query).
     *
     * @param longitude Center longitude
     * @param latitude Center latitude
     * @param limit Number of results
     * @return List of nearest communities
     */
    @Query(value = "SELECT * FROM community_data ORDER BY ST_Distance(coordinates, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) LIMIT :limit", nativeQuery = true)
    List<CommunityData> findNearest(@Param("longitude") double longitude,
                                     @Param("latitude") double latitude,
                                     @Param("limit") int limit);
}
