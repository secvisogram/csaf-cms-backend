package de.bsi.secvisogram.csaf_cms_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

/**
 * JPA entity for the advisories table.
 */
@Entity
@Table(name = "advisories")
public class AdvisoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_state", nullable = false)
    private String workflowState = "Draft";

    @Column(nullable = false)
    private String owner;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode csaf;

    @Column(name = "versioning_type", nullable = false)
    private String versioningType = "Semantic";

    @Column(name = "last_major_version")
    private String lastMajorVersion;

    @Column(name = "tmp_tracking_id")
    private String tmpTrackingId;

    @Column(name = "advisory_reference")
    private UUID advisoryReference;

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

    public String getTmpTrackingId() {
        return tmpTrackingId;
    }

    public void setTmpTrackingId(String tmpTrackingId) {
        this.tmpTrackingId = tmpTrackingId;
    }

    public UUID getAdvisoryReference() {
        return advisoryReference;
    }

    public void setAdvisoryReference(UUID advisoryReference) {
        this.advisoryReference = advisoryReference;
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
