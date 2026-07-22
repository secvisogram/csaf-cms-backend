package de.bsi.secvisogram.csaf_cms_backend.config;

/**
 * Configuration for the advisory workflow behavior.
 */
public class CsafWorkflowConfiguration {

    /**
     * Controls whether a user is allowed to approve (Review -> Approved) their own advisory.
     * When {@code false} (the default), a Reviewer or Publisher cannot move their own document
     * from {@code Review} to {@code Approved}, enforcing a peer-review separation of concerns.
     * When {@code true}, own-document approval is permitted.
     */
    private boolean allowOwnDocumentsApproved = false;

    /**
     * Controls whether an HTML reference is automatically added to {@code document/references}
     * when a CSAF advisory is published for the first time.
     * When {@code false} (the default), only the JSON reference is added.
     * Set to {@code true} to also add an HTML reference pointing to the {@code .html} variant of
     * the published document.
     */
    private boolean createHtmlReference = false;

    public boolean isAllowOwnDocumentsApproved() {
        return allowOwnDocumentsApproved;
    }

    public CsafWorkflowConfiguration setAllowOwnDocumentsApproved(boolean allowOwnDocumentsApproved) {
        this.allowOwnDocumentsApproved = allowOwnDocumentsApproved;
        return this;
    }

    public boolean isCreateHtmlReference() {
        return createHtmlReference;
    }

    public CsafWorkflowConfiguration setCreateHtmlReference(boolean createHtmlReference) {
        this.createHtmlReference = createHtmlReference;
        return this;
    }
}
