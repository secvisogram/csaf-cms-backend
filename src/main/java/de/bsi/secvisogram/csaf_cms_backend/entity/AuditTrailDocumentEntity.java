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
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

/**
 * JPA entity for the audit_trail_documents table.
 */
@Entity
@Table(name = "audit_trail_documents")
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Entity")
public class AuditTrailDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisory_id", nullable = false)
    private AdvisoryEntity advisory;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "\"user\"", nullable = false)
    private String user;

    @Column(name = "change_type", nullable = false)
    private String changeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode diff;

    @Column(name = "old_doc_version")
    private String oldDocVersion;

    @Column(name = "doc_version")
    private String docVersion;

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public JsonNode getDiff() {
        return diff;
    }

    public void setDiff(JsonNode diff) {
        this.diff = diff;
    }

    public String getOldDocVersion() {
        return oldDocVersion;
    }

    public void setOldDocVersion(String oldDocVersion) {
        this.oldDocVersion = oldDocVersion;
    }

    public String getDocVersion() {
        return docVersion;
    }

    public void setDocVersion(String docVersion) {
        this.docVersion = docVersion;
    }
}
