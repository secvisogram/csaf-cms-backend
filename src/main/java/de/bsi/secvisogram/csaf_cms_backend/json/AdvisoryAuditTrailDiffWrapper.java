package de.bsi.secvisogram.csaf_cms_backend.json;

import de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryAuditTrailField;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Wrapper around JsonNode to read and write audit trail objects for CSAF document changes from/to the CouchDB
 */
public class AdvisoryAuditTrailDiffWrapper extends AdvisoryAuditTrailWrapper {

    /**
     * Calculate a CSAF document diff in JSON Patch format for the given AdvisoryWrapper
     * and create an AuditTrailDocumentWrapper for this diff.
     * @param oldAdvisory the old advisory
     * @param newAdvisory the new advisory
     * @return the new wrapper
     */
    public static AdvisoryAuditTrailDiffWrapper createNewFromAdvisories(AdvisoryWrapper oldAdvisory, AdvisoryWrapper newAdvisory) {

        JsonNode diffPatch = oldAdvisory.calculateDiffTo(newAdvisory);
        ObjectNode rootNode = new JsonMapper().createObjectNode();

        AdvisoryAuditTrailDiffWrapper wrapper =  new AdvisoryAuditTrailDiffWrapper(rootNode)
                .setDiffPatch(diffPatch);
        wrapper.setDocVersion(newAdvisory.getDocumentTrackingVersion())
                .setOldDocVersion(oldAdvisory.getDocumentTrackingVersion())
                .setType(ObjectType.AuditTrailDocument)
                .setCreatedAtToNow();
        return wrapper;
    }

    private AdvisoryAuditTrailDiffWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }

    public JsonNode getDiffPatch() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.DIFF.getDbName());
    }

    private AdvisoryAuditTrailDiffWrapper setDiffPatch(JsonNode diff) {

        this.getAuditTrailNode().set(AdvisoryAuditTrailField.DIFF.getDbName(), diff);
        return this;
    }

}
