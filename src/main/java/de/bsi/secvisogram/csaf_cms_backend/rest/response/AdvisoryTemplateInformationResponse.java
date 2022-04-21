package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Entry in a list of advisory  template information's
 */
@ApiModel("AdvisoryTemplateInformation")
public class AdvisoryTemplateInformationResponse {

    private final long templateId;
    private final String templateDescription;

    public AdvisoryTemplateInformationResponse(long templateId, String templateDescription) {
        this.templateId = templateId;
        this.templateDescription = templateDescription;
    }

    @ApiModelProperty(value = "Unique Id of the template", example = "33476793")
    public long getTemplateId() {
        return templateId;
    }

    @ApiModelProperty(value = "Description of the template", example = "Template for profile ")
    public String getTemplateDescription() {
        return templateDescription;
    }
}
