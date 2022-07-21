package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CommentAuditTrailField;
import de.bsi.secvisogram.csaf_cms_backend.model.ChangeType;

public class CommentAuditTrailWrapper extends AuditTrailWrapper {

    public CommentAuditTrailWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }


    public String getCommentId() {

        return this.getAuditTrailNode().get(CommentAuditTrailField.COMMENT_ID.getDbName()).asText();
    }

    public CommentAuditTrailWrapper setCommentId(String newValue) {

        this.getAuditTrailNode().put(CommentAuditTrailField.COMMENT_ID.getDbName(), newValue);
        return this;
    }

    public String getCommentText() {

        return this.getAuditTrailNode().get(CommentAuditTrailField.COMMENT_TEXT.getDbName()).asText();
    }

    public CommentAuditTrailWrapper setCommentText(String newValue) {

        this.getAuditTrailNode().put(CommentAuditTrailField.COMMENT_TEXT.getDbName(), newValue);
        return this;
    }

    /**
     * Create a new comment audit trail for the given comment
     *
     * @param comment the comment
     * @return the new wrapper
     */
    public static CommentAuditTrailWrapper createNew(CommentWrapper comment) {

        ObjectNode rootNode = new ObjectMapper().createObjectNode();

        CommentAuditTrailWrapper wrapper = new CommentAuditTrailWrapper(rootNode)
                .setCommentId(comment.getCommentId())
                .setCommentText(comment.getText());
        wrapper.setType(ObjectType.CommentAuditTrail)
                .setChangeType(ChangeType.Create)
                .setCreatedAtToNow();
        return wrapper;
    }

}
