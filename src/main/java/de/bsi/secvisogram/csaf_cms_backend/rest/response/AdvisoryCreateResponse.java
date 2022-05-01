package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdvisoryCreateResponse")
public class AdvisoryCreateResponse {

    private final String id;
    private final String revision;

    public AdvisoryCreateResponse(String id, String revision) {
        this.id = id;
        this.revision = revision;
    }

    @Schema(description = "The unique ID of the create object.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getId() {
        return id;
    }

    @Schema(
            description = "The document revision for optimistic concurrency.",
            example = "2-efaa5db9409b2d4300535c70aaf6a66b"
    )
    public String getRevision() {
        return revision;
    }
}
