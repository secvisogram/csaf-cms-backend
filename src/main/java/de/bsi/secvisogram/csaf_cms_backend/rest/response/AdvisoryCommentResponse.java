package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name="AdvisoryCommentResponse")
public class AdvisoryCommentResponse {

    private final long commentId;

    private final long advisoryId;

    private final String createdBy;

    private final LocalDate createdAt;

    private final String advisoryVersion;

    private final String fieldName;

    private final String commentText;

    /**
     * an Answers references it's question
     */
    private final long questionId;


    public AdvisoryCommentResponse(long commentId, long advisoryId, String createdBy, LocalDate createdAt
            , String advisoryVersion, String fieldName, String commentText, long questionId) {
        this.commentId = commentId;
        this.advisoryId = advisoryId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.advisoryVersion = advisoryVersion;
        this.fieldName = fieldName;
        this.commentText = commentText;
        this.questionId = questionId;
    }

    @Schema(description = "The unique if of the comment", example = "238745")
    public long getCommentId() {
        return commentId;
    }

    @Schema(description = "Only in answers. The id of of the comment the answer belongs to", example = "923873")
    public long getQuestionId() {
        return questionId;
    }

    @Schema(description  = "The id advisory", example = "6677234")
    public long getAdvisoryId() {
        return advisoryId;
    }

    @Schema(description  = "The user which created the comment", example = "Mustermann")
    public String getCreatedBy() {
        return createdBy;
    }

    @Schema(description  = "The date when the comment was created", example = "2022-01-12T11:00:00.000Z")
    public LocalDate getCreatedAt() {
        return createdAt;
    }

    @Schema(description  = "The version of the advisory", example = "0.1.3")
    public String getAdvisoryVersion() {
        return advisoryVersion;
    }

    @Schema(description  = "The text of the comment", example = "Is this value correct?")
    public String getCommentText() {
        return commentText;
    }

    @Schema(description  = "A comment is added to an object in the CSAF document. This name specifies the field name of the " +
            "concrete value in the object the comment belongs to. When its empty, the comment belongs to the whole object.", example = "csaf_version")
    public String getFieldName() {
        return fieldName;
    }
}
