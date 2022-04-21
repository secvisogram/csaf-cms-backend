package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Advisory content response
 */
@ApiModel("AdvisoryDocument")
public class AdvisoryResponse extends  AdvisoryInformationResponse {

    private String revision;
    private String csafJsonWithComments;

    public AdvisoryResponse(String advisoryId, WorkflowState status, String csafJsonWithComments) {
        super(advisoryId, status);
        this.csafJsonWithComments = csafJsonWithComments;
    }

    @ApiModelProperty(value = "The current CASF document enhanced with comment ids ", example = "{" +
            "document: { $comment: [23454], category: generic_csaf,...")
    public String getCsafJsonWithComments() {
      return this.csafJsonWithComments;
    }

    public void setCsafJsonWithComments(String csafJsonWithComments) {
        this.csafJsonWithComments = csafJsonWithComments;
    }

    @ApiModelProperty(value = "Document revision for optimistic concurrency", example = "2-efaa5db9409b2d4300535c70aaf6a66b")
    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}
