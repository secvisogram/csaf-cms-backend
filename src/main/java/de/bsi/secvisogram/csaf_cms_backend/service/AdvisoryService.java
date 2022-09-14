package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.AUDITOR;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryAuditTrailField.ADVISORY_ID;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.TYPE_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey.NoPermissionForAdvisory;
import static de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey.SummaryInHistoryEmpty;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.containsIgnoreCase;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.equal;
import static de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryWorkflowUtil.*;
import static java.util.Collections.emptyList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.*;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.json.*;
import de.bsi.secvisogram.csaf_cms_backend.model.ChangeType;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.AndExpression;
import de.bsi.secvisogram.csaf_cms_backend.mustache.JavascriptExporter;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateCommentRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.*;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AdvisoryService {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryService.class);
    @Autowired
    private CouchDbService couchDbService;

    @Autowired
    private JavascriptExporter javascriptExporter;

    @Autowired
    private PandocService pandocService;

    @Autowired
    private WeasyprintService weasyprintService;

    @Value("${csaf.document.versioning}")
    private String versioningStrategy;

    @Value("${csaf.validation.baseurl}")
    private String validationBaseUrl;

    @Autowired
    private CsafConfiguration configuration;

    /**
     * get number of documents
     *
     * @return number of all documents in the DB
     */
    public Long getDocumentCount() {
        return couchDbService.getDocumentCount();
    }

    /**
     * get information on all advisories
     *
     * @return a list of information objects
     */
    @RolesAllowed({CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR})
    public List<AdvisoryInformationResponse> getAdvisoryInformations(String expression) throws IOException, CsafException {

        Authentication credentials = getAuthentication();
        List<AdvisoryInformationResponse> allAdvisories = readAllAdvisories(expression, ObjectType.Advisory);
        // set calculated fields in response
        for (AdvisoryInformationResponse response : allAdvisories) {
            response.setDeletable(canDeleteAdvisory(response, credentials));
            response.setChangeable(canChangeAdvisory(response, credentials));
            response.setAllowedStateChanges(getAllowedStates(response, credentials));
            response.setCanCreateVersion(canCreateNewVersion(response, credentials));
        }
        List<AdvisoryInformationResponse> allResponses
                = new ArrayList<>(allAdvisories
                    .stream()
                    .filter(response -> canViewAdvisory(response, credentials))
                    .toList());

        if (hasRole(AUDITOR, credentials)) {
            List<AdvisoryInformationResponse> allAdvisoryVersions = readAllAdvisories(expression, ObjectType.AdvisoryVersion);
            for (AdvisoryInformationResponse response : allAdvisoryVersions) {
                response.setDeletable(false);
                response.setChangeable(false);
                response.setAllowedStateChanges(emptyList());
                response.setCanCreateVersion(false);
            }
            allResponses.addAll(allAdvisoryVersions);
        }
        return allResponses;
    }

    private List<AdvisoryInformationResponse> readAllAdvisories(String expression, ObjectType objectType) throws CsafException, IOException {

        Map<DbField, BiConsumer<AdvisoryInformationResponse, String>> infoFields = AdvisoryWorkflowUtil.advisoryReadFields();
        Map<String, Object> selector = AdvisorySearchUtil.buildAdvisoryExpression(expression, objectType);
        List<JsonNode> docList = this.findDocuments(selector, new ArrayList<>(infoFields.keySet()));
        return docList.stream()
                .map(couchDbDoc -> AdvisoryWrapper.convertToAdvisoryInfo(couchDbDoc, infoFields))
                .toList();
    }

    private List<WorkflowState> getAllowedStates(AdvisoryInformationResponse response, Authentication credentials) {

        return Arrays.stream(WorkflowState.values())
                .filter(state -> AdvisoryWorkflowUtil.canChangeWorkflow(response, state, credentials))
                .collect(Collectors.toList());
    }

    /**
     * read from {@link CouchDbService#findDocumentsAsStream(Map, Collection)} and convert it to a list of JsonNode
     *
     * @param selector the selector to search for
     * @param fields   the fields of information to select
     * @return the result nodes of the search
     */
    List<JsonNode> findDocuments(Map<String, Object> selector, Collection<DbField> fields) throws IOException {

        return AdvisoryWorkflowUtil.findDocuments(this.couchDbService, selector, fields);
    }

    /**
     * Adds an advisory to the system
     *
     * @param newCsafJson the advisory as JSON String
     * @return a tuple of assigned id as UUID and the current revision for concurrent control
     * @throws JsonProcessingException if the given JSON string is not valid
     */
    @RolesAllowed({ CsafRoles.ROLE_AUTHOR})
    public IdAndRevision addAdvisory(CreateAdvisoryRequest newCsafJson) throws IOException, CsafException {

        LOG.debug("addAdvisory");
        Authentication credentials = getAuthentication();

        return addAdvisoryForCredentials(newCsafJson, credentials);
    }

    IdAndRevision addAdvisoryForCredentials(CreateAdvisoryRequest newCsafJson, Authentication credentials) throws IOException, CsafException {

        if (newCsafJson.getSummary() == null || newCsafJson.getSummary().isBlank()) {
            throw new CsafException("Summary must not be empty", SummaryInHistoryEmpty, BAD_REQUEST);
        }

        UUID advisoryId = UUID.randomUUID();
        AdvisoryWrapper emptyAdvisory = AdvisoryWrapper.createInitialEmptyAdvisoryForUser(credentials.getName());
        AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.createNewFromCsaf(newCsafJson, credentials.getName(),
                this.versioningStrategy);
        AuditTrailWrapper auditTrail = AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(emptyAdvisory, newAdvisoryNode)
                .setAdvisoryId(advisoryId.toString())
                .setChangeType(ChangeType.Create)
                .setUser(credentials.getName());

        newAdvisoryNode.removeAllRevisionHistoryEntries();
        newAdvisoryNode.addRevisionHistoryEntry(newCsafJson);

        String revision = couchDbService.writeDocument(advisoryId, newAdvisoryNode.advisoryAsString());
        this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());

        return new IdAndRevision(advisoryId.toString(), revision);
    }

    /**
     * get a specific advisory
     *
     * @param advisoryId the ID of the advisory to get
     * @return the requested advisory
     * @throws IdNotFoundException if there is no advisory with given ID
     */
    public AdvisoryResponse getAdvisory(String advisoryId) throws DatabaseException, CsafException {

        try (InputStream advisoryStream = couchDbService.readDocumentAsStream(advisoryId)) {

            AdvisoryWrapper advisory = AdvisoryWrapper.createFromCouchDb(advisoryStream);
            if (canViewAdvisory(advisory, getAuthentication())) {

                Authentication credentials = getAuthentication();
                boolean isVersion = advisory.getType() == ObjectType.AdvisoryVersion;

                AdvisoryResponse response = new AdvisoryResponse(advisoryId, advisory.getWorkflowState(), advisory.getCsaf());
                response.setTitle(advisory.getDocumentTitle());
                response.setCurrentReleaseDate(advisory.getDocumentTrackingCurrentReleaseDate());
                response.setDocumentTrackingId(advisory.getDocumentTrackingId());
                response.setOwner(advisory.getOwner());
                response.setDeletable(!isVersion && canDeleteAdvisory(response, credentials));
                response.setChangeable(!isVersion && canChangeAdvisory(response, credentials));
                response.setCanCreateVersion(!isVersion && canCreateNewVersion(response, credentials));
                List<WorkflowState> allowedStateChanges = (!isVersion) ? getAllowedStates(response, credentials) : emptyList();
                response.setAllowedStateChanges(allowedStateChanges);
                response.setRevision(advisory.getRevision());
                return response;
            } else {
                throw new CsafException("The user has no permission to view this advisory",
                        NoPermissionForAdvisory, UNAUTHORIZED);
            }
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Deletes an advisory with given id from the database and all comments and answers belonging to it
     *
     * @param advisoryId the ID of the advisory to delete
     * @param revision   the revision for concurrent control
     * @throws BadRequestException if the request was
     * @throws NotFoundException   if there is no advisory with given ID
     */
    @RolesAllowed({ CsafRoles.ROLE_AUTHOR})
    public void deleteAdvisory(String advisoryId, String revision) throws DatabaseException, IOException, CsafException {

        LOG.debug("deleteAdvisory");
        InputStream advisoryStream = couchDbService.readDocumentAsStream(advisoryId);
        AdvisoryWrapper advisory = AdvisoryWrapper.createFromCouchDb(advisoryStream);
        if (canDeleteAdvisory(advisory, getAuthentication())) {

            this.couchDbService.deleteDocument(advisoryId, revision);
            deleteAllAuditTrailDocumentsFromDbFor(advisoryId, ADVISORY_ID.getDbName());
            deleteAllCommentsFromDbForAdvisory(advisoryId);
        } else {
            throw new AccessDeniedException("User has not the permission to delete the advisory");
        }
    }

    private void deleteAllCommentsFromDbForAdvisory(String advisoryId) throws IOException, DatabaseException {
        AndExpression searchExpr = new AndExpression(equal(ObjectType.Comment.name(), TYPE_FIELD.getDbName()),
                equal(advisoryId, CommentField.ADVISORY_ID.getDbName()));

        Collection<DbField> fields = Arrays.asList(CouchDbField.ID_FIELD, CouchDbField.REVISION_FIELD);

        Map<String, Object> selector = expr2CouchDBFilter(searchExpr);
        List<JsonNode> commentsToDelete = this.findDocuments(selector, fields);

        Collection<IdAndRevision> bulkDeletes = new ArrayList<>(commentsToDelete.size());
        for (JsonNode doc : commentsToDelete) {
            String commentId = CouchDbField.ID_FIELD.stringVal(doc);
            String commentRev = CouchDbField.REVISION_FIELD.stringVal(doc);
            deleteComment(commentId, commentRev);
        }
        this.couchDbService.bulkDeleteDocuments(bulkDeletes);
    }

    private void deleteAllAuditTrailDocumentsFromDbFor(String itemId, String idKey) throws IOException, DatabaseException {

        Collection<DbField> fields = Arrays.asList(CouchDbField.ID_FIELD, CouchDbField.REVISION_FIELD);

        AndExpression searchExpr = new AndExpression(containsIgnoreCase("AuditTrail", TYPE_FIELD.getDbName()),
                equal(itemId, idKey));
        Map<String, Object> selector = expr2CouchDBFilter(searchExpr);
        var auditTrailDocs = this.findDocuments(selector, fields);

        Collection<IdAndRevision> bulkDeletes = new ArrayList<>(auditTrailDocs.size());
        for (JsonNode doc : auditTrailDocs) {
            bulkDeletes.add(new IdAndRevision(CouchDbField.ID_FIELD.stringVal(doc),
                    CouchDbField.REVISION_FIELD.stringVal(doc)));
        }
        this.couchDbService.bulkDeleteDocuments(bulkDeletes);

    }

    /**
     * @param advisoryId      the ID of the advisory to update
     * @param revision        the revision for concurrent control
     * @param changedCsafJson the updated csaf json
     * @return the new revision of the updated csaf document
     * @throws JsonProcessingException if the given JSON string is not valid
     * @throws DatabaseException       if there was an error updating the advisory in the DB
     */
    public String updateAdvisory(String advisoryId, String revision, CreateAdvisoryRequest changedCsafJson) throws IOException, DatabaseException, CsafException {

        LOG.debug("updateAdvisory");
        try (InputStream existingAdvisoryStream = this.couchDbService.readDocumentAsStream(advisoryId)) {

            if (existingAdvisoryStream == null) {
                throw new DatabaseException("Invalid advisory ID!");
            }
            AdvisoryWrapper oldAdvisoryNode = AdvisoryWrapper.createFromCouchDb(existingAdvisoryStream);
            Authentication credentials = getAuthentication();
            if (canChangeAdvisory(oldAdvisoryNode, credentials)) {

                if (changedCsafJson.getSummary() == null || changedCsafJson.getSummary().isBlank()) {
                    throw new CsafException("Summary must not be empty", SummaryInHistoryEmpty, BAD_REQUEST);
                }

                AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.updateFromExisting(oldAdvisoryNode, changedCsafJson);
                newAdvisoryNode.setRevision(revision);
                PatchType changeType = AdvisoryWorkflowUtil.getChangeType(oldAdvisoryNode, newAdvisoryNode, configuration.getVersioning().getLevenshtein());
                String nextVersion = oldAdvisoryNode.getVersioningStrategy().getNextVersion(changeType, oldAdvisoryNode.getDocumentTrackingVersion(), oldAdvisoryNode.getLastVersion());
                newAdvisoryNode.setDocumentTrackingVersion(nextVersion);
                newAdvisoryNode.checkCurrentReleaseDateIsSet();
                newAdvisoryNode.addRevisionHistoryEntry(changedCsafJson);
                String result = this.couchDbService.updateDocument(newAdvisoryNode.advisoryAsString());

                AuditTrailWrapper auditTrail = AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(oldAdvisoryNode, newAdvisoryNode)
                        .setAdvisoryId(advisoryId)
                        .setChangeType(ChangeType.Update)
                        .setUser(credentials.getName());
                this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());
                return result;
            } else {
                throw new CsafException("User has no permission to edit the advisory", NoPermissionForAdvisory, UNAUTHORIZED);
            }
        }
    }

    /**
     * Export the Advisory with the given advisoryId in the given format. The export will be written to a
     * temporary file and the path to the file will be returned.
     *
     * @param advisoryId the id of the advisory that should be exported
     * @param format     the format in which the export should be written (default JSON on null)
     * @return the path to the temporary file that contains the export
     * @throws CsafException    if the advisory with the given id does not exist or the export format is unknown
     * @throws IOException          on any error regarding writing/reading from disk
     * @throws InterruptedException if the export did take too long and thus timed out
     */
    @RolesAllowed({CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR})
    public Path exportAdvisory(
            @Nonnull final String advisoryId,
            @Nullable final ExportFormat format)
            throws IOException, CsafException {
        // read the advisory form the database
        try {
            final InputStream existingAdvisoryStream = this.couchDbService.readDocumentAsStream(advisoryId);
            final AdvisoryWrapper advisoryNode = AdvisoryWrapper.createFromCouchDb(existingAdvisoryStream);
            final JsonNode csaf = advisoryNode.getCsaf();
            RemoveIdHelper.removeCommentIds(csaf);
            final String csafDocument = csaf.toString();

            // if format is JSON - write it to temporary file and return the path
            if (format == ExportFormat.JSON || format == null) {
                final Path jsonFile = Files.createTempFile("advisory__", ".json");
                Files.writeString(jsonFile, csafDocument);
                return jsonFile;
            } else {
                // other formats have to start with an HTML export first
                final String htmlExport = javascriptExporter.createHtml(csafDocument);
                final Path htmlFile = Files.createTempFile("advisory__", ".html");
                Files.writeString(htmlFile, htmlExport);
                if (format == ExportFormat.HTML) {
                    // we already have an HTML file - done!
                    return htmlFile;
                } else if (format == ExportFormat.Markdown && pandocService.isReady()) {
                    final Path markdownFile = Files.createTempFile("advisory__", ".md");
                    pandocService.convert(htmlFile, markdownFile);
                    Files.delete(htmlFile);
                    return markdownFile;
                } else if (format == ExportFormat.PDF && weasyprintService.isReady()) {
                    final Path pdfFile = Files.createTempFile("advisory__", ".pdf");
                    weasyprintService.convert(htmlFile, pdfFile);
                    Files.delete(htmlFile);
                    return pdfFile;
                }
                throw new CsafException("Unknown export format: " + format, CsafExceptionKey.UnknownExportFormat, BAD_REQUEST);
            }
        } catch (IdNotFoundException e) {
            throw new CsafException("Can not find advisory with ID " + advisoryId,
                    CsafExceptionKey.AdvisoryNotFound, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Changes the workflow state of the advisory to the given new WorkflowState
     *
     * @param advisoryId       the ID of the advisory to update the workflow state of
     * @param revision         the revision for concurrent control
     * @param newWorkflowState the new workflow state to set
     * @return the new revision of the updated csaf document
     * @throws DatabaseException if there was an error updating the advisory in the DB
     */
    public String changeAdvisoryWorkflowState(String advisoryId, String revision, WorkflowState newWorkflowState,
                                              String proposedTime, DocumentTrackingStatus documentTrackingStatus)
            throws IOException, DatabaseException, CsafException {

        Authentication credentials = getAuthentication();
        InputStream existingAdvisoryStream = couchDbService.readDocumentAsStream(advisoryId);
        if (existingAdvisoryStream == null) {
            throw new DatabaseException("Invalid advisory ID!");
        }
        AdvisoryWrapper existingAdvisoryNode = AdvisoryWrapper.createFromCouchDb(existingAdvisoryStream);

        if (canChangeWorkflow(existingAdvisoryNode, newWorkflowState, credentials)) {

            String workflowStateChangeMsg = "Status changed from " + existingAdvisoryNode.getWorkflowStateString() + " to " + newWorkflowState;

            existingAdvisoryNode.setWorkflowState(newWorkflowState);
            if (documentTrackingStatus != null) {
                existingAdvisoryNode.setDocumentTrackingStatus(documentTrackingStatus);
            }

            if (proposedTime != null) {
                existingAdvisoryNode.setDocumentTrackingCurrentReleaseDate(proposedTime);
            }

            if (newWorkflowState == WorkflowState.Approved) {
                String nextVersion = existingAdvisoryNode.getVersioningStrategy()
                        .getNextApprovedVersion(existingAdvisoryNode.getDocumentTrackingVersion());
                existingAdvisoryNode.setDocumentTrackingVersion(nextVersion);
                existingAdvisoryNode.addRevisionHistoryEntry(workflowStateChangeMsg, "");
            }

            if (newWorkflowState == WorkflowState.Draft) {
                String nextVersion = existingAdvisoryNode.getVersioningStrategy()
                        .getNextDraftVersion(existingAdvisoryNode.getDocumentTrackingVersion());
                existingAdvisoryNode.setDocumentTrackingVersion(nextVersion);
                existingAdvisoryNode.addRevisionHistoryEntry(workflowStateChangeMsg, "");
            }

            if (newWorkflowState == WorkflowState.RfPublication) {
                // In this step we only want to check if the document would be valid if published but not change it yet.
                createReleaseReadyAdvisoryAndValidate(existingAdvisoryNode, proposedTime);
            }

            if (newWorkflowState == WorkflowState.Published) {
                existingAdvisoryNode = createReleaseReadyAdvisoryAndValidate(existingAdvisoryNode, proposedTime);
            }

            AuditTrailWrapper auditTrail = AdvisoryAuditTrailWorkflowWrapper.createNewFrom(newWorkflowState, existingAdvisoryNode.getWorkflowState())
                    .setDocVersion(existingAdvisoryNode.getDocumentTrackingVersion())
                    .setOldDocVersion(existingAdvisoryNode.getDocumentTrackingVersion())
                    .setAdvisoryId(advisoryId)
                    .setUser(credentials.getName());
            this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());

            existingAdvisoryNode.setRevision(revision);
            return this.couchDbService.updateDocument(existingAdvisoryNode.advisoryAsString());
        } else {
            throw new CsafException("User has not the permission to change the workflow state of the advisory",
                    NoPermissionForAdvisory, UNAUTHORIZED);
        }
    }

    private AdvisoryWrapper createReleaseReadyAdvisoryAndValidate(AdvisoryWrapper advisory, String initialReleaseDate) throws CsafException, IOException {

        String summary = configuration.getSummary().getPublication();
        if (advisory.versionIsAfterInitialPublication()) {
            summary = advisory.getLastRevisionHistoryEntrySummary();
        }

        AdvisoryWrapper advisoryCopy = AdvisoryWrapper.createCopy(advisory);
        advisoryCopy.removeAllPrereleaseVersions();

        String versionWithoutSuffix = advisoryCopy.getVersioningStrategy()
                .removeVersionSuffix(advisoryCopy.getDocumentTrackingVersion());
        advisoryCopy.setDocumentTrackingVersion(versionWithoutSuffix);

        advisoryCopy.addRevisionHistoryEntry(summary, "");
        if (advisoryCopy.getLastMajorVersion() == 0) {
            advisoryCopy.setDocumentTrackingInitialReleaseDate(initialReleaseDate != null && !initialReleaseDate.isBlank()
                    ? initialReleaseDate
                    : DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        }

        if (! ValidatorServiceClient.isAdvisoryValid(this.validationBaseUrl, advisoryCopy)) {
            throw new CsafException("Advisory is no valid CSAF document",
                    CsafExceptionKey.AdvisoryValidationError, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return advisoryCopy;
    }

    /**
     * Adds a new version of the document in Draft workflow state
     *
     * @param advisoryId the ID of the advisory to create a new version of
     * @param revision   the revision for concurrent control
     * @return the revision of the updated CSAF document
     * @throws DatabaseException if there was an error updating the document in the database
     */
    public String createNewCsafDocumentVersion(String advisoryId, String revision)
            throws IOException, DatabaseException, CsafException {

        LOG.debug("createNewCsafDocumentVersion");
        Authentication credentials = getAuthentication();
        InputStream existingAdvisoryStream = couchDbService.readDocumentAsStream(advisoryId);
        if (existingAdvisoryStream == null) {
            throw new DatabaseException("Invalid advisory ID!");
        }
        AdvisoryWrapper existingAdvisoryNode = AdvisoryWrapper.createFromCouchDb(existingAdvisoryStream);

        if (canCreateNewVersion(existingAdvisoryNode, credentials)) {

            // make copy of current state
            AdvisoryWrapper advisoryVersionBackup = AdvisoryWrapper.createVersionFrom(existingAdvisoryNode);
            // Set existing version to Draft
            existingAdvisoryNode.setLastVersion(existingAdvisoryNode.getDocumentTrackingVersion());
            existingAdvisoryNode.setWorkflowState(WorkflowState.Draft);
            existingAdvisoryNode.setDocumentTrackingStatus(DocumentTrackingStatus.Draft);
            existingAdvisoryNode.setDocumentTrackingCurrentReleaseDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            existingAdvisoryNode.setDocumentTrackingVersion(existingAdvisoryNode.getVersioningStrategy()
                    .getNewDocumentVersion(existingAdvisoryNode.getDocumentTrackingVersion()));
            existingAdvisoryNode.addEntryForNewCreatedVersion("New Version", "");
            existingAdvisoryNode.setRevision(revision);

            AuditTrailWrapper auditTrail = AdvisoryAuditTrailWorkflowWrapper.createNewFrom(WorkflowState.Draft, existingAdvisoryNode.getWorkflowState())
                    .setDocVersion(existingAdvisoryNode.getDocumentTrackingVersion())
                    .setOldDocVersion(existingAdvisoryNode.getDocumentTrackingVersion())
                    .setAdvisoryId(advisoryId)
                    .setUser(credentials.getName());
            this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());
            this.deleteAllCommentsFromDbForAdvisory(existingAdvisoryNode.getAdvisoryId());

            String newRevision =  this.couchDbService.updateDocument(existingAdvisoryNode.advisoryAsString());

            this.couchDbService.writeDocument(UUID.randomUUID(), advisoryVersionBackup.advisoryAsString());

            return newRevision;
        } else {
            throw new CsafException("User has not the permission to create a new Version in this state",
                    NoPermissionForAdvisory, UNAUTHORIZED);

        }
    }

    /**
     * Adds a comment to the advisory
     *
     * @param advisoryId the ID of the advisory to add the comment to
     * @param comment    the comment to add as JSON string, requires a commentText
     * @return a tuple of ID and revision of the added comment
     * @throws DatabaseException when there are database errors
     * @throws CsafException     when a known csaf exception occurs
     */
    @RolesAllowed({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER})
    public IdAndRevision addComment(String advisoryId, CreateCommentRequest comment) throws DatabaseException, CsafException {

        LOG.debug("addComment");
        UUID commentId = UUID.randomUUID();
        Authentication credentials = getAuthentication();
        AdvisoryInformationResponse advisoryInfo = getAdvisoryForId(advisoryId, this.couchDbService);

        if (AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisoryInfo, credentials)) {

            CommentWrapper newComment = CommentWrapper.createNew(advisoryId, comment);
            newComment.setOwner(credentials.getName());
            String commentRevision = this.couchDbService.writeDocument(commentId, newComment.commentAsString());

            AuditTrailWrapper auditTrail = CommentAuditTrailWrapper.createNew(newComment)
                    .setCommentId(commentId.toString())
                    .setUser(credentials.getName());
            this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());
            return new IdAndRevision(commentId.toString(), commentRevision);
        } else {
            throw new AccessDeniedException("User has not the permission to add a comment to the advisory");
        }
    }

    /**
     * Get a specific comment (or answer)
     *
     * @param commentId the ID of the comment to get
     * @return the requested comment
     * @throws IdNotFoundException if there is no comment with given ID
     */
    @RolesAllowed({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_AUDITOR})
    public CommentResponse getComment(String commentId) throws DatabaseException, CsafException {


        try (InputStream commentStream = couchDbService.readDocumentAsStream(commentId)) {
            CommentWrapper comment = CommentWrapper.createFromCouchDb(commentStream);

            Authentication credentials = getAuthentication();
            AdvisoryInformationResponse advisoryInfo = getAdvisoryForId(comment.getAdvisoryId(), this.couchDbService);
            if (AdvisoryWorkflowUtil.canViewComment(advisoryInfo, credentials)) {
                return new CommentResponse(
                        commentId,
                        comment.getRevision(),
                        comment.getAdvisoryId(),
                        comment.getOwner(),
                        comment.getText(),
                        comment.getCsafNodeId(),
                        comment.getFieldName(),
                        comment.getAnswerTo()
                );
            } else {
                throw new CsafException("User has not the permission to view comment from the advisory",
                        NoPermissionForAdvisory, UNAUTHORIZED);
            }

        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Retrieves all comments for a given advisory
     *
     * @param advisoryId the ID of the advisory to get comments of
     * @return a list of information on all comments for the requested advisory
     * @throws IOException when there are errors in JSON handling
     */
    @RolesAllowed({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_AUDITOR})
    public List<CommentInformationResponse> getComments(String advisoryId) throws IOException, CsafException {

        Authentication credentials = getAuthentication();
        AdvisoryInformationResponse advisoryInfo = getAdvisoryForId(advisoryId, this.couchDbService);
        if (AdvisoryWorkflowUtil.canViewComment(advisoryInfo, credentials)) {

            List<DbField> fields = Arrays.asList(
                    CouchDbField.ID_FIELD,
                    CouchDbField.REVISION_FIELD,
                    CommentField.ADVISORY_ID,
                    CommentField.CSAF_NODE_ID,
                    CommentField.OWNER,
                    CommentField.ANSWER_TO
            );

            AndExpression searchExpr = new AndExpression(
                    equal(ObjectType.Comment.name(), TYPE_FIELD.getDbName()),
                    equal(advisoryId, CommentField.ADVISORY_ID.getDbName())
            );
            Map<String, Object> selector = expr2CouchDBFilter(searchExpr);
            List<JsonNode> commentInfosJson = this.findDocuments(selector, fields);

            return commentInfosJson.stream().map(CommentWrapper::convertToCommentInfo).toList();
        } else {
            throw new AccessDeniedException("User has not the permission to add a comment to the advisory");
        }
    }

    /**
     * Deletes a comment without its answers from the database
     *
     * @param commentId       the ID of the comment to remove
     * @param commentRevision the comment's revision for concurrent control
     * @throws DatabaseException when there are database errors
     * @throws IOException       when there are errors in JSON handling
     */
    void deleteComment(String commentId, String commentRevision) throws DatabaseException, IOException {

        couchDbService.deleteDocument(commentId, commentRevision);
        deleteAllAuditTrailDocumentsFromDbFor(commentId, CommentAuditTrailField.COMMENT_ID.getDbName());
    }

    /**
     * Updates the text of a comment (or answer)
     *
     * @param commentId the ID of the comment to update
     * @param revision  the revision for concurrent control
     * @param newText   the updated text of the comment
     * @return the new revision of the updated comment
     */
    @RolesAllowed({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER})
    public String updateComment(String advisoryId, String commentId, String revision, String newText) throws IOException, DatabaseException, CsafException {

        Authentication credentials = getAuthentication();
        InputStream existingCommentStream = this.couchDbService.readDocumentAsStream(commentId);
        if (existingCommentStream == null) {
            throw new DatabaseException("Invalid comment ID!");
        }
        CommentWrapper comment = CommentWrapper.createFromCouchDb(existingCommentStream);
        final String commentOwner = comment.getOwner();
        if (commentOwner == null || !commentOwner.equals(credentials.getName())) {
            throw new AccessDeniedException("User has not the permission to change the comment");
        }
        comment.setRevision(revision);
        comment.setText(newText);

        AuditTrailWrapper auditTrail = CommentAuditTrailWrapper.createNew(comment)
                .setCommentId(commentId)
                .setCommentText(newText)
                .setCreatedAtToNow()
                .setChangeType(ChangeType.Update)
                .setUser("Mustermann");

        String newRevision = this.couchDbService.updateDocument(comment.commentAsString());
        this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());
        return newRevision;
    }

    /**
     * Adds an answer to a comment
     *
     * @param commentId   the ID of the comment to add the answer to
     * @param commentText the answer to add, requires a commentText
     * @return a tuple of ID and revision of the added comment
     * @throws DatabaseException when there are database errors
     * @throws CsafException     when there are errors in reading advisory
     */
    @RolesAllowed({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER})
    public IdAndRevision addAnswer(String advisoryId, String commentId, String commentText) throws DatabaseException, CsafException {

        Authentication credentials = getAuthentication();
        AdvisoryInformationResponse advisoryInfo = getAdvisoryForId(advisoryId, this.couchDbService);
        if (AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisoryInfo, credentials)) {

            UUID answerId = UUID.randomUUID();

            CommentWrapper newAnswer = CommentWrapper.createNewAnswerFromJson(advisoryId, commentId, commentText);
            newAnswer.setOwner(credentials.getName());
            String commentRevision = this.couchDbService.writeDocument(answerId, newAnswer.commentAsString());

            AuditTrailWrapper auditTrail = CommentAuditTrailWrapper.createNew(newAnswer)
                    .setCommentId(answerId.toString())
                    .setChangeType(ChangeType.Create)
                    .setUser(credentials.getName());
            this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());

            return new IdAndRevision(answerId.toString(), commentRevision);
        } else {
            throw new AccessDeniedException("User has not the permission to add a comment to the advisory");
        }
    }

    /**
     * Retrieves all answers for a given comment
     *
     * @param commentId the ID of the comment to get answers of
     * @return a list of information on all answers for the requested comment
     * @throws IOException when there are errors in JSON handling
     */
    @RolesAllowed({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_AUDITOR})
    public List<AnswerInformationResponse> getAnswers(String advisoryId, String commentId) throws IOException, CsafException {

        Authentication credentials = getAuthentication();
        AdvisoryInformationResponse advisoryInfo = getAdvisoryForId(advisoryId, this.couchDbService);
        if (AdvisoryWorkflowUtil.canViewComment(advisoryInfo, credentials)) {
            List<DbField> fields = Arrays.asList(
                    CouchDbField.ID_FIELD, CouchDbField.REVISION_FIELD, CommentField.ANSWER_TO, CommentField.OWNER);

            AndExpression searchExpr = new AndExpression(
                    equal(ObjectType.Comment.name(), TYPE_FIELD.getDbName()),
                    equal(commentId, CommentField.ANSWER_TO.getDbName())
            );
            Map<String, Object> selector = expr2CouchDBFilter(searchExpr);
            List<JsonNode> answerInfosJson = this.findDocuments(selector, fields);

            return answerInfosJson.stream().map(CommentWrapper::convertToAnswerInfo).toList();
        } else {
            throw new AccessDeniedException("User has not the permission to view comments of the advisory");
        }
    }

    /**
     * Deletes an answer from the database
     *
     * @param answerId       the ID of the comment to remove
     * @param answerRevision the comment's revision for concurrent control
     * @throws DatabaseException when there are database errors
     * @throws IOException       when there are errors in JSON handling
     */
    void deleteAnswer(String answerId, String answerRevision) throws DatabaseException, IOException {
        couchDbService.deleteDocument(answerId, answerRevision);
        deleteAllAuditTrailDocumentsFromDbFor(answerId, CommentAuditTrailField.COMMENT_ID.getDbName());
    }

    /**
     * Get the credentials for the authenticated in user.
     *
     * @return the credentials
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
