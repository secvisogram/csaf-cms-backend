package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryAuditTrailField;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;

/**
 * Wrapper around JsonNode to read and write audit trail objects for advisory workflow changes from/to the CouchDB
 */
public class AdvisoryAuditTrailWorkflowWrapper extends AdvisoryAuditTrailWrapper {

    /**
     * Create an AuditTrailWorkflowWrapper for a change in the workflow state of anadvisory
     *
     * @param newWorkflowState the new state
     * @param oldWorkflowState the old state
     * @return the new wrapper
     */
    public static AdvisoryAuditTrailWorkflowWrapper createNewFrom(WorkflowState newWorkflowState, WorkflowState oldWorkflowState) {

        ObjectNode rootNode = new ObjectMapper().createObjectNode();

        AdvisoryAuditTrailWorkflowWrapper wrapper = new AdvisoryAuditTrailWorkflowWrapper(rootNode)
                .setNewWorkflowState(newWorkflowState)
                .setOldWorkflowState(oldWorkflowState);
        wrapper.setType(ObjectType.AuditTrailWorkflow)
                .setCreatedAtToNow();
        return wrapper;
    }


    private AdvisoryAuditTrailWorkflowWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }

    public String getOldWorkflowState() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.OLD_WORKFLOW_STATE.getDbName()).asText();
    }

    public String getNewWorkflowState() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.NEW_WORKFLOW_STATE.getDbName()).asText();
    }

    public AdvisoryAuditTrailWorkflowWrapper setOldWorkflowState(WorkflowState newValue) {

        this.getAuditTrailNode().put(AdvisoryAuditTrailField.OLD_WORKFLOW_STATE.getDbName(), newValue.name());
        return this;
    }

    public AdvisoryAuditTrailWorkflowWrapper setNewWorkflowState(WorkflowState newValue) {

        this.getAuditTrailNode().put(AdvisoryAuditTrailField.NEW_WORKFLOW_STATE.getDbName(), newValue.name());
        return this;
    }
}
