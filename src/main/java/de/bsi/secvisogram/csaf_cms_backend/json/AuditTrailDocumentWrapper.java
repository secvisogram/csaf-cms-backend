package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField;

public class AuditTrailDocumentWrapper extends AuditTrailWrapper {

    public static AuditTrailDocumentWrapper createNewFromPatch(JsonNode diffPatch) {

        ObjectNode rootNode = new ObjectMapper().createObjectNode();

        AuditTrailDocumentWrapper wrapper =  new AuditTrailDocumentWrapper(rootNode)
                .setDiffPatch(diffPatch);
        wrapper.setType(ObjectType.AuditTrailDocument)
                .setCreatedAtToNow();
        return wrapper;
    }


    private AuditTrailDocumentWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }

    public JsonNode getDiffPatch() {

        return this.getAuditTrailNode().get(AuditTrailField.DIFF.getDbName());
    }

    private AuditTrailDocumentWrapper setDiffPatch(JsonNode diff) {

        this.getAuditTrailNode().put(AuditTrailField.DIFF.getDbName(), diff);
        return this;
    }

}
