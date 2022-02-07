package de.exxcellent.bsi.rest.response;

import java.time.LocalDate;

public class CommentResponse {

    private final long commentId;

    private final long advisoryId;

    private final String createdBy;

    private final LocalDate createdAt;

    private final String advisoryVersion;

    private final String jsonPath;

    private final String commentText;

    /**
     * an Answers references it's question
     */
    private final long questionId;


    public CommentResponse(long commentId, long advisoryId, String createdBy, LocalDate createdAt
            , String advisoryVersion, String jsonPath, String commentText, long questionId) {
        this.commentId = commentId;
        this.advisoryId = advisoryId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.advisoryVersion = advisoryVersion;
        this.jsonPath = jsonPath;
        this.commentText = commentText;
        this.questionId = questionId;
    }

    public long getCommentId() {
        return commentId;
    }

    public long getQuestionId() {
        return questionId;
    }

    public long getAdvisoryId() {
        return advisoryId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public String getAdvisoryVersion() {
        return advisoryVersion;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public String getCommentText() {
        return commentText;
    }
}
