package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.*;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.ID_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.TYPE_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.equal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.*;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.ObjectType;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.AndExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Helper Methods for workflow and permissions
 */
public class AdvisoryWorkflowUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryWorkflowUtil.class);

    /**
     * Check whether the given advisory info can be deleted with the given credentials
     * @param response the advisory info to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    public static boolean canDeleteAdvisory(AdvisoryInformationResponse response, Authentication credentials) {

        return canDeleteAdvisory(response.getOwner(), response.getWorkflowState(), credentials);
    }

    /**
     * Check whether the given advisory can be deleted with the given credentials
     * @param advisory the advisory to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    public static boolean canDeleteAdvisory(AdvisoryWrapper advisory, Authentication credentials) {

        return canDeleteAdvisory(advisory.getOwner(), advisory.getWorkflowState(), credentials);
    }

    /**
     * Check whether a advisory with the geiven user and state can be deleted with the given credentials
     * @param userToCheck the advisory user to checck
     * @param stateToCheck the advisory workflow state to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    public static boolean canDeleteAdvisory(String userToCheck, WorkflowState stateToCheck, Authentication credentials) {

        boolean canBeDeleted = false;
        if (hasRole(AUTHOR, credentials)) {
            canBeDeleted = isOwnAdvisory(userToCheck, credentials) && isInStateDraft(stateToCheck);
        }
        if (hasRole(EDITOR, credentials)) {
            canBeDeleted = isInStateDraft(stateToCheck);
        }
        if (hasRole(MANAGER, credentials)) {
            canBeDeleted = true;
        }
        return canBeDeleted;
    }

    /**
     * Check whether the given advisory info can be deleted with the given credentials
     * @param response the advisory info to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    public static boolean canChangeAdvisory(AdvisoryInformationResponse response, Authentication credentials) {

        return canChangeAdvisory(response.getOwner(), response.getWorkflowState(), credentials);
    }

    public static boolean canChangeAdvisory(String userToCheck, WorkflowState advisoryState, Authentication credentials) {

        boolean canBeChanged = false;
        if (hasRole(AUTHOR, credentials)) {
            canBeChanged = isOwnAdvisory(userToCheck, credentials) && isInStateDraft(advisoryState);
        }
        if (hasRole(EDITOR, credentials)) {
            canBeChanged = isInStateDraft(advisoryState);
        }

        return canBeChanged;
    }

    /**
     * Check whether comments or Answers can be added to the  given advisory with the given credentials
     * @param advisory the advisory info to check
     * @param credentials the credentials for the check
     * @return true - comments/answers can be added
     */
    public static boolean canAddAndReplyCommentToAdvisory(AdvisoryInformationResponse advisory, Authentication credentials) {

        String advisoryOwner = advisory.getOwner();
        WorkflowState advisoryState = advisory.getWorkflowState();
        boolean canBeAdded = false;

        if (hasRole(AUTHOR, credentials)) {
            canBeAdded = isOwnAdvisory(advisoryOwner, credentials)
                    && isInStateDraft(advisoryState);
        }
        if (hasRole(EDITOR, credentials)) {
            canBeAdded = isInStateDraft(advisoryState);
        }
        if (hasRole(REVIEWER, credentials)) {
            canBeAdded = isInState(advisoryState, WorkflowState.Draft, WorkflowState.Review, WorkflowState.Approved);
        }

        return canBeAdded;
    }

    /**
     * Check whether comments or Answers can be listed or viewed
     * @param advisory the advisory info to check
     * @param credentials the credentials for the check
     * @return true - comment/answer can be viewed/listed
     */
    public static boolean canViewComment(AdvisoryInformationResponse advisory, Authentication credentials) {

        String advisoryOwner = advisory.getOwner();
        WorkflowState advisoryState = advisory.getWorkflowState();
        boolean canBeAdded = false;

        if (hasRole(AUTHOR, credentials)) {
            canBeAdded = isOwnAdvisory(advisoryOwner, credentials)
                    && isInStateDraft(advisoryState);
        }
        if (hasRole(EDITOR, credentials)) {
            canBeAdded = isInStateDraft(advisoryState);
        }
        if (hasRole(REVIEWER, credentials)) {
            canBeAdded = isInState(advisoryState, WorkflowState.Draft, WorkflowState.Review, WorkflowState.Approved);
        }
        if (hasRole(AUDITOR, credentials)) {
            canBeAdded = isInState(advisoryState, WorkflowState.Draft, WorkflowState.Review, WorkflowState.Approved);
        }

        return canBeAdded;
    }


    public static boolean hasRole(CsafRoles.Role csafRole, Authentication credentials) {

        return credentials.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(csafRole.getRoleName()));
    }

    public static boolean isOwnAdvisory(String userToCheck, Authentication credentials) {
        return userToCheck.equals(credentials.getName());
    }

    public static boolean isInStateDraft(WorkflowState stateToCheck) {
        return stateToCheck == WorkflowState.Draft;
    }

    public static boolean isInState(WorkflowState stateToCheck, WorkflowState ... allowedStates) {

        return Arrays.stream(allowedStates)
                .anyMatch(state -> stateToCheck == state);
    }

    public static Map<DbField, BiConsumer<AdvisoryInformationResponse, String>> adivoryReadFields() {

        return Map.of(
                AdvisoryField.WORKFLOW_STATE, AdvisoryInformationResponse::setWorkflowState,
                AdvisoryField.OWNER, AdvisoryInformationResponse::setOwner,
                AdvisorySearchField.DOCUMENT_TITLE, AdvisoryInformationResponse::setTitle,
                AdvisorySearchField.DOCUMENT_TRACKING_ID, AdvisoryInformationResponse::setDocumentTrackingId,
                CouchDbField.ID_FIELD, AdvisoryInformationResponse::setAdvisoryId
        );
    }

    public static AdvisoryInformationResponse getAdvisoryForId(String advisoryId, CouchDbService couchDbService) throws CsafException {

        Map<DbField, BiConsumer<AdvisoryInformationResponse, String>> infoFields = AdvisoryWorkflowUtil.adivoryReadFields();
        OperatorExpression typeIsAdvisory = equal(ObjectType.Advisory.name(), TYPE_FIELD.getDbName());
        OperatorExpression advisoryIdIsEqual = equal(advisoryId, ID_FIELD.getDbName());
        Map<String, Object> selector = expr2CouchDBFilter(new AndExpression(typeIsAdvisory, advisoryIdIsEqual));
        try {
            List<JsonNode> docList = findDocuments(couchDbService, selector, new ArrayList<>(infoFields.keySet()));
            List<AdvisoryInformationResponse> allResposes =  docList.stream()
                    .map(couchDbDoc -> AdvisoryWrapper.convertToAdvisoryInfo(couchDbDoc, infoFields))
                    .toList();
            if (allResposes.size() == 1) {
                return allResposes.get(0);
            } else {
                throw new CsafException("Advisory not found", CsafExceptionKey.AdvisoryNotFound);
            }
        } catch (IOException ex) {
            LOG.error("Could not create Advisory", ex);
            throw new CsafException(ex, CsafExceptionKey.AdvisoryNotFound, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * read from {@link CouchDbService#findDocumentsAsStream(Map, Collection)} and convert it to a list of JsonNode
     *
     * @param selector the selector to search for
     * @param fields   the fields of information to select
     * @return the result nodes of the search
     */
    public static List<JsonNode> findDocuments(CouchDbService couchDbService, Map<String, Object> selector, Collection<DbField> fields)
            throws IOException {

        InputStream inputStream = couchDbService.findDocumentsAsStream(selector, fields);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode couchDbResultNode = mapper.readValue(inputStream, JsonNode.class);
        ArrayNode couchDbDocs = (ArrayNode) couchDbResultNode.get("docs");
        List<JsonNode> docNodes = new ArrayList<>();
        couchDbDocs.forEach(docNodes::add);
        return docNodes;
    }

}
