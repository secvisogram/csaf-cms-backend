package de.bsi.secvisogram.csaf_cms_backend.json;

import de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryAuditTrailField;
import de.bsi.secvisogram.csaf_cms_backend.model.ChangeType;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Wrapper around JsonNode to read and write audit trail objects for advisory workflow changes from/to the CouchDB
 */
public class AdvisoryAuditTrailWorkflowWrapper extends AdvisoryAuditTrailWrapper {

    /**
     * Create an AuditTrailWorkflowWrapper for a change in the workflow state of an advisory
     *
     * @param newWorkflowState the new state
     * @param oldWorkflowState the old state
     * @return the new wrapper
     */
    public static AdvisoryAuditTrailWorkflowWrapper createNewFrom(WorkflowState newWorkflowState, WorkflowState oldWorkflowState) {

        ObjectNode rootNode = new JsonMapper().createObjectNode();

        AdvisoryAuditTrailWorkflowWrapper wrapper = new AdvisoryAuditTrailWorkflowWrapper(rootNode)
                .setNewWorkflowState(newWorkflowState)
                .setOldWorkflowState(oldWorkflowState);
        wrapper.setType(ObjectType.AuditTrailWorkflow)
                .setCreatedAtToNow()
                .setChangeType(ChangeType.Update);
        return wrapper;
    }


    private AdvisoryAuditTrailWorkflowWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }

    public String getOldWorkflowState() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.OLD_WORKFLOW_STATE.getDbName()).asString();
    }

    public String getNewWorkflowState() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.NEW_WORKFLOW_STATE.getDbName()).asString();
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
