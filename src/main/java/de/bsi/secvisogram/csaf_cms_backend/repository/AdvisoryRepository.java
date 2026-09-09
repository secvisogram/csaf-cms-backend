package de.bsi.secvisogram.csaf_cms_backend.repository;

import de.bsi.secvisogram.csaf_cms_backend.entity.AdvisoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for advisories.
 */
public interface AdvisoryRepository extends JpaRepository<AdvisoryEntity, UUID>,
        JpaSpecificationExecutor<AdvisoryEntity> {

    List<AdvisoryEntity> findByWorkflowState(String workflowState);

    List<AdvisoryEntity> findByOwner(String owner);

    /**
     * Find an advisory by CSAF tracking ID (inside JSONB).
     */
    @Query(value = "SELECT * FROM advisories WHERE csaf -> 'document' -> 'tracking' ->> 'id' = :trackingId",
            nativeQuery = true)
    Optional<AdvisoryEntity> findByTrackingId(@Param("trackingId") String trackingId);

    /**
     * Check if an advisory with a given CSAF tracking ID exists.
     */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM advisories WHERE csaf -> 'document' -> 'tracking' ->> 'id' = :trackingId)",
            nativeQuery = true)
    boolean existsByTrackingId(@Param("trackingId") String trackingId);
}
