package de.bsi.secvisogram.csaf_cms_backend.postgres;

import de.bsi.secvisogram.csaf_cms_backend.postgres.entity.*;
import de.bsi.secvisogram.csaf_cms_backend.postgres.repository.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bridge service that wraps the JPA repositories and provides a single access point
 * for all PostgreSQL persistence operations. Acts as an adapter layer during the
 * gradual migration away from CouchDB, allowing the rest of the service layer to
 * be migrated incrementally without exposing individual repositories directly.
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring DI")
public class PostgresRepositoryService {

    private final AdvisoryRepository advisoryRepository;
    private final AdvisoryVersionRepository advisoryVersionRepository;
    private final AuditTrailDocumentRepository auditTrailDocumentRepository;
    private final AuditTrailWorkflowRepository auditTrailWorkflowRepository;
    private final CommentRepository commentRepository;
    private final AuditTrailCommentRepository auditTrailCommentRepository;
    private final CounterRepository counterRepository;

    /**
     * Create a new PostgresRepositoryService with all required repositories.
     *
     * @param advisoryRepository           repository for advisory entities
     * @param advisoryVersionRepository    repository for advisory version snapshots
     * @param auditTrailDocumentRepository repository for document audit trail entries
     * @param auditTrailWorkflowRepository repository for workflow audit trail entries
     * @param commentRepository            repository for comment entities
     * @param auditTrailCommentRepository  repository for comment audit trail entries
     * @param counterRepository            repository for sequential counters
     */
    public PostgresRepositoryService(
            AdvisoryRepository advisoryRepository,
            AdvisoryVersionRepository advisoryVersionRepository,
            AuditTrailDocumentRepository auditTrailDocumentRepository,
            AuditTrailWorkflowRepository auditTrailWorkflowRepository,
            CommentRepository commentRepository,
            AuditTrailCommentRepository auditTrailCommentRepository,
            CounterRepository counterRepository) {
        this.advisoryRepository = advisoryRepository;
        this.advisoryVersionRepository = advisoryVersionRepository;
        this.auditTrailDocumentRepository = auditTrailDocumentRepository;
        this.auditTrailWorkflowRepository = auditTrailWorkflowRepository;
        this.commentRepository = commentRepository;
        this.auditTrailCommentRepository = auditTrailCommentRepository;
        this.counterRepository = counterRepository;
    }

    // --- Advisory operations ---

    /**
     * Persist a new advisory or update an existing one.
     *
     * @param entity the advisory entity to save
     * @return the saved entity (with generated ID if new), with an up-to-date {@code version}
     */
    public AdvisoryEntity saveAdvisory(AdvisoryEntity entity) {
        updateAdvisoryListColumns(entity);
        return advisoryRepository.saveAndFlush(entity);
    }

    private void updateAdvisoryListColumns(AdvisoryEntity entity) {
        JsonNode csaf = entity.getCsaf();
        entity.setTrackingId(textAt(csaf, "/document/tracking/id"));
        entity.setDocumentTitle(textAt(csaf, "/document/title"));

        String releaseDate = textAt(csaf, "/document/tracking/current_release_date");
        entity.setCurrentReleaseDate(parseReleaseDate(releaseDate));
    }

