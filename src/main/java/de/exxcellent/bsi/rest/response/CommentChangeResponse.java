package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.ChangeType;

import java.time.LocalDate;

public class CommentChangeResponse extends ChangeResponse {

    private final ChangeType changeType;
    private final String newComment;

    public CommentChangeResponse(long advisoryId, String advisoryVersion, String userId, LocalDate createdAt
            , ChangeType changeType, String newComment) {
        super(advisoryId, advisoryVersion, userId, createdAt);
        this.changeType = changeType;
        this.newComment = newComment;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getNewComment() {
        return newComment;
    }
}
