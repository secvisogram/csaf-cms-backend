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
     * the csaf_extract_list_columns trigger keeps in sync. A filter on such a selector uses the column; every other
     * selector, and every filter the column cannot answer identically, falls back to a JSON path on the document.
     * All selectors exist unchanged in CSAF 2.0 and 2.1.
     */
    static final Map<String, ListColumn> ADVISORIES = Map.of(
            "document.tracking.id", new ListColumn("tracking_id", Kind.TEXT),
            "document.title", new ListColumn("document_title", Kind.TEXT),
            "document.tracking.version", new ListColumn("tracking_version", Kind.TEXT),
            "document.tracking.status", new ListColumn("tracking_status", Kind.TEXT),
            "document.category", new ListColumn("category", Kind.TEXT),
            "document.distribution.tlp.label", new ListColumn("tlp", Kind.TEXT),
            "document.tracking.current_release_date", new ListColumn("current_release_date", Kind.TIMESTAMP),
            "document.tracking.aliases", new ListColumn("aliases", Kind.TEXT_ARRAY),
            "vulnerabilities.cve", new ListColumn("cves", Kind.TEXT_ARRAY),
            "vulnerabilities.ids.text", new ListColumn("vul_ids", Kind.TEXT_ARRAY));

    /** The selectors of {@link #ADVISORIES} that {@code advisory_versions} also stores in a column. */
    static final Map<String, ListColumn> VERSIONS = Map.of(
            "document.tracking.id", ADVISORIES.get("document.tracking.id"),
            "document.title", ADVISORIES.get("document.title"),
            "document.tracking.current_release_date", ADVISORIES.get("document.tracking.current_release_date"));
}
