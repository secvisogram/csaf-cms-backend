package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Advisory content response
 */
@ApiModel("AdvisoryDocument")
public class AdvisoryResponse extends  AdvisoryInformationResponse {

    private final String revision;
    private final String csafJsonWithComments;

    public AdvisoryResponse(String advisoryId, WorkflowState status, String csafJsonWithComments) {
        super(advisoryId, status);
        this.csafJsonWithComments = csafJsonWithComments;
        this.revision = "2-efaa5db9409b2d4300535c70aaf6a66b";
    }

    @ApiModelProperty(value = "The current CASF document enhanced with comment ids ", example = "{" +
            "document: { $comment: [23454], category: generic_csaf,...")
    public String getCsafJsonWithComments() {
      return this.csafJsonWithComments;
    }

    @ApiModelProperty(value = "Document revision for optimistic concurrency", example = "2-efaa5db9409b2d4300535c70aaf6a66b")
    public String getRevision() {
        return revision;
    }
}
