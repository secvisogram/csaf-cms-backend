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

    public AnswerInformationResponse() {
    }

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

    public AnswerInformationResponse setAnswerId(String answerId) {
        this.answerId = answerId;
        return this;
    }

    @Schema(description = "The ID of the comment this is an answer to.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getAnswerTo() {
        return this.answerTo;
    }

    public AnswerInformationResponse setAnswerTo(String answerTo) {
        this.answerTo = answerTo;
        return this;
    }

    @Schema(description = "The current owner of the answer.", example = "Mustermann")
    public String getOwner() {
        return owner;
    }

    public AnswerInformationResponse setOwner(String owner) {
        this.owner = owner;
        return this;
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
