package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField;
import de.bsi.secvisogram.csaf_cms_backend.model.ChangeType;
import java.time.Instant;

/**
 * Superclass for all audit trail entries
 */
public abstract class AuditTrailWrapper {

    private final ObjectNode auditTrailNode;

    AuditTrailWrapper(ObjectNode auditTrailNode) {
        this.auditTrailNode = auditTrailNode;
    }

    ObjectNode getAuditTrailNode() {
        return auditTrailNode;
    }

    public String auditTrailAsString() {

        return this.auditTrailNode.toString();
    }

    public String getType() {

        return this.auditTrailNode.get(CouchDbField.TYPE_FIELD.getDbName()).asText();
    }

    AuditTrailWrapper setType(ObjectType newValue) {

        this.auditTrailNode.put(CouchDbField.TYPE_FIELD.getDbName(), newValue.name());
        return this;
    }

    public String getAdvisoryId() {

        return this.auditTrailNode.get(AuditTrailField.ADVISORY_ID.getDbName()).asText();
    }

    public String getDocVersion() {

        return this.auditTrailNode.get(AuditTrailField.DOC_VERSION.getDbName()).asText();
    }

    public String getOldDocVersion() {

        return this.auditTrailNode.get(AuditTrailField.OLD_DOC_VERSION.getDbName()).asText();
    }

    public String getUser() {

        return this.auditTrailNode.get(AuditTrailField.USER.getDbName()).asText();
    }

    public String getCreatedAt() {

        return this.auditTrailNode.get(AuditTrailField.CREATED_AT.getDbName()).asText();
    }

    public ChangeType getChangeType() {

        return ChangeType.valueOf(this.auditTrailNode.get(AuditTrailField.CHANGE_TYPE.getDbName()).asText());
    }

    public AuditTrailWrapper setAdvisoryId(String newValue) {

        this.auditTrailNode.put(AuditTrailField.ADVISORY_ID.getDbName(), newValue);
        return this;
    }

    public AuditTrailWrapper setDocVersion(String newValue) {

        this.auditTrailNode.put(AuditTrailField.DOC_VERSION.getDbName(), newValue);
        return this;
    }

    public AuditTrailWrapper setOldDocVersion(String newValue) {

        this.auditTrailNode.put(AuditTrailField.OLD_DOC_VERSION.getDbName(), newValue);
        return this;
    }

    public AuditTrailWrapper setUser(String newValue) {

        this.auditTrailNode.put(AuditTrailField.USER.getDbName(), newValue);
        return this;
    }

    public AuditTrailWrapper setCreatedAtToNow() {

        this.auditTrailNode.put(AuditTrailField.CREATED_AT.getDbName(), Instant.now().toString());
        return this;
    }

    public AuditTrailWrapper setChangeType(ChangeType newValue) {

        this.auditTrailNode.put(AuditTrailField.CHANGE_TYPE.getDbName(), newValue.name());
        return this;
    }

}
