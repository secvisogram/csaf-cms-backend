package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * One page of the advisory list and the number of all matching advisories.
 *
 * @param advisories the advisories of the page, in sort order
 * @param total      the number of advisories matching the filter on all pages
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Short-lived result holder, the list is not shared")
public record AdvisoryListPage(List<AdvisoryPageProjection> advisories, long total) {
}
