package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AdvisoryUpdateResponse")
public class AdvisoryUpdateResponse {

    private final String revision;

    public AdvisoryUpdateResponse(String revision) {
        this.revision = revision;
    }

    @ApiModelProperty(value = "Revision for optimistic concurrency", example = "2-efaa5db9409b2d4300535c70aaf6a66b")
    public String getRevision() {
        return revision;
    }
}
