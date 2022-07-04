package de.bsi.secvisogram.csaf_cms_backend.rest.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Comment")
public class Comment {

    private String commentText;
    private String csafNodeId;
    private String fieldName;

    public Comment(String commentText, String csafNodeId) {
        this.commentText = commentText;
        this.csafNodeId = csafNodeId;
    }

    @JsonCreator
    public Comment(@JsonProperty(value = "commentText", required = true) String commentText,
                   @JsonProperty(value = "csafNodeId", required = true) String csafNodeId,
                   @JsonProperty("fieldName") String fieldName) {
        this.commentText = commentText;
        this.csafNodeId = csafNodeId;
        this.fieldName = fieldName;
    }

    @Schema(
            description = "The text of the comment.",
            example = "This is a comment."
    )
    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    @Schema(
            description = "The ID of the node this comment belongs to.",
            example = "3a49e7c5-ff70-4d6d-bdf8-afc8fdfb1c31"
    )
    public String getCsafNodeId() {
        return csafNodeId;
    }

    public void setCsafNodeId(String csafNodeId) {
        this.csafNodeId = csafNodeId;
    }

    /**
     * indicates if this comment refers to the whole document or a specific node
     *
     * @return true if the nodeId is null, false otherwise
     */
    public boolean isCommentWholeDocument() {
        return this.csafNodeId == null;
    }

    @Schema(
            description = "The field name in case this is a comment for a non-object node.",
            example = "category"
    )
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * indicates if this is a comment on an object node or links to a nested field
     *
     * @return true if fieldName is null, false otherwise
     */
    public boolean isObjectComment() {
        return fieldName == null;
    }

}
