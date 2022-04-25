package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AdvisoryCreateResponse")
public class AdvisoryCreateResponse {

    private final String id;
    private final String revision;

    public AdvisoryCreateResponse(String id, String revision) {
        this.id = id;
        this.revision = revision;
    }

    @ApiModelProperty(value = "Unique Id of the create object", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getId() {
        return id;
    }

    @ApiModelProperty(value = "Revision for optimistic concurrency", example = "2-efaa5db9409b2d4300535c70aaf6a66b")
    public String getRevision() {
        return revision;
    }
}
