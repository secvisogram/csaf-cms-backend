package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Advisory content response
 */
@ApiModel("AdvisoryDocument")
public class AdvisoryResponse extends  AdvisoryInformationResponse {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final long revision;
    private final String csafJsonWithComments;

    public AdvisoryResponse(long advisoryId, WorkflowState status, String csafJsonWithComments) {
        super(advisoryId, status);
        this.csafJsonWithComments = csafJsonWithComments;
        this.revision = RANDOM.nextLong();
    }

    @ApiModelProperty(value = "The current CASF document enhanced with comment ids ", example = "{" +
            "document: { $comment: [23454], category: generic_csaf,...")
    public String getCsafJsonWithComments() {
      return this.csafJsonWithComments;
    }

    @ApiModelProperty(value = "Document revision for optimistic concurrency", example = "52303295832")
    public long getRevision() {
        return revision;
    }
}
