package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.ChangeType;

public class CommentChangeResponse extends ChangeResponse {

    ChangeType changeType;
    String newComment;

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getNewComment() {
        return newComment;
    }
}
