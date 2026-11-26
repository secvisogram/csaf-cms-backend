package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One page of the advisory list.
 */
@Schema(name = "AdvisoryPage")
public record AdvisoryPageResponse(
        @Schema(description = "The advisories of this page, in sort order.")
        List<AdvisoryInformationPageResponse> items,
        @Schema(description = "The zero-based page number.", example = "0")
        int page,
        @Schema(description = "The requested page size.", example = "50")
        int size,
        @Schema(description = "The number of advisories matching the filter on all pages.", example = "123")
        long totalElements) {

    public AdvisoryPageResponse {
        items = List.copyOf(items);
    }
}
