package de.exxcellent.bsi.rest.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDate;

@ApiModel("AdvisoryCommentResponse")
public class AdvisoryCommentResponse {

    private final long commentId;

    private final long advisoryId;

    private final String createdBy;

    private final LocalDate createdAt;

    private final String advisoryVersion;

    private final String jsonName;

    private final String commentText;

    /**
     * an Answers references it's question
     */
    private final long questionId;


    public AdvisoryCommentResponse(long commentId, long advisoryId, String createdBy, LocalDate createdAt
            , String advisoryVersion, String jsonName, String commentText, long questionId) {
        this.commentId = commentId;
        this.advisoryId = advisoryId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.advisoryVersion = advisoryVersion;
        this.jsonName = jsonName;
        this.commentText = commentText;
        this.questionId = questionId;
    }

    @ApiModelProperty(value = "The unique if of the comment", example = "238745")
    public long getCommentId() {
        return commentId;
    }

    @ApiModelProperty(value = "Only in answers. The id of of the comment the answer belongs to", example = "923873")
    public long getQuestionId() {
        return questionId;
    }

    @ApiModelProperty(value = "The id advisory", example = "6677234")
    public long getAdvisoryId() {
        return advisoryId;
    }

    @ApiModelProperty(value = "The user which created the comment", example = "Mustermann")
    public String getCreatedBy() {
        return createdBy;
    }

    @ApiModelProperty(value = "The date when the comment was created", example = "2022-01-12T11:00:00.000Z")
    public LocalDate getCreatedAt() {
        return createdAt;
    }

    @ApiModelProperty(value = "The version of the advisory", example = "0.1.3")
    public String getAdvisoryVersion() {
        return advisoryVersion;
    }

    @ApiModelProperty(value = "The text of the comment", example = "Is this value correct?")
    public String getCommentText() {
        return commentText;
    }

    @ApiModelProperty(value = "A comment is added to an object in the CSAF document. This name specifies the " +
            "concrete value in the object the comment belongs to. When its empty, the comment belongs to the whole object.", example = "csaf_version")
    public String getJsonName() {
        return jsonName;
    }
}
