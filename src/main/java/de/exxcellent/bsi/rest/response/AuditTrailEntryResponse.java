package de.exxcellent.bsi.rest.response;

import java.time.LocalDate;

public class AuditTrailEntryResponse {

    private CommentChangeResponse commentChangeResponse;
    private DocumentChangeResponse documentChangeResponse;
    private WorkflowChangeResponse workflowChangeResponse;

    public CommentChangeResponse getCommentChangeResponse() {
        return commentChangeResponse;
    }

    public DocumentChangeResponse getDocumentChangeResponse() {
        return documentChangeResponse;
    }

    public WorkflowChangeResponse getWorkflowChangeResponse() {
        return workflowChangeResponse;
    }
}
