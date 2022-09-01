package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.ID_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.REVISION_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey.InvalidObjectType;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CommentField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DbField;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateCommentRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AnswerInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.CommentInformationResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
    public static CommentWrapper createFromCouchDb(InputStream commentStream) throws IOException, CsafException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        CommentWrapper wrapperFomDb = new CommentWrapper(jacksonMapper.readValue(commentStream, ObjectNode.class));
        if (wrapperFomDb.getType() != ObjectType.Comment) {
            throw new CsafException("Object for id is not of type Comment", InvalidObjectType, BAD_REQUEST);
        }

        return wrapperFomDb;
    }

    /**
     * Convert a comment String to an initial CommentWrapper for a given user.
     * The wrapper has no id and revision.
     *
     * @param advisoryId     the ID of the advisory to add the comment to
     * @param newComment     the new comment
     * @return the wrapper
     */
    public static CommentWrapper createNew(String advisoryId, CreateCommentRequest newComment) {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        CommentWrapper wrapper =  new CommentWrapper(jacksonMapper.createObjectNode());
        wrapper.setAdvisoryId(advisoryId);
        wrapper.setType(ObjectType.Comment);
        wrapper.setText(newComment.getCommentText());
        wrapper.setCreatedAtToNow();
        if (!newComment.isObjectComment()) {
            wrapper.setFieldName(newComment.getFieldName());
        }
        if (!newComment.isCommentWholeDocument()) {
            checkValidUuid(newComment.getCsafNodeId());
            wrapper.setCsafNodeId(newComment.getCsafNodeId());
        }
        return wrapper;
    }

    private static void checkValidUuid(String uuidString) {
        try {
            UUID.fromString(uuidString);
        } catch (IllegalArgumentException iaEx) {
            throw new IllegalArgumentException("csafNodeId is not a valid UUID!", iaEx);
        }
    }

    /**
     * Convert a comment String to an initial CommentWrapper for a given user.
     * The wrapper has no id and revision.
     *
     * @param commentId     the ID of the comment to add the answer to
     * @param commentText   the new comment text
     * @return the wrapper
     */
    public static CommentWrapper createNewAnswerFromJson(String advisoryId, String commentId, String commentText) {

        final ObjectMapper jacksonMapper = new ObjectMapper();

        if (commentText == null) {
            throw new IllegalArgumentException("commentText must be provided!");
        }
        if (advisoryId == null) {
            throw new IllegalArgumentException("advisoryId must be provided for answers!");
        }

        CommentWrapper wrapper = new CommentWrapper(jacksonMapper.createObjectNode());
        wrapper.setAdvisoryId(advisoryId);
        wrapper.setAnswerTo(commentId);
        wrapper.setText(commentText);
        wrapper.setType(ObjectType.Comment);
        wrapper.setCreatedAtToNow();

        return wrapper;
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

        return getTextFor(CommentField.TEXT);
    }

    public CommentWrapper setText(String newValue) {

        this.commentNode.put(CommentField.TEXT.getDbName(), newValue);
        return this;
    }

    public String getCsafNodeId() {

        return getTextFor(CommentField.CSAF_NODE_ID);
    }

    public CommentWrapper setCsafNodeId(String newValue) {

        this.commentNode.put(CommentField.CSAF_NODE_ID.getDbName(), newValue);
        return this;
    }

    public String getFieldName() {

        return getTextFor(CommentField.FIELD_NAME);
    }

    public CommentWrapper setFieldName(String newValue) {

        this.commentNode.put(CommentField.FIELD_NAME.getDbName(), newValue);
        return this;
    }

    public String getOwner() {

        return getTextFor(CommentField.OWNER);
    }

    public CommentWrapper setOwner(String newValue) {

        this.commentNode.put(CommentField.OWNER.getDbName(), newValue);
        return this;
    }

    public String getRevision() {

        return getTextFor(REVISION_FIELD);
    }

    public CommentWrapper setRevision(String newValue) {

        this.commentNode.put(REVISION_FIELD.getDbName(), newValue);
        return this;
    }

    public String getAdvisoryId() {

        return  getTextFor(CommentField.ADVISORY_ID);
    }

    public CommentWrapper setAdvisoryId(String newValue) {

        this.commentNode.put(CommentField.ADVISORY_ID.getDbName(), newValue);
        return this;
    }


    public String getAnswerTo() {

        return getTextFor(CommentField.ANSWER_TO);
    }

    public CommentWrapper setAnswerTo(String newValue) {

        this.commentNode.put(CommentField.ANSWER_TO.getDbName(), newValue);
        return this;
    }

    ObjectType getType() {

        try {
            String typeText = getTextFor(CouchDbField.TYPE_FIELD);
            return (typeText != null) ? ObjectType.valueOf(typeText) : null;
        } catch (IllegalArgumentException ex) {
            return null; // unknown type
        }
    }

    String getTextFor(DbField dbField) {

        return (this.commentNode.has(dbField.getDbName())) ? this.commentNode.get(dbField.getDbName()).asText() : null;
    }


    private CommentWrapper setType(ObjectType newValue) {

        this.commentNode.put(CouchDbField.TYPE_FIELD.getDbName(), newValue.name());
        return this;
    }

    public String commentAsString() {

        return this.commentNode.toString();
    }

    public CommentWrapper setCreatedAtToNow() {

        this.commentNode.put(AuditTrailField.CREATED_AT.getDbName(), DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        return this;
    }


    public static CommentInformationResponse convertToCommentInfo(JsonNode commentJson) {

        String commentId = commentJson.get(ID_FIELD.getDbName()).asText();
        String advisoryId = commentJson.get(CommentField.ADVISORY_ID.getDbName()).asText();
        String csafNodeId = commentJson.has(CommentField.CSAF_NODE_ID.getDbName()) ? commentJson.get(CommentField.CSAF_NODE_ID.getDbName()).asText() : null;
        String owner = commentJson.has(CommentField.OWNER.getDbName()) ? commentJson.get(CommentField.OWNER.getDbName()).asText() : null;
        String answerTo = commentJson.has(CommentField.ANSWER_TO.getDbName()) ? commentJson.get(CommentField.ANSWER_TO.getDbName()).asText() : null;
        CommentInformationResponse response = new CommentInformationResponse(commentId, advisoryId, csafNodeId, owner);
        response.setAnswerTo(answerTo);
        return response;
    }

    public static AnswerInformationResponse convertToAnswerInfo(JsonNode answerJson) {
        String answerId = answerJson.get(ID_FIELD.getDbName()).asText();
        String answerTo = answerJson.get(CommentField.ANSWER_TO.getDbName()).asText();
        String owner = answerJson.has(CommentField.OWNER.getDbName()) ? answerJson.get(CommentField.OWNER.getDbName()).asText() : null;
        return new AnswerInformationResponse(answerId, answerTo, owner);
    }

}
