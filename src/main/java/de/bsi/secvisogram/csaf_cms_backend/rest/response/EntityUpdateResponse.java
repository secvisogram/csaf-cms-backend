package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EntityUpdateResponse")
public class EntityUpdateResponse {

    private final String revision;

    public EntityUpdateResponse(String revision) {
        this.revision = revision;
    }

    @Schema(
            description = "The document revision for optimistic concurrency.",
            example = "2-efaa5db9409b2d4300535c70aaf6a66b"
    )
    public String getRevision() {
        return revision;
    }
}
