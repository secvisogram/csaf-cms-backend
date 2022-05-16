package de.bsi.secvisogram.csaf_cms_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.cloud.cloudant.v1.model.Document;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbService;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.IdNotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdvisoryService {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryService.class);

    @Autowired
    private CouchDbService couchDbService;

    private static final String WORKFLOW_STATE_FIELD = "workflowState";
    private static final String OWNER_FIELD = "owner";
    private static final String TYPE_FIELD = "type";
    private static final String CSAF_FIELD = "csaf";
    private static final Set<String> REQUIRED_FIELDS = Set.of(
            WORKFLOW_STATE_FIELD, OWNER_FIELD, TYPE_FIELD, CSAF_FIELD
    );
    private static final String[] DOCUMENT_TITLE = {"csaf", "document", "title"};
    private static final String[] DOCUMENT_TRACKING_ID = {"csaf", "document", "tracking", "id"};

    private static final ObjectMapper jacksonMapper = new ObjectMapper();

    private ObjectNode stringToAdvisory(String jsonString) throws JsonProcessingException {
        ObjectNode jsonObject = jacksonMapper.readValue(jsonString, ObjectNode.class);
        if (!basicValidate(jsonObject)) {
            throw new IllegalArgumentException("The advisory did not pass basic validation!");
        }
        return jsonObject;
    }

    private boolean basicValidate(ObjectNode advisoryJsonObject) {
        Set<String> fields = new HashSet<>();
        advisoryJsonObject.fieldNames().forEachRemaining(fields::add);
        Set<String> missingFields = new HashSet<>(REQUIRED_FIELDS);
        missingFields.removeAll(fields);
        if (!missingFields.isEmpty()) {
            LOG.error("The advisory json does not contain the required fields: {} (got {})", missingFields, fields);
            return false;
        }
        return true;
    }


    /**
     * get number of advisories
     *
     * @return number of advisories in the DB
     */
    public Long getAdvisoryCount() {
        return couchDbService.getDocumentCount();
    }

    /**
     * get information on all advisories
     *
     * @return a list of information objects
     */
    public List<AdvisoryInformationResponse> getAdvisoryIds() {

        List<String> infoFields = List.of(
                WORKFLOW_STATE_FIELD,
                OWNER_FIELD,
                TYPE_FIELD,
                String.join(".", DOCUMENT_TITLE),
                String.join(".", DOCUMENT_TRACKING_ID),
                CouchDbService.REVISION_FIELD,
                CouchDbService.ID_FIELD
        );

        List<Document> docList = couchDbService.readAllCsafDocuments(infoFields);
        return docList.stream().map(this::convertToAdvisoryInfo).toList();
    }

    /**
     * Adds an advisory to the system
     *
     * @param advisoryJsonString the advisory as JSON String
     * @return a tuple of assigned id as UUID and the current revision for concurrent control
     * @throws JsonProcessingException  if the given JSON string is not valid
     * @throws IllegalArgumentException if the given JSON does not satisfy basic requirements
     */
    public IdAndRevision addAdvisory(String advisoryJsonString) throws JsonProcessingException, IllegalArgumentException {

        UUID advisoryId = UUID.randomUUID();
        ObjectNode objectNode = stringToAdvisory(advisoryJsonString);
        String revision = couchDbService.writeCsafDocument(advisoryId, objectNode);
        return new IdAndRevision(advisoryId, revision);
    }

    /**
     * get a specific advisory
     *
     * @param advisoryId the ID of the advisory to get
     * @return the requested advisory
     * @throws IdNotFoundException if there is no advisory with given ID
     */
    public AdvisoryResponse getAdvisory(UUID advisoryId) throws IdNotFoundException {
        Document dbDoc = couchDbService.readCsafDocument(advisoryId.toString());
        return convertToAdvisory(dbDoc);
    }

    /**
     * @param advisoryId the ID of the advisory to delete
     * @param revision   the revision for concurrent control
     * @throws BadRequestException if the request was
     * @throws NotFoundException   if there is no advisory with given ID
     */
    public void deleteAdvisory(UUID advisoryId, String revision) throws DatabaseException {
        couchDbService.deleteCsafDocument(advisoryId.toString(), revision);
    }

    /**
     * @param advisoryId                the ID of the advisory to update
     * @param revision                  the revision for concurrent control
     * @param updatedAdvisoryJsonString the updated advisory json as string
     * @return the new revision of the updated csaf document
     * @throws JsonProcessingException if the given JSON string is not valid
     * @throws DatabaseException       if there was an error updating the advisory in the DB
     */
    public String updateAdvisory(UUID advisoryId, String revision, String updatedAdvisoryJsonString) throws JsonProcessingException, DatabaseException {

        ObjectNode objectNode = stringToAdvisory(updatedAdvisoryJsonString);
        return couchDbService.updateCsafDocument(advisoryId.toString(), revision, objectNode);
    }

    /**
     * @param advisoryId       the ID of the advisory to update the workflow state of
     * @param revision         the revision for concurrent control
     * @param newWorkflowState the new workflow state to set
     * @return the new revision of the updated csaf document
     * @throws DatabaseException if there was an error updating the advisory in the DB
     */
    public String changeAdvisoryWorkflowState(UUID advisoryId, String revision, WorkflowState newWorkflowState) throws IOException, DatabaseException {

        AdvisoryResponse advisory = getAdvisory(advisoryId);
        advisory.setWorkflowState(newWorkflowState);
        return updateAdvisory(advisoryId, revision, convertToString(advisory));

    }


    private AdvisoryInformationResponse convertToAdvisoryInfo(Document doc) {
        String advisoryId = doc.getId();
        WorkflowState wkfState = WorkflowState.valueOf(CouchDbService.getStringFieldValue(WORKFLOW_STATE_FIELD, doc));
        String trackingId = CouchDbService.getStringFieldValue(DOCUMENT_TRACKING_ID, doc);
        String title = CouchDbService.getStringFieldValue(DOCUMENT_TITLE, doc);
        String owner = CouchDbService.getStringFieldValue(OWNER_FIELD, doc);
        return new AdvisoryInformationResponse(advisoryId, wkfState, trackingId, title, owner);
    }

    private AdvisoryResponse convertToAdvisory(Document doc) {
        String advisoryId = doc.getId();
        WorkflowState wkfState = WorkflowState.valueOf(CouchDbService.getStringFieldValue(WORKFLOW_STATE_FIELD, doc));
        String csafString = jacksonMapper.convertValue(doc.get(CSAF_FIELD), JsonNode.class).toPrettyString();
        AdvisoryResponse advisory = new AdvisoryResponse(advisoryId, wkfState, csafString);
        advisory.setRevision(doc.getRev());
        return advisory;
    }

    private String convertToString(AdvisoryResponse advisory) throws JsonProcessingException {
        JsonNode csafRootNode = jacksonMapper.readValue(advisory.getCsaf(), JsonNode.class);

        ObjectNode rootNode = jacksonMapper.createObjectNode();
        rootNode.put(WORKFLOW_STATE_FIELD, advisory.getWorkflowState().name());
        rootNode.put(OWNER_FIELD, advisory.getOwner());
        rootNode.put(TYPE_FIELD, CouchDbService.ObjectType.Advisory.name());
        rootNode.set(CSAF_FIELD, csafRootNode);

        return rootNode.toPrettyString();
    }

}
