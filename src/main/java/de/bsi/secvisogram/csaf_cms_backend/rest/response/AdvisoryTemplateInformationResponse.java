package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Entry in a list of advisory template information items
 */
@Schema(name = "AdvisoryTemplateInformation")
public class AdvisoryTemplateInformationResponse {

    private final long templateId;
    private final String templateDescription;

    public AdvisoryTemplateInformationResponse(long templateId, String templateDescription) {
        this.templateId = templateId;
        this.templateDescription = templateDescription;
    }

    @Schema(description = "The unique ID of the template.", example = "33476793")
    public long getTemplateId() {
        return templateId;
    }

    @Schema(description = "The description of the template.", example = "Template for the profile CSAF base.")
    public String getTemplateDescription() {
        return templateDescription;
    }
}
