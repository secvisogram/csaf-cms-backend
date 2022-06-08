package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField.ADVISORY_ID;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.TYPE_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.containsIgnoreCase;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.equal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ibm.cloud.cloudant.v1.model.Document;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.*;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.AuditTrailDocumentWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.AuditTrailWorkflowWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.AuditTrailWrapper;
import de.bsi.secvisogram.csaf_cms_backend.model.ChangeType;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.AndExpression;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdvisoryService {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryService.class);
    static final String emptyCsafDocument = """
                { "document": {
                   }
                }""";

    @Autowired
    private CouchDbService couchDbService;

    /**
     * get number of advisories
     *
     * @return number of advisories in the DB
     */
    public Long getDocumentCount() {
        return couchDbService.getDocumentCount();
    }

    /**
     * get information on all advisories
     *
     * @return a list of information objects
     */
    public List<AdvisoryInformationResponse> getAdvisoryInformations() {

        Map<DbField, BiConsumer<AdvisoryInformationResponse, String>> infoFields = Map.of(
                AdvisoryField.WORKFLOW_STATE, AdvisoryInformationResponse::setWorkflowState,
                AdvisoryField.OWNER, AdvisoryInformationResponse::setOwner,
                AdvisorySearchField.DOCUMENT_TITLE, AdvisoryInformationResponse::setTitle,
                AdvisorySearchField.DOCUMENT_TRACKING_ID, AdvisoryInformationResponse::setDocumentTrackingId,
                CouchDbField.ID_FIELD, AdvisoryInformationResponse::setAdvisoryId
        );

        List<Document> docList = couchDbService.readAllCsafDocuments(new ArrayList<>(infoFields.keySet()));
        return docList.stream()
                .map(couchDbDoc -> AdvisoryWrapper.convertToAdvisoryInfo(couchDbDoc, infoFields))
                .toList();
    }

    /**
     * read from {@link CouchDbService#findDocumentsAsStream(Map, Collection)} and convert it to a list of JsonNode
     *
     * @param selector the selector to search for
     * @param fields   the fields of information to select
     * @return the result nodes of the search
     */
    List<JsonNode> findDocuments(Map<String, Object> selector, Collection<DbField> fields) throws IOException {

        InputStream inputStream = couchDbService.findDocumentsAsStream(selector, fields);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode couchDbResultNode = mapper.readValue(inputStream, JsonNode.class);
        ArrayNode couchDbDocs = (ArrayNode) couchDbResultNode.get("docs");
        List<JsonNode> docNodes = new ArrayList<>();
        couchDbDocs.forEach(docNodes::add);
        return docNodes;
    }

    /**
     * Adds an advisory to the system
     *
     * @param newCsafJson the advisory as JSON String
     * @return a tuple of assigned id as UUID and the current revision for concurrent control
     * @throws JsonProcessingException  if the given JSON string is not valid
     */
    public IdAndRevision addAdvisory(String newCsafJson) throws IOException {

        UUID advisoryId = UUID.randomUUID();
        AdvisoryWrapper emptyAdvisory = AdvisoryWrapper.createNewFromCsaf(emptyCsafDocument, "");
        AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.createNewFromCsaf(newCsafJson, "Mustermann");
        AuditTrailWrapper auditTrail = AuditTrailDocumentWrapper.createNewFromAdvisories(emptyAdvisory, newAdvisoryNode)
                .setAdvisoryId(advisoryId.toString())
                .setChangeType(ChangeType.Create)
                .setUser("Mustermann");

        String revision = couchDbService.writeCsafDocument(advisoryId, newAdvisoryNode.advisoryAsString());
        this.couchDbService.writeCsafDocument(UUID.randomUUID(), auditTrail.advisoryAsString());

        return new IdAndRevision(advisoryId.toString(), revision);
    }

    /**
     * get a specific advisory
     *
     * @param advisoryId the ID of the advisory to get
     * @return the requested advisory
     * @throws IdNotFoundException if there is no advisory with given ID
     */
    public AdvisoryResponse getAdvisory(String advisoryId) throws DatabaseException {
        InputStream advisoryStream = couchDbService.readCsafDocumentAsStream(advisoryId);
        try {
            AdvisoryWrapper advisory = AdvisoryWrapper.createFromCouchDb(advisoryStream);
            AdvisoryResponse response = new AdvisoryResponse(advisoryId, advisory.getWorkflowState(), advisory.getCsaf());
            response.setRevision(advisory.getRevision());
            return response;

        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Delete advisory with given id
     * @param advisoryId the ID of the advisory to delete
     * @param revision   the revision for concurrent control
     * @throws BadRequestException if the request was
     * @throws NotFoundException   if there is no advisory with given ID
     */
    public void deleteAdvisory(String advisoryId, String revision) throws DatabaseException {
        this.couchDbService.deleteCsafDocument(advisoryId, revision);

        try {
            var auditTrailDocs = this.readAllAuditTrailDocumentsFromDbFor(advisoryId);
            Collection<IdAndRevision> bulkDeletes = new ArrayList<>(auditTrailDocs.size());
            for (JsonNode doc : auditTrailDocs) {
                bulkDeletes.add(new IdAndRevision(CouchDbField.ID_FIELD.stringVal(doc),
                        CouchDbField.REVISION_FIELD.stringVal(doc)));
            }
            this.couchDbService.bulkDeleteDocuments(bulkDeletes);

        } catch (IOException ex) {
            throw new DatabaseException(ex);
        }
    }

    private List<JsonNode> readAllAuditTrailDocumentsFromDbFor(String advisoryId) throws IOException {

        Collection<DbField> fields = Arrays.asList(CouchDbField.ID_FIELD, CouchDbField.REVISION_FIELD);

        AndExpression searchExpr = new AndExpression(containsIgnoreCase("AuditTrail", TYPE_FIELD.getDbName()),
                equal(advisoryId, ADVISORY_ID.getDbName()));
        Map<String, Object> selector = expr2CouchDBFilter(searchExpr);
        return this.findDocuments(selector, fields);
    }


    /**
     * @param advisoryId                the ID of the advisory to update
     * @param revision                  the revision for concurrent control
     * @param changedCsafJson           the updated csaf json as string
     * @return the new revision of the updated csaf document
     * @throws JsonProcessingException if the given JSON string is not valid
     * @throws DatabaseException       if there was an error updating the advisory in the DB
     */
    public String updateAdvisory(String advisoryId, String revision, String changedCsafJson) throws IOException, DatabaseException {

        InputStream existingAdvisoryStream = this.couchDbService.readCsafDocumentAsStream(advisoryId);
        if (existingAdvisoryStream == null) {
            throw new DatabaseException("Invalid advisory ID!");
        }
        AdvisoryWrapper oldAdvisoryNode = AdvisoryWrapper.createFromCouchDb(existingAdvisoryStream);
        AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.updateFromExisting(oldAdvisoryNode, changedCsafJson);
        newAdvisoryNode.setRevision(revision);

        AuditTrailWrapper auditTrail = AuditTrailDocumentWrapper.createNewFromAdvisories(oldAdvisoryNode, newAdvisoryNode)
            .setAdvisoryId(advisoryId)
            .setChangeType(ChangeType.Update)
            .setUser("Mustermann");

        String result =  this.couchDbService.updateCsafDocument(newAdvisoryNode.advisoryAsString());
        this.couchDbService.writeCsafDocument(UUID.randomUUID(), auditTrail.advisoryAsString());
        return result;
    }

    /**
     * @param advisoryId       the ID of the advisory to update the workflow state of
     * @param revision         the revision for concurrent control
     * @param newWorkflowState the new workflow state to set
     * @return the new revision of the updated csaf document
     * @throws DatabaseException if there was an error updating the advisory in the DB
     */
    public String changeAdvisoryWorkflowState(String advisoryId, String revision, WorkflowState newWorkflowState) throws IOException, DatabaseException {

        InputStream existingAdvisoryStream = couchDbService.readCsafDocumentAsStream(advisoryId);
        if (existingAdvisoryStream == null) {
            throw new DatabaseException("Invalid advisory ID!");
        }
        AdvisoryWrapper existingAdvisoryNode = AdvisoryWrapper.createFromCouchDb(existingAdvisoryStream);

        AuditTrailWrapper auditTrail = AuditTrailWorkflowWrapper.createNewFrom(newWorkflowState, existingAdvisoryNode.getWorkflowState())
                .setAdvisoryId(advisoryId)
                .setCreatedAtToNow()
                .setChangeType(ChangeType.Update)
                .setUser("Mustermann")
                .setDocVersion(existingAdvisoryNode.getDocumentTrackingVersion())
                .setOldDocVersion(existingAdvisoryNode.getDocumentTrackingVersion());
        this.couchDbService.writeCsafDocument(UUID.randomUUID(), auditTrail.advisoryAsString());

        existingAdvisoryNode.setWorkflowState(newWorkflowState);
        existingAdvisoryNode.setRevision(revision);
        return this.couchDbService.updateCsafDocument(existingAdvisoryNode.advisoryAsString());
    }



}
