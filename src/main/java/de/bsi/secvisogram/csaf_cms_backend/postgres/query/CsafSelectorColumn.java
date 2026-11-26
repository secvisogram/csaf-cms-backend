package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import java.util.Map;

/**
 * The column that stores the value of a CSAF selector, so that the paged list (v2) can filter on the column instead of
 * a JSON path on the document.
 *
 * @param column the column name
 * @param kind   how the value of the column is compared
 */
record CsafSelectorColumn(String column, Kind kind) {

    /** How the value of the column is compared. */
    enum Kind { TEXT, TIMESTAMP, TEXT_ARRAY }

    /**
     * CSAF selectors (without the optional leading {@code csaf}) that {@code advisories} also stores in a column, which
     * are generated from the csaf_jsonb column. Only the paged list (v2) filters on them; the list v1
     * filters every CSAF selector by a JSON path on the document.
     * All selectors exist unchanged in CSAF 2.0 and 2.1. The derived CVSS scores and the revision history length are
     * not listed: they are aggregates (a maximum, a count) and do not answer a filter on a CSAF path identically.
     */
    static final Map<String, CsafSelectorColumn> ADVISORIES = Map.ofEntries(
            Map.entry("document.tracking.id", new CsafSelectorColumn("tracking_id", Kind.TEXT)),
            Map.entry("document.title", new CsafSelectorColumn("document_title", Kind.TEXT)),
            Map.entry("document.tracking.version", new CsafSelectorColumn("tracking_version", Kind.TEXT)),
            Map.entry("document.tracking.status", new CsafSelectorColumn("tracking_status", Kind.TEXT)),
            Map.entry("document.category", new CsafSelectorColumn("category", Kind.TEXT)),
            Map.entry("document.distribution.tlp.label", new CsafSelectorColumn("tlp", Kind.TEXT)),
            Map.entry("document.publisher.name", new CsafSelectorColumn("publisher_name", Kind.TEXT)),
            Map.entry("document.tracking.current_release_date",
                    new CsafSelectorColumn("current_release_date", Kind.TIMESTAMP)),
            Map.entry("document.tracking.initial_release_date",
                    new CsafSelectorColumn("initial_release_date", Kind.TIMESTAMP)),
            Map.entry("document.tracking.aliases", new CsafSelectorColumn("aliases", Kind.TEXT_ARRAY)),
            Map.entry("vulnerabilities.cve", new CsafSelectorColumn("cves", Kind.TEXT_ARRAY)),
            Map.entry("vulnerabilities.ids.text", new CsafSelectorColumn("vul_ids", Kind.TEXT_ARRAY)));
}
