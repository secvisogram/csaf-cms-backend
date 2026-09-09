package de.bsi.secvisogram.csaf_cms_backend.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the comments table.
 */
@Entity
@Table(name = "comments")
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Entity")
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisory_id", nullable = false)
    private AdvisoryEntity advisory;

    @Column(nullable = false)
    private String owner;

    @Column(name = "comment_text", nullable = false)
    private String commentText;

    @Column(name = "csaf_node_id")
    private String csafNodeId;

    @Column(name = "field_name")
    private String fieldName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_to")
    private CommentEntity answerTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AdvisoryEntity getAdvisory() {
        return advisory;
    }

    public void setAdvisory(AdvisoryEntity advisory) {
        this.advisory = advisory;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getCsafNodeId() {
        return csafNodeId;
    }

    public void setCsafNodeId(String csafNodeId) {
        this.csafNodeId = csafNodeId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public CommentEntity getAnswerTo() {
        return answerTo;
    }

    public void setAnswerTo(CommentEntity answerTo) {
        this.answerTo = answerTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
