package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import java.util.Map;

/**
 * A CSAF selector that is also stored in a column of the list tables.
 *
 * @param column the column name
 * @param kind   how the value of the column is compared
 */
record ListColumn(String column, Kind kind) {

    /** How the value of a list column is compared. */
    enum Kind { TEXT, TIMESTAMP, TEXT_ARRAY }

    /**
     * CSAF selectors (without the optional leading {@code csaf}) that {@code advisories} also stores in a column, which
     * the csaf_extract_list_columns trigger keeps in sync. Only the paged list (v2) filters on them; the list v1
     * filters every CSAF selector by a JSON path on the document.
     * All selectors exist unchanged in CSAF 2.0 and 2.1. The derived CVSS scores and the revision history length are
     * not listed: they are aggregates (a maximum, a count) and do not answer a filter on a CSAF path identically.
     */
    static final Map<String, ListColumn> ADVISORIES = Map.ofEntries(
            Map.entry("document.tracking.id", new ListColumn("tracking_id", Kind.TEXT)),
            Map.entry("document.title", new ListColumn("document_title", Kind.TEXT)),
            Map.entry("document.tracking.version", new ListColumn("tracking_version", Kind.TEXT)),
            Map.entry("document.tracking.status", new ListColumn("tracking_status", Kind.TEXT)),
            Map.entry("document.category", new ListColumn("category", Kind.TEXT)),
            Map.entry("document.distribution.tlp.label", new ListColumn("tlp", Kind.TEXT)),
            Map.entry("document.publisher.name", new ListColumn("publisher_name", Kind.TEXT)),
            Map.entry("document.tracking.current_release_date",
                    new ListColumn("current_release_date", Kind.TIMESTAMP)),
            Map.entry("document.tracking.initial_release_date",
                    new ListColumn("initial_release_date", Kind.TIMESTAMP)),
            Map.entry("document.tracking.aliases", new ListColumn("aliases", Kind.TEXT_ARRAY)),
            Map.entry("vulnerabilities.cve", new ListColumn("cves", Kind.TEXT_ARRAY)),
            Map.entry("vulnerabilities.ids.text", new ListColumn("vul_ids", Kind.TEXT_ARRAY)));
}
