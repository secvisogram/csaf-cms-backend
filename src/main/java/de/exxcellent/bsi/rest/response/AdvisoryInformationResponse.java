package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;

public class AdvisoryInformationResponse {

    private String advisoryId;
    private WorkflowState status;


    public String getAdvisoryId() {
        return advisoryId;
    }

    public WorkflowState getStatus() {
        return status;
    }
}
