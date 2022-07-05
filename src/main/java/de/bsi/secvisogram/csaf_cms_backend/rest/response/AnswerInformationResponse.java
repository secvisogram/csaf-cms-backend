package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An entry in a list of comment information items.
 */
@Schema(name = "CommentInformation")
public class AnswerInformationResponse {

    private String answerId;
    private String owner;
    private String answerTo;
    private boolean changeable;
    private boolean deletable;

    public AnswerInformationResponse(String answerId, String answerTo, String owner) {
        this.answerId = answerId;
        this.answerTo = answerTo;
        this.owner = owner;
        this.changeable = true;
        this.deletable = true;
    }

    @Schema(description = "The unique ID of the answer.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getAnswerId() {
        return answerId;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
    }

    @Schema(description = "The ID of the comment this is an answer to.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getAnswerTo() {
        return this.answerTo;
    }

    public void setAnswerTo(String answerTo) {
        this.answerTo = answerTo;
    }

    @Schema(description = "The current owner of the answer.", example = "Mustermann")
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Schema(description = "Indicates if the currently logged in user can change this answer.", example = "true")
    public boolean isChangeable() {
        return changeable;
    }

    @Schema(description = "Indicates if the logged in user can delete this answer.", example = "false")
    public boolean isDeletable() {
        return deletable;
    }

}
