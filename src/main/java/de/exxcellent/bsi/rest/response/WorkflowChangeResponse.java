package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;

import java.time.LocalDate;

public class WorkflowChangeResponse extends ChangeResponse {

    private final WorkflowState oldState;
    private final WorkflowState newState;

    public WorkflowChangeResponse(long advisoryId, String advisoryVersion, String userId, LocalDate createdAt
            , WorkflowState oldState, WorkflowState newState) {
        super(advisoryId, advisoryVersion, userId, createdAt);
        this.oldState = oldState;
        this.newState = newState;
    }

    public WorkflowState getOldState() {
        return oldState;
    }

    public WorkflowState getNewState() {
        return newState;
    }
}
