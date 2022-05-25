package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryField.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.ibm.cloud.cloudant.v1.model.Document;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbService;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DbField;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

public class AdvisoryWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryWrapper.class);

    private static final Set<DbField> REQUIRED_FIELDS = Set.of(
            AdvisoryField.WORKFLOW_STATE, AdvisoryField.OWNER, CouchDbField.TYPE_FIELD, CSAF
    );

    public static AdvisoryWrapper createFromCouchDb(InputStream advisoryStream) throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        return new AdvisoryWrapper(jacksonMapper.readValue(advisoryStream, ObjectNode.class));
    }

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

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "1. JsonNode is readonly 2. advisoryNode could be very huge")
    public JsonNode getAdvisoryNode() {
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

        return advisoryNode.get(CouchDbField.REVISION_FIELD.getDbName()).asText();
    }

    public AdvisoryWrapper setRevision(String newValue) {

        this.advisoryNode.put(CouchDbField.REVISION_FIELD.getDbName(), newValue);
        return this;
    }

    public String getAdvisoryId() {

        return advisoryNode.get(CouchDbField.ID_FIELD.getDbName()).asText();
    }

    private AdvisoryWrapper setAdvisoryId(String newValue) {

        this.advisoryNode.put(CouchDbField.ID_FIELD.getDbName(), newValue);
        return this;
    }


    public JsonNode getCsaf() {

        return this.advisoryNode.get(AdvisoryField.CSAF.getDbName());
    }

    private AdvisoryWrapper setCsaf(JsonNode node) {

        this.advisoryNode.putIfAbsent(CSAF.getDbName(), node);
        return this;
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
        if (field.equals(CouchDbField.ID_FIELD)) {
            value = doc.getId();
        } else if (field.equals(CouchDbField.REVISION_FIELD)) {
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
     * @param source either valid JSON objects or arrays or values
     * @param target either valid JSON objects or arrays or values
     * @return the resultant patch
     */
    public static JsonNode calculateJsonDiff(JsonNode source, JsonNode target) {


        return JsonDiff.asJson(source, target);
    }

}
