package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import java.util.Arrays;
import java.util.Optional;

/**
 * The fields the paged advisory list can be sorted by, each backed by a column of {@code advisories}.
 * The column names are fixed here and never taken from the request.
 */
public enum AdvisoryListSortField {
    WORKFLOW_STATE("workflowState", "workflow_state"),
    OWNER("owner", "owner"),
    LAST_UPDATED("lastUpdated", "updated_at"),
    CREATED_AT("createdAt", "created_at"),
    TRACKING_ID("trackingId", "tracking_id"),
    TITLE("title", "document_title"),
    CURRENT_RELEASE_DATE("currentReleaseDate", "current_release_date"),
    /** Sorted as text: {@code 1.10.0} comes before {@code 1.9.0}. */
    TRACKING_VERSION("trackingVersion", "tracking_version"),
    TRACKING_STATUS("trackingStatus", "tracking_status"),
    CATEGORY("category", "category"),
    TLP("tlp", "tlp");

    private final String apiName;
    private final String column;

    AdvisoryListSortField(String apiName, String column) {
        this.apiName = apiName;
        this.column = column;
    }

    public String getApiName() {
        return apiName;
    }

    String getColumn() {
        return column;
    }

    public static Optional<AdvisoryListSortField> fromApiName(String apiName) {
        return Arrays.stream(values()).filter(field -> field.apiName.equals(apiName)).findFirst();
    }
}
