package de.bsi.secvisogram.csaf_cms_backend.repository;

import de.bsi.secvisogram.csaf_cms_backend.entity.AuditTrailDocumentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for advisory document audit trail entries.
 */
public interface AuditTrailDocumentRepository extends JpaRepository<AuditTrailDocumentEntity, UUID> {

    List<AuditTrailDocumentEntity> findByAdvisoryId(UUID advisoryId);

    void deleteByAdvisoryId(UUID advisoryId);
}
