package de.bsi.secvisogram.csaf_cms_backend.postgres.repository;

import de.bsi.secvisogram.csaf_cms_backend.postgres.entity.AdvisoryVersionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for advisory version snapshots.
 */
public interface AdvisoryVersionRepository extends JpaRepository<AdvisoryVersionEntity, UUID> {

    List<AdvisoryVersionEntity> findByAdvisoryId(UUID advisoryId);

    void deleteByAdvisoryId(UUID advisoryId);
}
