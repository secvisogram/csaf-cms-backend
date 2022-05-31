package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryField.*;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.ID_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.REVISION_FIELD;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import com.ibm.cloud.cloudant.v1.model.Document;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbService;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DbField;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper to JsonNode to read and write advisory objects from/to the CouchDB
 */
public class AdvisoryWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryWrapper.class);

    private static final Set<DbField> REQUIRED_FIELDS = Set.of(
            AdvisoryField.WORKFLOW_STATE, AdvisoryField.OWNER, CouchDbField.TYPE_FIELD, CSAF
    );

    /**
     * Convert an input stream from th couch db to an AdvisoryWrapper
     * @param advisoryStream the stream
     * @return the wrapper
     * @throws IOException error in processing the input stream
     */
    public static AdvisoryWrapper createFromCouchDb(InputStream advisoryStream) throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        return new AdvisoryWrapper(jacksonMapper.readValue(advisoryStream, ObjectNode.class));
    }

    /**
     * Convert an CSAF document to an initial AdvisoryWrapper for given user.
     * The wrapper has no id and revision.
     * @param newCsafJson the csaf string
     * @param userName the user
     * @return the wrapper
     * @throws IOException exception in handling json string
     */
    public static AdvisoryWrapper createNewFromCsaf(String newCsafJson, String userName) throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        final InputStream csafStream = new ByteArrayInputStream(newCsafJson.getBytes(StandardCharsets.UTF_8));
        JsonNode csafRootNode = jacksonMapper.readValue(csafStream, JsonNode.class);

        if (csafRootNode.get("document") == null) {
            throw new IllegalArgumentException("Csaf contains no document entry");
        }
        ObjectNode rootNode = jacksonMapper.createObjectNode();
        rootNode.put(WORKFLOW_STATE.getDbName(), WorkflowState.Draft.name());
        rootNode.put(OWNER.getDbName(), userName);
        rootNode.put(CouchDbField.TYPE_FIELD.getDbName(), ObjectType.Advisory.name());
        rootNode.set(CSAF.getDbName(), csafRootNode);

        return new AdvisoryWrapper(rootNode);
    }

    public static AdvisoryWrapper updateFromExisting(AdvisoryWrapper existing, String changedCsafJson) throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        final InputStream csafStream = new ByteArrayInputStream(changedCsafJson.getBytes(StandardCharsets.UTF_8));
        JsonNode csafRootNode = jacksonMapper.readValue(csafStream, JsonNode.class);

        ObjectNode newRootNode = jacksonMapper.createObjectNode();
        return new AdvisoryWrapper(newRootNode)
                .setAdvisoryId(existing.getAdvisoryId())
                .setOwner(existing.getOwner())
                .setWorkflowState(existing.getWorkflowState())
                .setType(ObjectType.Advisory)
                .setCsaf(csafRootNode);
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

    public JsonNode at(String jsonPtrExpr) {
        return this.advisoryNode.at(jsonPtrExpr);
    }

    public String advisoryAsString() {

        return this.advisoryNode.toString();
    }

    public boolean basicValidate(ObjectNode advisoryJsonObject) {
        Set<String> fields = new HashSet<>();
        advisoryJsonObject.fieldNames().forEachRemaining(fields::add);
        Set<String> missingFields = new HashSet<>(REQUIRED_FIELDS.stream().map(DbField::getDbName).collect(Collectors.toList()));
        missingFields.removeAll(fields);
        if (!missingFields.isEmpty()) {
            LOG.error("The advisory json does not contain the required fields: {} (got {})", missingFields, fields);
            return false;
        }
        return true;
    }

    public static AdvisoryInformationResponse convertToAdvisoryInfo(Document doc, Map<DbField,
            BiConsumer<AdvisoryInformationResponse, String>> infoFields) {
        String advisoryId = doc.getId();
        final AdvisoryInformationResponse response = new AdvisoryInformationResponse(advisoryId, null);
        infoFields.entrySet()
                .forEach(entry -> setValueInResponse(response, entry.getKey(), doc, entry.getValue()));

        return response;
    }

    public static void setValueInResponse(AdvisoryInformationResponse response, DbField field, Document doc, BiConsumer<AdvisoryInformationResponse, String> advisorySetter) {

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
     * @param target either valid JSON objects or arrays or values
     * @return the resultant patch
     */
    public JsonNode calculateJsonDiff(AdvisoryWrapper target) {


        return JsonDiff.asJson(this.getAdvisoryNode(), target.getAdvisoryNode());
    }

    /**
     * Apply path o JsonNode
     * @param patch the patch to apply
     * @param source the JsonNode the pacht is applied o
     * @return the patched JsonNode
     */
    public JsonNode applyJsonPatch(JsonNode patch, JsonNode source) {

        return JsonPatch.apply(patch, source);
    }

}
