package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;

/**
 * Wrapper around JsonNode to read and write audit trail objects for advisory workflow changes from/to the CouchDB
 */
public class AuditTrailWorkflowWrapper extends AuditTrailWrapper {

    /**
     * Create an AuditTrailWorkflowWrapper for a change in the workflow state of an advisory
     * @param newWorkflowState the new state
     * @param oldWorkflowState the old state
     * @return the new wrapper
     */
    public static AuditTrailWorkflowWrapper createNewFrom(WorkflowState newWorkflowState, WorkflowState oldWorkflowState) {

        ObjectNode rootNode = new ObjectMapper().createObjectNode();

        AuditTrailWorkflowWrapper wrapper =  new AuditTrailWorkflowWrapper(rootNode)
                .setNewWorkflowState(newWorkflowState)
                .setOldWorkflowState(oldWorkflowState);
        wrapper.setType(ObjectType.AuditTrailWorkflow)
                .setCreatedAtToNow();
        return wrapper;
    }


    private AuditTrailWorkflowWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }

    public String getOldWorkflowState() {

        return this.getAuditTrailNode().get(AuditTrailField.OLD_WORKFLOW_STATE.getDbName()).asText();
    }

    public String getNewWorkflowState() {

        return this.getAuditTrailNode().get(AuditTrailField.NEW_WORKFLOW_STATE.getDbName()).asText();
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