    private String textAt(JsonNode node, String path) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.at(path);
        return value.isMissingNode() || value.isNull() ? null : value.asString();
    }

    private Instant parseReleaseDate(String releaseDate) {

        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(releaseDate);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * Look up a single advisory by its primary key.
     *
     * @param id the advisory UUID
     * @return an Optional containing the entity, or empty if not found
     */
    public Optional<AdvisoryEntity> findAdvisoryById(UUID id) {
        return advisoryRepository.findById(id);
    }

    /**
     * Delete the advisory.
     *
     * @param entity the advisory to delete
     */
    @Transactional
    public void deleteAdvisory(AdvisoryEntity entity) {
        advisoryRepository.delete(entity);
    }

    /**
     * Check whether an advisory with the given CSAF tracking ID already exists.
     *
     * @param trackingId the CSAF document tracking ID
     * @return true if at least one advisory carries that tracking ID
     */
    public boolean advisoryExistsByTrackingId(String trackingId) {
        return advisoryRepository.existsByTrackingId(trackingId);
    }

    /**
     * Return the total number of documents across all tables.
     * This is primarily used by integration tests to verify that the expected number of records
     * are created/deleted across all entity types (advisories, comments, audit trails, counters).
     *
     * @return total count of all entities
     */
    public long getTotalDocumentCount() {
        return advisoryRepository.count()
                + advisoryVersionRepository.count()
                + auditTrailDocumentRepository.count()
                + auditTrailWorkflowRepository.count()
                + commentRepository.count()
                + auditTrailCommentRepository.count()
                + counterRepository.count();
    }

    // --- Advisory Version operations ---

    /**
     * Persist a new advisory version snapshot or update an existing one.
     *
     * @param entity the version entity to save
     * @return the saved entity
     */
    public AdvisoryVersionEntity saveAdvisoryVersion(AdvisoryVersionEntity entity) {
        updateAdvisoryVersionListColumns(entity);
        return advisoryVersionRepository.save(entity);
    }

    private void updateAdvisoryVersionListColumns(AdvisoryVersionEntity entity) {
        JsonNode csaf = entity.getCsaf();
        entity.setTrackingId(textAt(csaf, "/document/tracking/id"));
        entity.setDocumentTitle(textAt(csaf, "/document/title"));

        String releaseDate = textAt(csaf, "/document/tracking/current_release_date");
        entity.setCurrentReleaseDate(parseReleaseDate(releaseDate));
    }


    // --- Audit Trail Document operations ---

    /**
     * Persist a new document audit trail entry or update an existing one.
     *
     * @param entity the audit trail document entity to save
     * @return the saved entity
     */
    public AuditTrailDocumentEntity saveAuditTrailDocument(AuditTrailDocumentEntity entity) {
        return auditTrailDocumentRepository.save(entity);
    }

    // --- Audit Trail Workflow operations ---

    /**
     * Persist a new workflow audit trail entry or update an existing one.
     *
     * @param entity the audit trail workflow entity to save
     * @return the saved entity
     */
    public AuditTrailWorkflowEntity saveAuditTrailWorkflow(AuditTrailWorkflowEntity entity) {
        return auditTrailWorkflowRepository.save(entity);
    }

    // --- Comment operations ---

    /**
     * Persist a new comment or update an existing one.
     *
     * @param entity the comment entity to save
     * @return the saved entity, with an up-to-date {@code version}
     */
    public CommentEntity saveComment(CommentEntity entity) {
        return commentRepository.saveAndFlush(entity);
    }

    /**
     * Look up a single comment by its primary key.
     *
     * @param id the comment UUID
     * @return an Optional containing the entity, or empty if not found
     */
    public Optional<CommentEntity> findCommentById(UUID id) {
        return commentRepository.findById(id);
    }

    /**
     * Return all top-level comments and answers belonging to the given advisory.
     *
     * @param advisoryId the advisory UUID
     * @return matching comment entities
     */
    public List<CommentEntity> findCommentsByAdvisoryId(UUID advisoryId) {
        return commentRepository.findByAdvisoryId(advisoryId);
    }

    /**
     * Return all answers (replies) that reference the given parent comment.
     *
     * @param commentId the UUID of the parent comment
     * @return answer entities whose answerTo references the given comment
     */
    public List<CommentEntity> findAnswersByCommentId(UUID commentId) {
        return commentRepository.findByAnswerToId(commentId);
    }

    /**
     * Delete the comment identified by the given UUID.
     *
     * @param id the comment UUID to delete
     */
    @Transactional
    public void deleteComment(UUID id) {
        commentRepository.deleteById(id);
    }

    /**
     * Delete the comment.
     *
     * @param entity the comment entity to delete
     */
    @Transactional
    public void deleteComment(CommentEntity entity) {
        commentRepository.delete(entity);
    }

    /**
     * Delete all comments belonging to the given advisory.
     *
     * @param advisoryId the advisory UUID
     */
    @Transactional
    public void deleteCommentsByAdvisoryId(UUID advisoryId) {
        commentRepository.deleteByAdvisoryId(advisoryId);
    }

    // --- Audit Trail Comment operations ---

    /**
     * Persist a new comment audit trail entry or update an existing one.
     *
     * @param entity the audit trail comment entity to save
     * @return the saved entity
     */
    public AuditTrailCommentEntity saveAuditTrailComment(AuditTrailCommentEntity entity) {
        return auditTrailCommentRepository.save(entity);
    }


    // --- Counter operations ---

    /**
     * Atomically increment the named counter and return its new value.
     * If the counter does not yet exist it is created with an initial value of zero
     * before the increment is applied, so the first call returns 1.
     *
     * <p>This method runs in its own transaction ({@code REQUIRES_NEW}) so that a
     * {@link DataIntegrityViolationException} caused by a concurrent first-time insert
     * rolls back only this unit of work and does not poison the caller's transaction.
     * On conflict the method retries once: the row is guaranteed to exist at that point,
     * so the retry always succeeds.</p>
     *
     * <p>{@code saveAndFlush} is used (rather than {@code save}) to guarantee the new
     * row is written to the database before the subsequent native UPDATE executes.
     * Without an explicit flush, JPA's {@code AUTO} flush mode does not guarantee
     * ordering relative to native queries.</p>
     *
     * @param counterId the logical name of the counter (e.g. "TMP_TRACKING_ID_COUNTER")
     * @return the post-increment counter value (always &gt;= 1)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long incrementAndGetCounter(String counterId) {
        Long newValue = counterRepository.incrementAndGet(counterId);
        if (newValue != null) {
            return newValue;
        }
        try {
            CounterEntity counter = new CounterEntity();
            counter.setId(counterId);
            counter.setCount(0L);
            counterRepository.saveAndFlush(counter);
        } catch (DataIntegrityViolationException ignored) {
            // A concurrent caller created the row first; proceed to increment below.
        }
        Long retried = counterRepository.incrementAndGet(counterId);
        if (retried == null) {
            throw new IllegalStateException(
                    "Counter '" + counterId + "' could not be incremented after creation");
        }
        return retried;
    }

}
