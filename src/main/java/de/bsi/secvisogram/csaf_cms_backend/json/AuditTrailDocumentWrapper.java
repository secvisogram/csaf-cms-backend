package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField;

/**
 * Wrapper around JsonNode to read and write audit trail objects for CSAF document changes from/to the CouchDB
 */
public class AuditTrailDocumentWrapper extends AuditTrailWrapper {

    /**
     * Calculate a CSAF document diff in JSON Patch format for the given AdvisoryWrapper
     * and create an AuditTrailDocumentWrapper for this diff.
     * @param oldAdvisory the old advisory
     * @param newAdvisory the new advisory
     * @return the new wrapper
     */
    public static AuditTrailDocumentWrapper createNewFromAdvisories(AdvisoryWrapper oldAdvisory, AdvisoryWrapper newAdvisory) {

        JsonNode diffPatch = oldAdvisory.calculateDiffTo(newAdvisory);
        ObjectNode rootNode = new ObjectMapper().createObjectNode();

        AuditTrailDocumentWrapper wrapper =  new AuditTrailDocumentWrapper(rootNode)
                .setDiffPatch(diffPatch);
        wrapper.setType(ObjectType.AuditTrailDocument)
                .setCreatedAtToNow()
                .setDocVersion(newAdvisory.getDocumentTrackingVersion())
                .setOldDocVersion(oldAdvisory.getDocumentTrackingVersion());
        return wrapper;
    }


    private AuditTrailDocumentWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }

    public JsonNode getDiffPatch() {

        return this.getAuditTrailNode().get(AuditTrailField.DIFF.getDbName());
    }

    private AuditTrailDocumentWrapper setDiffPatch(JsonNode diff) {

        this.getAuditTrailNode().set(AuditTrailField.DIFF.getDbName(), diff);
        return this;
    }

}
