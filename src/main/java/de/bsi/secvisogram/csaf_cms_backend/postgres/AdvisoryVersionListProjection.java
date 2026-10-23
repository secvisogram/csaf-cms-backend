package de.bsi.secvisogram.csaf_cms_backend.postgres;

/**
 * Scalar values required to render an advisory version snapshot in the list endpoint.
 * The complete CSAF JSON document is deliberately not part of this projection.
 */
public record AdvisoryVersionListProjection(
        String advisoryId,
        String workflowState,
        String owner,
        String documentTrackingId,
        String title,
        String currentReleaseDate) {
}