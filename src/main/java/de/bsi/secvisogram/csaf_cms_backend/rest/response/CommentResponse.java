package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdvisoryCommentResponse")
public class CommentResponse {

    private final String commentId;

    private String revision;

    private final String advisoryId;

    private final String createdBy;

    private final String commentText;

    private final String field;

    /**
     * An answer needs to reference the question it answers.
     */
    private final String answerToId;


    public CommentResponse(
            String commentId,
            String revision,
            String advisoryId,
            String createdBy,
            String commentText,
            String field,
            String answerToId
    ) {
        this.commentId = commentId;
        this.revision = revision;
        this.advisoryId = advisoryId;
        this.createdBy = createdBy;
        this.commentText = commentText;
        this.field = field;
        this.answerToId = answerToId;
    }

    @Schema(description = "The unique ID of the comment.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getCommentId() {
        return commentId;
    }

    @Schema(
            description = "Only present if the comment is an answer. The ID of the comment the answer belongs to.",
            example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5"
    )
    public String getAnswerToId() {
        return answerToId;
    }

    @Schema(description = "The ID of the advisory.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getAdvisoryId() {
        return advisoryId;
    }

    @Schema(description = "The user who created the comment.", example = "Mustermann")
    public String getCreatedBy() {
        return createdBy;
    }

    @Schema(description = "The text of the comment.", example = "Is this value correct?")
    public String getCommentText() {
        return commentText;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Schema(description = "The path to a nested leaf field when the comment is not on an object", example = "category")
    public String getField() {
        return field;
    }
}
