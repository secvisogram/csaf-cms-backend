package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;

public class WorkflowChangeResponse extends AuditTrailEntryResponse {

    private WorkflowState oldState;
    private WorkflowState newState;
}
