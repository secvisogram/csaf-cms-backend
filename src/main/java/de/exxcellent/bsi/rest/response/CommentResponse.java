package de.exxcellent.bsi.rest.response;

import java.time.LocalDate;

public class CommentResponse {

    private long commentId;

    private long advisoryId;

    private String createdBy;

    private LocalDate createdAt;

    private String advisoryVersion;

    private String jsonPath;

    private String commentText;

    /**
     * Answers references it's question
     */
    private long questionId;

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
