package de.bsi.secvisogram.csaf_cms_backend.repository;

import de.bsi.secvisogram.csaf_cms_backend.entity.AuditTrailWorkflowEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for advisory workflow audit trail entries.
 */
public interface AuditTrailWorkflowRepository extends JpaRepository<AuditTrailWorkflowEntity, UUID> {

    List<AuditTrailWorkflowEntity> findByAdvisoryId(UUID advisoryId);

    void deleteByAdvisoryId(UUID advisoryId);
}
