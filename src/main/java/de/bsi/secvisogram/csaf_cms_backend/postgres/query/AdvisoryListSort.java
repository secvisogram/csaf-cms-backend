package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

/** One sort criterion of the paged advisory list. */
public record AdvisoryListSort(AdvisoryListSortField field, boolean descending) {
}
