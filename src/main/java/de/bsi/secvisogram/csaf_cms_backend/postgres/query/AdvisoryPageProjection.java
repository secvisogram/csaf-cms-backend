package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;

/**
 * An advisory of the paged list: the values of the list v1 plus every column the paged list can filter or sort by.
 * The complete CSAF JSON document is deliberately not part of this projection.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Short-lived result holder, the lists are immutable")
public record AdvisoryPageProjection(
        AdvisoryListProjection base,
        String trackingVersion,
        String trackingStatus,
        String category,
        String tlp,
        String initialReleaseDate,
        String publisherName,
        Integer revisionHistoryLength,
        BigDecimal cvssV2Score,
        BigDecimal cvssV3Score,
        BigDecimal cvssV4Score,
        BigDecimal critical,
        List<String> cves,
        List<String> vulnerabilityIds,
        List<String> aliases) {
}
