package de.bsi.secvisogram.csaf_cms_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.cloud.cloudant.v1.model.Document;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.*;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.AuditTrailDocumentWrapper;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdvisoryService {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryService.class);

    @Autowired
    public CouchDbService couchDbService;

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
    public List<AdvisoryInformationResponse> getAdvisoryIds() {

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
     * Adds an advisory to the system
     *
     * @param newCsafJson the advisory as JSON String
     * @return a tuple of assigned id as UUID and the current revision for concurrent control
     * @throws JsonProcessingException  if the given JSON string is not valid
     */
    public IdAndRevision addAdvisory(String newCsafJson) throws IOException {

        UUID advisoryId = UUID.randomUUID();
        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(newCsafJson, "Mustermann");
        String revision = couchDbService.writeCsafDocument(advisoryId, advisory.getAdvisoryNode());
        return new IdAndRevision(advisoryId, revision);
    }

    /**
     * get a specific advisory
     *
     * @param advisoryId the ID of the advisory to get
     * @return the requested advisory
     * @throws IdNotFoundException if there is no advisory with given ID
     */
    public AdvisoryResponse getAdvisory(UUID advisoryId) throws DatabaseException {
        InputStream advisoryStream = couchDbService.readCsafDocumentAsStream(advisoryId.toString());
        try {
            AdvisoryWrapper advisory = AdvisoryWrapper.createFromCouchDb(advisoryStream);
            AdvisoryResponse response = new AdvisoryResponse(advisoryId.toString(), advisory.getWorkflowState(), advisory.getCsaf());
            response.setRevision(advisory.getRevision());
            return response;

        } catch (IOException e) {
            throw new DatabaseException(e);
        }
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
     * @param changedCsafJson           the updated csaf json as string
     * @return the new revision of the updated csaf document
     * @throws JsonProcessingException if the given JSON string is not valid
     * @throws DatabaseException       if there was an error updating the advisory in the DB
     */
    public String updateAdvisory(UUID advisoryId, String revision, String changedCsafJson) throws IOException, DatabaseException {

        InputStream existingAdvisory = this.couchDbService.readCsafDocumentAsStream(advisoryId.toString());
        if (existingAdvisory == null) {
            throw new DatabaseException("Invalid advisory ID!");
        }
        AdvisoryWrapper oldAdvisoryNode = AdvisoryWrapper.createFromCouchDb(existingAdvisory);
        AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.updateFromExisting(oldAdvisoryNode, changedCsafJson);
        newAdvisoryNode.setRevision(revision);

        JsonNode patch = AdvisoryWrapper.calculateJsonDiff(oldAdvisoryNode.getAdvisoryNode(),
                newAdvisoryNode.getAdvisoryNode());
        AuditTrailDocumentWrapper auditTrail = AuditTrailDocumentWrapper.createNewFromPatch(patch)
            .setAdvisoryId(advisoryId.toString())
            .setCreatedAtToNow()
            .setChangeType(AuditTrailDocumentWrapper.ChangeType.UPDATED)
            .setUser("Mustermann")
            .setDocVersion("")
            .setOldDocVersion("");
        this.couchDbService.writeCsafDocument(UUID.randomUUID(), auditTrail.getAuditTrailNode());

        return this.couchDbService.updateCsafDocument(newAdvisoryNode.getAdvisoryNode());
    }

    /**
     * @param advisoryId       the ID of the advisory to update the workflow state of
     * @param revision         the revision for concurrent control
     * @param newWorkflowState the new workflow state to set
     * @return the new revision of the updated csaf document
     * @throws DatabaseException if there was an error updating the advisory in the DB
     */
    public String changeAdvisoryWorkflowState(UUID advisoryId, String revision, WorkflowState newWorkflowState) throws IOException, DatabaseException {

        InputStream advisoryStream = couchDbService.readCsafDocumentAsStream(advisoryId.toString());
        AdvisoryWrapper advisoryNode = AdvisoryWrapper.createFromCouchDb(advisoryStream);
        advisoryNode.setWorkflowState(newWorkflowState);
        return updateAdvisory(advisoryId, revision, advisoryNode.advisoryAsString());
    }



}
