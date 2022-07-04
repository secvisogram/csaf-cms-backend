package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An entry in a list of comment information items.
 */
@Schema(name = "CommentInformation")
public class CommentInformationResponse {

    private String commentId;
    private String advisoryId;
    private String csafNodeId;
    private String owner;
    private boolean changeable;
    private boolean deletable;

    public CommentInformationResponse(String commentId, String advisoryId, String csafNodeId, String owner) {
        this.commentId = commentId;
        this.advisoryId = advisoryId;
        this.csafNodeId = csafNodeId;
        this.owner = owner;
        this.changeable = true;
        this.deletable = true;
    }

    @Schema(description = "The unique ID of the comment.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    @Schema(description = "The ID of the advisory this is a comment to.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getAdvisoryId() {
        return this.advisoryId;
    }

    public void setAdvisoryId(String advisoryId) {
        this.advisoryId = advisoryId;
    }

    @Schema(description = "The ID of the node this comment refers to.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getCsafNodeId() {
        return this.csafNodeId;
    }

    public void setCsafNodeId(String csafNodeId) {
        this.csafNodeId = csafNodeId;
    }

    @Schema(description = "The current owner of the comment.", example = "Mustermann")
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Schema(description = "Indicates if the currently logged in user can change this comment.", example = "true")
    public boolean isChangeable() {
        return changeable;
    }

    @Schema(description = "Indicates if the logged in user can delete this comment.", example = "false")
    public boolean isDeletable() {
        return deletable;
    }

}
