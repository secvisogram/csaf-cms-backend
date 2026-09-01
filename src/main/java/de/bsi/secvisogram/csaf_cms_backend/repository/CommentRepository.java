package de.bsi.secvisogram.csaf_cms_backend.repository;

import de.bsi.secvisogram.csaf_cms_backend.entity.CommentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for comments and answers.
 */
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByAdvisoryId(UUID advisoryId);

    List<CommentEntity> findByAnswerToId(UUID commentId);

    void deleteByAdvisoryId(UUID advisoryId);
}
