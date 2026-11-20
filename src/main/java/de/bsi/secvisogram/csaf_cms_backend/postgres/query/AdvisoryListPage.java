package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import java.util.List;

/**
 * One page of the advisory list and the number of all matching advisories.
 *
 * @param advisories the advisories of the page, in sort order
 * @param total      the number of advisories matching the filter on all pages
 */
public record AdvisoryListPage(List<AdvisoryListProjection> advisories, long total) {
}
