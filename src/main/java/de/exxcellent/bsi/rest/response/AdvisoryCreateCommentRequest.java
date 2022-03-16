package de.exxcellent.bsi.rest.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@ApiModel("AdvisoryCreateComment")
public class AdvisoryCreateCommentRequest {

   @Schema(description = "A comment is added to an object in the CSAF document. This name specifies the " +
           "concrete value in the object the comment belongs to. When its empty, the comment belongs to the whole object.")
   private final String fieldName;
   @Schema(description = "The text of the comment")
   private final String commentText;

   public AdvisoryCreateCommentRequest(String fieldName, String commentText) {
      this.fieldName = fieldName;
      this.commentText = commentText;
   }

   @ApiModelProperty(value = "A comment is added to an object in the CSAF document. This name specifies the " +
           "concrete value in the object the comment belongs to. When its empty, the comment belongs to the whole object.", example = "csaf_version")
   public String getFieldName() {
      return fieldName;
   }

   @ApiModelProperty(value = "The text of the comment", example = "Is this value correct?")
   public String getCommentText() {
      return commentText;
   }
}
