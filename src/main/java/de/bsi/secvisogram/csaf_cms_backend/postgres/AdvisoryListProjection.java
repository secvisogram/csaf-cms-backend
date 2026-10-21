package de.bsi.secvisogram.csaf_cms_backend.postgres;

/**
 * Scalar values required to render an advisory in the list endpoint.
 * The complete CSAF JSON document is deliberately not part of this projection.
 */
public record AdvisoryListProjection(
        String advisoryId,
        String workflowState,
        String owner,
        Long version,
        String documentTrackingId,
        String title,
        String currentReleaseDate) {
}