package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField;
import java.time.Instant;

public class AuditTrailDocumentWrapper {

    public enum ChangeType {
        CREATED,
        UPDATED
    }

    public static AuditTrailDocumentWrapper createNewFromPatch(JsonNode diffPatch) {

        ObjectNode rootNode = new ObjectMapper().createObjectNode();

        return new AuditTrailDocumentWrapper(rootNode)
                .setType(ObjectType.AuditTrailDocument)
                .setCreatedAtToNow()
                .setDiffPatch(diffPatch);
    }

    private final ObjectNode auditTrailNode;

    private AuditTrailDocumentWrapper(ObjectNode auditTrailNode) {
        this.auditTrailNode = auditTrailNode;
    }

    public String advisoryAsString() {

        return this.auditTrailNode.toString();
    }

    private AuditTrailDocumentWrapper setType(ObjectType newValue) {

        this.auditTrailNode.put(CouchDbField.TYPE_FIELD.getDbName(), newValue.name());
        return this;
    }

    public JsonNode getDiffPatch() {

        return this.auditTrailNode.get(AuditTrailField.DIFF.getDbName());
    }

    public String getAdvisoryId() {

        return this.auditTrailNode.get(AuditTrailField.ADVISORY_ID.getDbName()).asText();
    }

    public String getDocVersion() {

        return this.auditTrailNode.get(AuditTrailField.OLD_DOC_VERSION.getDbName()).asText();
    }

    public String getOldDocVersion() {

        return this.auditTrailNode.get(AuditTrailField.DOC_VERSION.getDbName()).asText();
    }

    public String getUser() {

        return this.auditTrailNode.get(AuditTrailField.USER.getDbName()).asText();
    }

    public String getCreatedAt() {

        return this.auditTrailNode.get(AuditTrailField.CREATED_AT.getDbName()).asText();
    }

    public ChangeType getChangeType() {

        return ChangeType.valueOf(this.auditTrailNode.get(AuditTrailField.CREATED_AT.getDbName()).asText());
    }

    private AuditTrailDocumentWrapper setDiffPatch(JsonNode diff) {

        this.auditTrailNode.put(AuditTrailField.DIFF.getDbName(), diff);
        return this;
    }


    public AuditTrailDocumentWrapper setAdvisoryId(String newValue) {

        this.auditTrailNode.put(AuditTrailField.ADVISORY_ID.getDbName(), newValue);
        return this;
    }

    public AuditTrailDocumentWrapper setDocVersion(String newValue) {

        this.auditTrailNode.put(AuditTrailField.OLD_DOC_VERSION.getDbName(), newValue);
        return this;
    }

    public AuditTrailDocumentWrapper setOldDocVersion(String newValue) {

        this.auditTrailNode.put(AuditTrailField.DOC_VERSION.getDbName(), newValue);
        return this;
    }

    public AuditTrailDocumentWrapper setUser(String newValue) {

        this.auditTrailNode.put(AuditTrailField.USER.getDbName(), newValue);
        return this;
    }

    public AuditTrailDocumentWrapper setCreatedAtToNow() {

        this.auditTrailNode.put(AuditTrailField.CREATED_AT.getDbName(), Instant.now().toString());
        return this;
    }

    public AuditTrailDocumentWrapper setChangeType(ChangeType newValue) {

        this.auditTrailNode.put(AuditTrailField.CREATED_AT.getDbName(), newValue.name());
        return this;
    }

}
