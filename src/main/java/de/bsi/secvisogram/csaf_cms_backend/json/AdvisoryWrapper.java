package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryField.*;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.ID_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.REVISION_FIELD;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import com.ibm.cloud.cloudant.v1.model.Document;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.*;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.Expression;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Wrapper around JsonNode to read and write advisory objects from/to the CouchDB
 */
public class AdvisoryWrapper {

    public static final String emptyCsafDocument = """
                { "document": {
                   }
                }""";
    private static final String COMMENT_KEY = "$comment";


    /**
     * Convert an input stream from the couch db to an AdvisoryWrapper
     *
     * @param advisoryStream the stream
     * @return the wrapper
     * @throws IOException error in processing the input stream
     */
    public static AdvisoryWrapper createFromCouchDb(InputStream advisoryStream) throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        return new AdvisoryWrapper(jacksonMapper.readValue(advisoryStream, ObjectNode.class));
    }

    private static ObjectNode createAdvisoryNodeFromString(String csafJson) throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        final InputStream csafStream = new ByteArrayInputStream(csafJson.getBytes(StandardCharsets.UTF_8));
        JsonNode csafRootNode = jacksonMapper.readValue(csafStream, JsonNode.class);
        if (csafRootNode.get("document") == null) {
            throw new IllegalArgumentException("Csaf contains no document entry");
        }

        ObjectNode rootNode = jacksonMapper.createObjectNode();
        rootNode.set(CSAF.getDbName(), csafRootNode);
        return rootNode;
    }

    /**
     * Create an initial empty AdvisoryWrapper for the given user
     *
     * @param userName the user
     * @return the wrapper
     * @throws IOException exception in handling json string
     */
    public static AdvisoryWrapper createInitialEmptyAdvisoryForUser(String userName) throws IOException {

        return createNewFromCsaf(emptyCsafDocument, userName);
    }

    /**
     * Convert an CSAF document to an initial AdvisoryWrapper for a given user.
     * The wrapper has no id and revision.
     *
     * @param newCsafJson the csaf string
     * @param userName    the user
     * @return the wrapper
     * @throws IOException exception in handling json string
     */
    public static AdvisoryWrapper createNewFromCsaf(String newCsafJson, String userName) throws IOException {

        ObjectNode rootNode = createAdvisoryNodeFromString(newCsafJson);
        rootNode.put(WORKFLOW_STATE.getDbName(), WorkflowState.Draft.name());
        rootNode.put(OWNER.getDbName(), userName);
        rootNode.put(CouchDbField.TYPE_FIELD.getDbName(), ObjectType.Advisory.name());

        return new AdvisoryWrapper(rootNode);
    }

    /**
     * Creates a new AdvisoryWrapper based on the given one and set its CSAF document to the changed CSAF document
     *
     * @param existing        the base AdvisoryWrapper
     * @param changedCsafJson the new CSAF document
     * @return the new AdvisoryWrapper
     * @throws IOException exception in handling json
     */
    public static AdvisoryWrapper updateFromExisting(AdvisoryWrapper existing, String changedCsafJson) throws IOException {

        ObjectNode rootNode = createAdvisoryNodeFromString(changedCsafJson);
        return new AdvisoryWrapper(rootNode)
                .setAdvisoryId(existing.getAdvisoryId())
                .setOwner(existing.getOwner())
                .setWorkflowState(existing.getWorkflowState())
                .setType(ObjectType.Advisory);
    }

    private final ObjectNode advisoryNode;

    private AdvisoryWrapper(ObjectNode advisoryNode) {

        this.advisoryNode = advisoryNode;
    }

    private JsonNode getAdvisoryNode() {
        return advisoryNode;
    }

    public String getWorkflowStateString() {

        return this.advisoryNode.get(AdvisoryField.WORKFLOW_STATE.getDbName()).asText();
    }

    public String getOwner() {

        return this.advisoryNode.get(AdvisoryField.OWNER.getDbName()).asText();
    }

    public AdvisoryWrapper setOwner(String newValue) {

        this.advisoryNode.put(AdvisoryField.OWNER.getDbName(), newValue);
        return this;
    }

    private AdvisoryWrapper setType(ObjectType newValue) {

        this.advisoryNode.put(CouchDbField.TYPE_FIELD.getDbName(), newValue.name());
        return this;
    }

    public WorkflowState getWorkflowState() {

        return WorkflowState.valueOf(this.advisoryNode.get(AdvisoryField.WORKFLOW_STATE.getDbName()).asText());
    }


    public AdvisoryWrapper setWorkflowState(WorkflowState newState) {

        this.advisoryNode.put(AdvisoryField.WORKFLOW_STATE.getDbName(), newState.name());
        return this;
    }


    public String getRevision() {

        return (advisoryNode.has(REVISION_FIELD.getDbName())) ? advisoryNode.get(REVISION_FIELD.getDbName()).asText() : null;
    }

    public AdvisoryWrapper setRevision(String newValue) {

        this.advisoryNode.put(REVISION_FIELD.getDbName(), newValue);
        return this;
    }

    public String getAdvisoryId() {

        return (advisoryNode.has(ID_FIELD.getDbName())) ? advisoryNode.get(ID_FIELD.getDbName()).asText() : null;
    }

    private AdvisoryWrapper setAdvisoryId(String newValue) {

        this.advisoryNode.put(ID_FIELD.getDbName(), newValue);
        return this;
    }


    public JsonNode getCsaf() {

        return this.advisoryNode.get(AdvisoryField.CSAF.getDbName());
    }

    private AdvisoryWrapper setCsaf(JsonNode node) {

        this.advisoryNode.putIfAbsent(CSAF.getDbName(), node);
        return this;
    }

    /**
     * Adds a comment to a specific field of the CSAF document
     *
     * @param objNode   the object node to add the comment to
     * @param commentId the comment ID to add
     * @return this advisory wrapper with the updated advisory
     */
    public AdvisoryWrapper addCommentId(ObjectNode objNode, String commentId) {

        if (objNode.has(COMMENT_KEY)) {
            ArrayNode commentNode = (ArrayNode) objNode.get(COMMENT_KEY);
            commentNode.add(commentId);
        } else {
            final ObjectMapper jacksonMapper = new ObjectMapper();
            ArrayNode commentNode = jacksonMapper.createArrayNode();
            commentNode.add(commentId);
            objNode.set(COMMENT_KEY, commentNode);
        }
        return this;
    }

    /**
     * Removes a comment from a specific field of the CSAF document
     * As comments are assumed to be unique, the comment's id is searched in the whole tree and the first occurrence
     * is removed. It will be silently ignored if the comment id is not found.
     *
     * @param commentId the comment ID to remove
     * @return this advisory wrapper with the updated advisory
     */
    public AdvisoryWrapper removeCommentId(String commentId) {
        ArrayNode commentNode = findCommentNode(getCsaf());
        if (commentNode != null) {
            for (Iterator<JsonNode> it = commentNode.iterator(); it.hasNext(); ) {
                JsonNode item = it.next();
                if (item.asText().equals(commentId)) {
                    it.remove();
                }
            }
        }
        return this;
    }

    private ArrayNode findCommentNode(JsonNode j) {
        Iterator<String> keyIter = j.fieldNames();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            System.out.println(key);
            if (j.get(key) instanceof ObjectNode objNode) {
                if (objNode.has(COMMENT_KEY)) {
                    return (ArrayNode) objNode.get(COMMENT_KEY);
                }
                return findCommentNode(objNode);
            } else if (j.get(key) instanceof ArrayNode) {
                for (JsonNode arrNode : j.get(key)) {
                    return findCommentNode(arrNode);
                }
            }
        }
        return null;
    }

    public JsonNode at(String jsonPtrExpr) {
        return this.advisoryNode.at(jsonPtrExpr);
    }

    /**
     * Get the node in the wrapped advisory node specified by given dbField.
     *
     * @param dbField dbField converted to JSON pointer instance
     * @return Node that matches given JSON Pointer: if no match exists, will return a node for which TreeNode.isMissingNode() returns true.
     */
    public JsonNode at(DbField dbField) {

        String jsonPtrExpr = String.join("/", dbField.getFieldPath());
        return this.advisoryNode.at("/" + jsonPtrExpr);
    }

    public String getDocumentTrackingVersion() {

        JsonNode versionNode = this.at(AdvisorySearchField.DOCUMENT_TRACKING_VERSION);
        return (versionNode.isMissingNode()) ? "" : versionNode.asText();
    }


    public String advisoryAsString() {

        return this.advisoryNode.toString();
    }

    public static AdvisoryInformationResponse convertToAdvisoryInfo(Document doc, Map<DbField,
            BiConsumer<AdvisoryInformationResponse, String>> infoFields) {
        String advisoryId = doc.getId();
        final AdvisoryInformationResponse response = new AdvisoryInformationResponse(advisoryId, null);
        infoFields.forEach((key, value) -> setValueInResponse(response, key, doc, value));

        return response;
    }

    private static void setValueInResponse(AdvisoryInformationResponse response, DbField field, Document doc, BiConsumer<AdvisoryInformationResponse, String> advisorySetter) {

        String value;
        if (field.equals(ID_FIELD)) {
            value = doc.getId();
        } else if (field.equals(REVISION_FIELD)) {
            value = doc.getRev();
        } else {
            value = CouchDbService.getStringFieldValue(field, doc);
        }
        advisorySetter.accept(response, value);
    }

    /**
     * Calculate the JavaScript Object Notation (JSON) Patch according to RFC 6902.
     * Computes and returns a JSON patch from source to target
     * Further, if resultant patch is applied to source, it will yield target
     *
     * @param target either valid JSON objects or arrays or values
     * @return the resultant patch
     */
    public JsonNode calculateDiffTo(AdvisoryWrapper target) {

        return calculateJsonDiff(this.getAdvisoryNode(), target.getAdvisoryNode());
    }

    public AdvisoryWrapper applyJsonPatch(JsonNode patch) {

        ObjectNode patched = (ObjectNode) applyJsonPatchToNode(patch, this.getAdvisoryNode());
        return new AdvisoryWrapper(patched);
    }


    /**
     * Calculate the JavaScript Object Notation (JSON) Patch according to RFC 6902.
     * Computes and returns a JSON patch from source to target
     * Further, if resultant patch is applied to source, it will yield target
     *
     * @param source either valid JSON objects or arrays or values
     * @param target either valid JSON objects or arrays or values
     * @return the resultant patch
     */
    public static JsonNode calculateJsonDiff(JsonNode source, JsonNode target) {


        return JsonDiff.asJson(source, target);
    }

    /**
     * Apply patch to JsonNode
     *
     * @param patch  the patch to apply
     * @param source the JsonNode the patch is applied to
     * @return the patched JsonNode
     */
    public static JsonNode applyJsonPatchToNode(JsonNode patch, JsonNode source) {

        return JsonPatch.apply(patch, source);
    }

    /**
     * Convert Search Expression to JSON String
     *
     * @param expression2Convert the expression to convert
     * @return the converted expression
     * @throws JsonProcessingException a conversion problem has occurred
     */
    public static String expression2Json(Expression expression2Convert) throws JsonProcessingException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        ObjectWriter writer = jacksonMapper.writer(new DefaultPrettyPrinter());

        return writer.writeValueAsString(expression2Convert);
    }

    /**
     * Convert JSON String to Search expression
     *
     * @param jsonString the String to convert
     * @return the converted expression
     * @throws JsonProcessingException error in json
     */
    public static Expression json2Expression(String jsonString) throws JsonProcessingException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        return jacksonMapper.readValue(jsonString, Expression.class);

    }
}
