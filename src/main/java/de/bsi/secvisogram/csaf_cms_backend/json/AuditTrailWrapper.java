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

    public String getUser() {

        return this.auditTrailNode.get(AuditTrailField.USER.getDbName()).asText();
    }

    public String getCreatedAt() {

        return this.auditTrailNode.get(AuditTrailField.CREATED_AT.getDbName()).asText();
    }

    public ChangeType getChangeType() {

        return ChangeType.valueOf(this.auditTrailNode.get(AuditTrailField.CHANGE_TYPE.getDbName()).asText());
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
