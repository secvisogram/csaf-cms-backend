package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;

public class WorkflowChangeResponse extends ChangeResponse {

    private WorkflowState oldState;
    private WorkflowState newState;

    public WorkflowState getOldState() {
        return oldState;
    }

    public WorkflowState getNewState() {
        return newState;
    }
}
