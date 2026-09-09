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
 * JPA entity for the advisory_versions table.
 */
@Entity
@Table(name = "advisory_versions")
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Entity")
public class AdvisoryVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisory_id", nullable = false)
    private AdvisoryEntity advisory;

    @Column(name = "workflow_state", nullable = false)
    private String workflowState;

    @Column(nullable = false)
    private String owner;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode csaf;

    @Column(name = "versioning_type", nullable = false)
    private String versioningType;

    @Column(name = "last_major_version")
    private String lastMajorVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public String getWorkflowState() {
        return workflowState;
    }

    public void setWorkflowState(String workflowState) {
        this.workflowState = workflowState;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public JsonNode getCsaf() {
        return csaf;
    }

    public void setCsaf(JsonNode csaf) {
        this.csaf = csaf;
    }

    public String getVersioningType() {
        return versioningType;
    }

    public void setVersioningType(String versioningType) {
        this.versioningType = versioningType;
    }

    public String getLastMajorVersion() {
        return lastMajorVersion;
    }

    public void setLastMajorVersion(String lastMajorVersion) {
        this.lastMajorVersion = lastMajorVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
