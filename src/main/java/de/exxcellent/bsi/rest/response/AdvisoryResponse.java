package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;

public class AdvisoryResponse extends  AdvisoryInformationResponse {

    private final String csafJson;

    public AdvisoryResponse(long advisoryId, WorkflowState status, String csafJson) {
        super(advisoryId, status);
        this.csafJson = csafJson;
    }

    public String getCsafJson() {
      return this.csafJson;
    }
}
