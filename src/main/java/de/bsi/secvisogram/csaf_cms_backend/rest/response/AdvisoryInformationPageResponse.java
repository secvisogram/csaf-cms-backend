package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * An entry of the paged advisory list: the values of the list v1 plus every field the paged list can filter or
 * sort by. The values are taken from the CSAF document; absent values are null, absent arrays empty.
 */
@Schema(name = "AdvisoryDocumentPageInformation")
public class AdvisoryInformationPageResponse extends AdvisoryInformationResponse {

    private String trackingVersion;
    private String trackingStatus;
    private String category;
    private String tlp;
    private String initialReleaseDate;
    private String publisherName;
    private Integer revisionHistoryLength;
    private BigDecimal cvssV2Score;
    private BigDecimal cvssV3Score;
    private BigDecimal cvssV4Score;
    private BigDecimal critical;
    private List<String> cves = List.of();
    private List<String> vulnerabilityIds = List.of();
    private List<String> aliases = List.of();

    public AdvisoryInformationPageResponse(String advisoryId) {
        super(advisoryId);
    }

    @Schema(description = "document/tracking/version.", example = "1.0.0")
    public String getTrackingVersion() {
        return trackingVersion;
    }

    public void setTrackingVersion(String trackingVersion) {
        this.trackingVersion = trackingVersion;
    }

    @Schema(description = "document/tracking/status.", example = "final")
    public String getTrackingStatus() {
        return trackingStatus;
    }

    public void setTrackingStatus(String trackingStatus) {
        this.trackingStatus = trackingStatus;
    }

    @Schema(description = "document/category.", example = "csaf_security_advisory")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Schema(description = "document/distribution/tlp/label.", example = "WHITE")
    public String getTlp() {
        return tlp;
    }

    public void setTlp(String tlp) {
        this.tlp = tlp;
    }

    @Schema(description = "document/tracking/initial_release_date as ISO-8601 instant; null if absent or not a "
            + "date-time with offset.", example = "2024-01-31T08:00:00Z")
    public String getInitialReleaseDate() {
        return initialReleaseDate;
    }

    public void setInitialReleaseDate(String initialReleaseDate) {
        this.initialReleaseDate = initialReleaseDate;
    }

    @Schema(description = "document/publisher/name.", example = "Example Inc.")
    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    @Schema(description = "The number of entries in document/tracking/revision_history.", example = "3")
    public Integer getRevisionHistoryLength() {
        return revisionHistoryLength;
    }

    public void setRevisionHistoryLength(Integer revisionHistoryLength) {
        this.revisionHistoryLength = revisionHistoryLength;
    }

    @Schema(description = "The highest CVSS v2 base score over all vulnerabilities.", example = "7.5")
    public BigDecimal getCvssV2Score() {
        return cvssV2Score;
    }

    public void setCvssV2Score(BigDecimal cvssV2Score) {
        this.cvssV2Score = cvssV2Score;
    }

    @Schema(description = "The highest CVSS v3 base score over all vulnerabilities.", example = "9.8")
    public BigDecimal getCvssV3Score() {
        return cvssV3Score;
    }

    public void setCvssV3Score(BigDecimal cvssV3Score) {
        this.cvssV3Score = cvssV3Score;
    }

    @Schema(description = "The highest CVSS v4 base score over all vulnerabilities.", example = "8.7")
    public BigDecimal getCvssV4Score() {
        return cvssV4Score;
    }

    public void setCvssV4Score(BigDecimal cvssV4Score) {
        this.cvssV4Score = cvssV4Score;
    }

    @Schema(description = "cvssV4Score, else cvssV3Score, else cvssV2Score.", example = "9.8")
    public BigDecimal getCritical() {
        return critical;
    }

    public void setCritical(BigDecimal critical) {
        this.critical = critical;
    }

    @Schema(description = "vulnerabilities[]/cve.", example = "[\"CVE-2024-0001\"]")
    public List<String> getCves() {
        return cves;
    }

    public void setCves(List<String> cves) {
        this.cves = List.copyOf(cves);
    }

    @Schema(description = "vulnerabilities[]/ids[]/text.", example = "[\"VID-1\"]")
    public List<String> getVulnerabilityIds() {
        return vulnerabilityIds;
    }

    public void setVulnerabilityIds(List<String> vulnerabilityIds) {
        this.vulnerabilityIds = List.copyOf(vulnerabilityIds);
    }

    @Schema(description = "document/tracking/aliases[].", example = "[\"ALIAS-1\"]")
    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = List.copyOf(aliases);
    }
}
