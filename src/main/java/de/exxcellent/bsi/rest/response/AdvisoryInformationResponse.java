package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;

public class AdvisoryInformationResponse {

    private final long advisoryId;
    private final WorkflowState status;

    public AdvisoryInformationResponse(long advisoryId, WorkflowState status) {
        this.advisoryId = advisoryId;
        this.status = status;
    }

    public long getAdvisoryId() {
        return advisoryId;
    }

    public WorkflowState getStatus() {
        return status;
    }
}
