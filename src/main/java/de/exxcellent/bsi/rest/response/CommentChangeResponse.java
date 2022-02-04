package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.ChangeType;

public class CommentChangeResponse  extends AuditTrailEntryResponse {

    ChangeType changeType;
    String newComment;
}
