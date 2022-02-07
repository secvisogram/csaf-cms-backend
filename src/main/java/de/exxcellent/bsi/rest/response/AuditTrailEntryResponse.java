package de.exxcellent.bsi.rest.response;

import java.time.LocalDate;

public class AuditTrailEntryResponse {

    private final CommentChangeResponse commentChangeResponse;
    private final DocumentChangeResponse documentChangeResponse;
    private final WorkflowChangeResponse workflowChangeResponse;

    public AuditTrailEntryResponse(CommentChangeResponse commentChangeResponse
            , DocumentChangeResponse documentChangeResponse, WorkflowChangeResponse workflowChangeResponse) {
        this.commentChangeResponse = commentChangeResponse;
        this.documentChangeResponse = documentChangeResponse;
        this.workflowChangeResponse = workflowChangeResponse;
    }

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
