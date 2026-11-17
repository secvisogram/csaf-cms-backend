package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import java.util.List;

/**
 * One page of the advisory list and the number of all matching entries.
 *
 * @param entries the entries of the page, in sort order
 * @param total   the number of entries matching the filter on all pages
 */
public record AdvisoryListPage(List<Entry> entries, long total) {

    /**
     * An entry of the page.
     *
     * @param version    true if the entry is a snapshot from {@code advisory_versions}; its
     *                   {@code projection.version()} is then null and {@code lastUpdated} is the snapshot time
     * @param projection the list values of the advisory or the snapshot
     */
    public record Entry(boolean version, AdvisoryListProjection projection) {
    }
}
