package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.ID_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.REVISION_FIELD;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CommentField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.Comment;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.CommentInformationResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Wrapper around JsonNode to read and write comment/answer objects from/to the CouchDB
 */
public class CommentWrapper {


    /**
     * Convert an input stream from the couch db to a CommentWrapper
     *
     * @param commentStream the stream
     * @return the wrapper
     * @throws IOException error in processing the input stream
     */
    public static CommentWrapper createFromCouchDb(InputStream commentStream) throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        return new CommentWrapper(jacksonMapper.readValue(commentStream, ObjectNode.class));
    }

    /**
     * Convert a comment String to an initial CommentWrapper for a given user.
     * The wrapper has no id and revision.
     *
     * @param advisoryId     the ID of the advisory to add the comment to
     * @param newComment     the new comment
     * @return the wrapper
     */
    public static CommentWrapper createNew(String advisoryId, Comment newComment) {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        ObjectNode commentRootNode = jacksonMapper.createObjectNode();

        commentRootNode.put(CommentField.TEXT.getDbName(), newComment.getCommentText());
        if (!newComment.isCommentWholeDocument()) {
            String nodeId = newComment.getCsafNodeId();
            try {
                UUID.fromString(nodeId);
            } catch (IllegalArgumentException iaEx) {
                throw new IllegalArgumentException("csafNodeId is not a valid UUID!", iaEx);
            }
            commentRootNode.put(CommentField.CSAF_NODE_ID.getDbName(), newComment.getCsafNodeId());
        }
        if (!newComment.isObjectComment()) {
            commentRootNode.put(CommentField.FIELD_NAME.getDbName(), newComment.getFieldName());
        }
        commentRootNode.put(CommentField.ADVISORY_ID.getDbName(), advisoryId);
        commentRootNode.put(CouchDbField.TYPE_FIELD.getDbName(), ObjectType.Comment.name());

        return new CommentWrapper(commentRootNode);
    }

    private final ObjectNode commentNode;

    private CommentWrapper(ObjectNode commentNode) {

        this.commentNode = commentNode;
    }

    private ObjectNode getCommentNode() {

        return this.commentNode;
    }

    public String getCommentId() {

        return (commentNode.has(ID_FIELD.getDbName())) ? commentNode.get(ID_FIELD.getDbName()).asText() : null;
    }

    private CommentWrapper setCommentId(String newValue) {

        this.commentNode.put(ID_FIELD.getDbName(), newValue);
        return this;
    }

    public String getText() {

        return this.commentNode.get(CommentField.TEXT.getDbName()).asText();
    }

    public CommentWrapper setText(String newValue) {

        this.commentNode.put(CommentField.TEXT.getDbName(), newValue);
        return this;
    }

    public String getCsafNodeId() {

        return commentNode.has(CommentField.CSAF_NODE_ID.getDbName()) ? commentNode.get(CommentField.CSAF_NODE_ID.getDbName()).asText() : null;
    }

    public CommentWrapper setCsafNodeId(String newValue) {

        this.commentNode.put(CommentField.CSAF_NODE_ID.getDbName(), newValue);
        return this;
    }

    public String getFieldName() {

        return commentNode.has(CommentField.FIELD_NAME.getDbName()) ? commentNode.get(CommentField.FIELD_NAME.getDbName()).asText() : null;
    }

    public CommentWrapper setFieldName(String newValue) {

        this.commentNode.put(CommentField.FIELD_NAME.getDbName(), newValue);
        return this;
    }

    public String getOwner() {

        return commentNode.has(CommentField.OWNER.getDbName()) ? commentNode.get(CommentField.OWNER.getDbName()).asText() : null;
    }

    public CommentWrapper setOwner(String newValue) {

        this.commentNode.put(CommentField.OWNER.getDbName(), newValue);
        return this;
    }

    public String getRevision() {

        return commentNode.has(REVISION_FIELD.getDbName()) ? commentNode.get(REVISION_FIELD.getDbName()).asText() : null;
    }

    public CommentWrapper setRevision(String newValue) {

        this.commentNode.put(REVISION_FIELD.getDbName(), newValue);
        return this;
    }

    public String getAdvisoryId() {

        return commentNode.has(CommentField.ADVISORY_ID.getDbName()) ? commentNode.get(CommentField.ADVISORY_ID.getDbName()).asText() : null;
    }

    public CommentWrapper setAdvisoryId(String newValue) {

        this.commentNode.put(CommentField.ADVISORY_ID.getDbName(), newValue);
        return this;
    }

    public String commentAsString() {

        return this.commentNode.toString();
    }

    public static CommentInformationResponse convertToCommentInfo(JsonNode commentJson) {

        String commentId = commentJson.get(ID_FIELD.getDbName()).asText();
        String advisoryId = commentJson.get(CommentField.ADVISORY_ID.getDbName()).asText();
        String csafNodeId = commentJson.has(CommentField.CSAF_NODE_ID.getDbName()) ? commentJson.get(CommentField.CSAF_NODE_ID.getDbName()).asText() : null;
        String owner = commentJson.has(CommentField.OWNER.getDbName()) ? commentJson.get(CommentField.OWNER.getDbName()).asText() : null;
        return new CommentInformationResponse(commentId, advisoryId, csafNodeId, owner);
    }

}
