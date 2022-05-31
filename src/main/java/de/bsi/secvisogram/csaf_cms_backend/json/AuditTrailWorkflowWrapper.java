package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;

public class AuditTrailWorkflowWrapper extends AuditTrailWrapper {

    public static AuditTrailWorkflowWrapper createNewFrom(WorkflowState newWorkflowState, WorkflowState oldWorkflowState) {

        ObjectNode rootNode = new ObjectMapper().createObjectNode();

        AuditTrailWorkflowWrapper wrapper =  new AuditTrailWorkflowWrapper(rootNode)
                .setNewWorkflowState(newWorkflowState)
                .setOldWorkflowState(oldWorkflowState);
        wrapper.setType(ObjectType.AuditTrailDocument)
                .setCreatedAtToNow();
        return wrapper;
    }


    private AuditTrailWorkflowWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }

    public JsonNode getOldWorkflowState() {

        return this.getAuditTrailNode().get(AuditTrailField.OLD_WORKFLOW_STATE.getDbName());
    }

    public JsonNode getNewWorkflowState() {

        return this.getAuditTrailNode().get(AuditTrailField.NEW_WORKFLOW_STATE.getDbName());
    }

    public AuditTrailWorkflowWrapper setOldWorkflowState(WorkflowState newValue) {

        this.getAuditTrailNode().put(AuditTrailField.OLD_WORKFLOW_STATE.getDbName(), newValue.name());
        return this;
    }

    public AuditTrailWorkflowWrapper setNewWorkflowState(WorkflowState newValue) {

        this.getAuditTrailNode().put(AuditTrailField.NEW_WORKFLOW_STATE.getDbName(), newValue.name());
        return this;
    }
}
