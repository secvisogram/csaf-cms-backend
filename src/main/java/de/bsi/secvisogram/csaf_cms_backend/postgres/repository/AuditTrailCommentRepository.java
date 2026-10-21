package de.bsi.secvisogram.csaf_cms_backend.postgres.repository;

import de.bsi.secvisogram.csaf_cms_backend.postgres.entity.AuditTrailCommentEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for comment audit trail entries.
 */
public interface AuditTrailCommentRepository extends JpaRepository<AuditTrailCommentEntity, UUID> {

    List<AuditTrailCommentEntity> findByCommentId(UUID commentId);

    void deleteByCommentId(UUID commentId);

    void deleteByCommentIdIn(Collection<UUID> commentIds);
}
