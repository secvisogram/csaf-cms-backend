package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryAuditTrailField.ADVISORY_ID;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.TYPE_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.containsIgnoreCase;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.equal;
import static de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryWorkflowUtil.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.*;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.json.*;
import de.bsi.secvisogram.csaf_cms_backend.model.ChangeType;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.AndExpression;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateCommentRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<AdvisoryInformationResponse> getAdvisoryInformations(String expression) throws IOException, CsafException {

        Map<DbField, BiConsumer<AdvisoryInformationResponse, String>> infoFields = AdvisoryWorkflowUtil.advisoryReadFields();
        Map<String, Object> selector = AdvisorySearchUtil.buildAdvisoryExpression(expression);
        List<JsonNode> docList = this.findDocuments(selector, new ArrayList<>(infoFields.keySet()));

        List<AdvisoryInformationResponse> allResponses =  docList.stream()
                .map(couchDbDoc -> AdvisoryWrapper.convertToAdvisoryInfo(couchDbDoc, infoFields))
                .toList();

        Authentication credentials = getAuthentication();
        // set calculated fields in response
        for (AdvisoryInformationResponse response : allResponses) {
            response.setDeletable(AdvisoryWorkflowUtil.canDeleteAdvisory(response, credentials));
            response.setChangeable(AdvisoryWorkflowUtil.canChangeAdvisory(response, credentials));
            response.setAllowedStateChanges(getAllowedStates(response, credentials));
        }
        return allResponses
                .stream()
                .filter(response -> canViewAdvisory(response, credentials))
                .collect(Collectors.toList());
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
    public IdAndRevision addAdvisory(String newCsafJson) throws IOException, CsafException {

        LOG.debug("addAdvisory");
        Authentication credentials = getAuthentication();

        return addAdvisoryForCredentials(newCsafJson, credentials);
    }

    IdAndRevision addAdvisoryForCredentials(String newCsafJson, Authentication credentials) throws IOException, CsafException {

        UUID advisoryId = UUID.randomUUID();
        AdvisoryWrapper emptyAdvisory = AdvisoryWrapper.createInitialEmptyAdvisoryForUser(credentials.getName());
        AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.createNewFromCsaf(newCsafJson, credentials.getName());
        AuditTrailWrapper auditTrail = AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(emptyAdvisory, newAdvisoryNode)
                .setAdvisoryId(advisoryId.toString())
                .setChangeType(ChangeType.Create)
                .setUser(credentials.getName());

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

                AdvisoryResponse response = new AdvisoryResponse(advisoryId, advisory.getWorkflowState(), advisory.getCsaf());
                response.setTitle(advisory.getDocumentTitle());
                response.setCurrentReleaseDate(advisory.getDocumentTrackingCurrentReleaseDate());
                response.setDocumentTrackingId(advisory.getDocumentTrackingId());
                response.setOwner(advisory.getOwner());
                response.setDeletable(AdvisoryWorkflowUtil.canDeleteAdvisory(response, credentials));
                response.setChangeable(AdvisoryWorkflowUtil.canChangeAdvisory(response, credentials));
                response.setAllowedStateChanges(getAllowedStates(response, credentials));
                response.setRevision(advisory.getRevision());
                return response;
            } else {
                throw new CsafException("The user has no permission to view this advisory",
                        CsafExceptionKey.NoPermissionForAdvisory, HttpStatus.UNAUTHORIZED);
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
    public void deleteAdvisory(String advisoryId, String revision) throws DatabaseException, IOException {

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
     * @param changedCsafJson the updated csaf json as string
     * @return the new revision of the updated csaf document
     * @throws JsonProcessingException if the given JSON string is not valid
     * @throws DatabaseException       if there was an error updating the advisory in the DB
     */
    public String updateAdvisory(String advisoryId, String revision, String changedCsafJson) throws IOException, DatabaseException, CsafException {

        LOG.debug("updateAdvisory");
        try (InputStream existingAdvisoryStream = this.couchDbService.readDocumentAsStream(advisoryId)) {

            if (existingAdvisoryStream == null) {
                throw new DatabaseException("Invalid advisory ID!");
            }
            AdvisoryWrapper oldAdvisoryNode = AdvisoryWrapper.createFromCouchDb(existingAdvisoryStream);
            Authentication credentials = getAuthentication();
            if (canChangeAdvisory(oldAdvisoryNode, credentials)) {

                AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.updateFromExisting(oldAdvisoryNode, changedCsafJson);
                newAdvisoryNode.setRevision(revision);
                String result = this.couchDbService.updateDocument(newAdvisoryNode.advisoryAsString());

                AuditTrailWrapper auditTrail = AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(oldAdvisoryNode, newAdvisoryNode)
                        .setAdvisoryId(advisoryId)
                        .setChangeType(ChangeType.Update)
                        .setUser(credentials.getName());
                this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());
                return result;
            } else {
                throw new CsafException("User has no permission to edit the advisory",
                        CsafExceptionKey.NoPermissionForAdvisory, HttpStatus.UNAUTHORIZED);
            }
        }
    }

    /**
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

            AuditTrailWrapper auditTrail = AdvisoryAuditTrailWorkflowWrapper.createNewFrom(newWorkflowState, existingAdvisoryNode.getWorkflowState())
                    .setDocVersion(existingAdvisoryNode.getDocumentTrackingVersion())
                    .setOldDocVersion(existingAdvisoryNode.getDocumentTrackingVersion())
                    .setAdvisoryId(advisoryId)
                    .setUser(credentials.getName());
            this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());

            existingAdvisoryNode.setWorkflowState(newWorkflowState);
            if (documentTrackingStatus != null) {
                existingAdvisoryNode.setDocumentTrackingStatus(documentTrackingStatus);
            }

            if (proposedTime != null) {
                existingAdvisoryNode.setDocumentTrackingCurrentReleaseDate(proposedTime);
            }

            if (newWorkflowState == WorkflowState.Published) {
                existingAdvisoryNode.setDocumentTrackingInitialReleaseDate(proposedTime != null
                        ? proposedTime
                        : DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            }

            existingAdvisoryNode.setRevision(revision);
            return this.couchDbService.updateDocument(existingAdvisoryNode.advisoryAsString());
        } else {
            throw new CsafException("User has not the permission to view change the workflow state of the advisory",
                    CsafExceptionKey.NoPermissionForAdvisory, HttpStatus.UNAUTHORIZED);

        }
    }

    public String createNewCsafDocumentVersion(String advisoryId, String revision)
            throws IOException, DatabaseException, CsafException {

        LOG.debug("createNewCsafDocumentVersion");
        Authentication credentials = getAuthentication();
        InputStream existingAdvisoryStream = couchDbService.readDocumentAsStream(advisoryId);
        if (existingAdvisoryStream == null) {
            throw new DatabaseException("Invalid advisory ID!");
        }
        AdvisoryWrapper existingAdvisoryNode = AdvisoryWrapper.createFromCouchDb(existingAdvisoryStream);

        if (canCreateNewVersion(existingAdvisoryNode)) {

            existingAdvisoryNode.setWorkflowState(WorkflowState.Draft);
            existingAdvisoryNode.setDocumentTrackingStatus(DocumentTrackingStatus.Draft);
            existingAdvisoryNode.setDocumentTrackingCurrentReleaseDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            existingAdvisoryNode.setRevision(revision);

            AuditTrailWrapper auditTrail = AdvisoryAuditTrailWorkflowWrapper.createNewFrom(WorkflowState.Draft, existingAdvisoryNode.getWorkflowState())
                    .setDocVersion(existingAdvisoryNode.getDocumentTrackingVersion())
                    .setOldDocVersion(existingAdvisoryNode.getDocumentTrackingVersion())
                    .setAdvisoryId(advisoryId)
                    .setUser(credentials.getName());
            this.couchDbService.writeDocument(UUID.randomUUID(), auditTrail.auditTrailAsString());

            return this.couchDbService.updateDocument(existingAdvisoryNode.advisoryAsString());
        } else {
            throw new CsafException("User has not the permission to create a new Version in this state",
                    CsafExceptionKey.NoPermissionForAdvisory, HttpStatus.UNAUTHORIZED);

        }
    }

    /**
     * Adds a comment to the advisory
     *
     * @param advisoryId  the ID of the advisory to add the comment to
     * @param comment     the comment to add as JSON string, requires a commentText
     * @return a tuple of ID and revision of the added comment
     * @throws DatabaseException when there are database errors
     * @throws CsafException   when a known csaf exception occurs
     */
    @RolesAllowed({ CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER })
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
    @RolesAllowed({ CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_AUDITOR })
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
                        CsafExceptionKey.NoPermissionForAdvisory, HttpStatus.UNAUTHORIZED);
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
    @RolesAllowed({ CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_AUDITOR })
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
    @RolesAllowed({ CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER })
    public String updateComment(String advisoryId, String commentId, String revision, String newText) throws IOException, DatabaseException {

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
     * @param commentText  the answer to add, requires a commentText
     * @return a tuple of ID and revision of the added comment
     * @throws DatabaseException when there are database errors
     * @throws CsafException       when there are errors in reading advisory
     */
    @RolesAllowed({ CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER })
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
    @RolesAllowed({ CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_AUDITOR })
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
     * @return the credentials
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
