package de.bsi.secvisogram.csaf_cms_backend.config;

/**
 * Configuration for the advisory workflow behavior.
 */
public class CsafWorkflowConfiguration {

    /**
     * Controls whether a user is allowed to approve (Review → Approved) their own advisory.
     * When {@code false} (the default), a Reviewer or Publisher cannot move their own document
     * from {@code Review} to {@code Approved}, enforcing a peer-review separation of concerns.
     * When {@code true}, own-document approval is permitted.
     */
    private boolean allowOwnDocumentsApproved = false;

    public boolean isAllowOwnDocumentsApproved() {
        return allowOwnDocumentsApproved;
    }

    public CsafWorkflowConfiguration setAllowOwnDocumentsApproved(boolean allowOwnDocumentsApproved) {
        this.allowOwnDocumentsApproved = allowOwnDocumentsApproved;
        return this;
    }
}
