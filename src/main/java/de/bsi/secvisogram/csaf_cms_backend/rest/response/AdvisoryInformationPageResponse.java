package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A single page of advisory information items returned when the list endpoint is called with a
 * {@code limit} query parameter. The element schema is identical to the legacy bare-array response;
 * only the surrounding envelope (bookmark, hasMore, limit) is added.
 */
@Schema(name = "AdvisoryDocumentInformationPage")
public class AdvisoryInformationPageResponse {

    private final List<AdvisoryInformationResponse> advisories;
    @Nullable
    private final String bookmark;
    private final boolean hasMore;
    private final int limit;

    public AdvisoryInformationPageResponse(List<AdvisoryInformationResponse> advisories,
                                           @Nullable String bookmark, boolean hasMore, int limit) {
        this.advisories = List.copyOf(advisories);
        this.bookmark = bookmark;
        this.hasMore = hasMore;
        this.limit = limit;
    }

    @ArraySchema(
            arraySchema = @Schema(description = "The advisories of this page, at most 'limit' items."),
            schema = @Schema(implementation = AdvisoryInformationResponse.class)
    )
    public List<AdvisoryInformationResponse> getAdvisories() {
        return Collections.unmodifiableList(advisories);
    }

    @Schema(
            description = "Opaque cursor for the next page, or null on the last page."
                          + " Clients must echo it back verbatim.",
            example = "eyJtYW5nb0Jvb2ttYXJrIjoiZzFBQUFBLi4uIn0",
            nullable = true
    )
    @Nullable
    public String getBookmark() {
        return bookmark;
    }

    @Schema(
            description = "True if another page exists. When false, bookmark is null.",
            example = "true"
    )
    public boolean isHasMore() {
        return hasMore;
    }

    @Schema(description = "Echo of the effective limit applied to this page.", example = "50")
    public int getLimit() {
        return limit;
    }
}
