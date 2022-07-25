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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import org.apache.commons.text.similarity.LevenshteinDetailedDistance;
import org.apache.commons.text.similarity.LevenshteinResults;
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
     * Check whether an advisory with the given user and state can be deleted with the given credentials
     * @param userToCheck the advisory user to check
     * @param advisoryState the advisory workflow state to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    static boolean canDeleteAdvisory(String userToCheck, WorkflowState advisoryState, Authentication credentials) {

        boolean canBeDeleted = false;
        if (hasRole(AUTHOR, credentials)) {
            canBeDeleted = isOwnAdvisory(userToCheck, credentials) && isInStateDraft(advisoryState);
        }
        if (hasRole(EDITOR, credentials)) {
            canBeDeleted = isInStateDraft(advisoryState);
        }
        if (hasRole(MANAGER, credentials)) {
            canBeDeleted = true;
        }
        return canBeDeleted;
    }

    /**
     * Check whether the given advisory info can be changed with the given credentials
     * @param response the advisory info to check
     * @param credentials the credentials for the check
     * @return true - info can be changed
     */
    public static boolean canChangeAdvisory(AdvisoryInformationResponse response, Authentication credentials) {

        return canChangeAdvisory(response.getOwner(), response.getWorkflowState(), credentials);
    }

    /**
     * Check whether the given advisory can be deleted with the given credentials
     * @param advisory the advisory to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    public static boolean canChangeAdvisory(AdvisoryWrapper advisory, Authentication credentials) {

        return canChangeAdvisory(advisory.getOwner(), advisory.getWorkflowState(), credentials);
    }

    /**
     * Check whether an advisory with the given user and state can be changed with the given credentials
     * @param userToCheck the advisory user to check
     * @param advisoryState the advisory workflow state to check
     * @param credentials the credentials for the check
     * @return true - info can be changed
     */
    static boolean canChangeAdvisory(String userToCheck, WorkflowState advisoryState, Authentication credentials) {

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
     * Check whether the given advisory can be viewed with the given credentials
     * @param advisory the advisory to check
     * @param credentials the credentials for the check
     * @return true - info can be deleted
     */
    public static boolean canViewAdvisory(AdvisoryWrapper advisory, Authentication credentials) {

        return canViewAdvisory(advisory.getOwner(), advisory.getWorkflowState(), credentials,
                advisory.getDocumentTrackingCurrentReleaseDate());
    }


    /**
     * Check whether the given advisory info can be viewed with the given credentials
     * @param response the advisory info to check
     * @param credentials the credentials for the check
     * @return true - info can be changed
     */
    public static boolean canViewAdvisory(AdvisoryInformationResponse response, Authentication credentials) {

        return canViewAdvisory(response.getOwner(), response.getWorkflowState(), credentials,
                response.getCurrentReleaseDate());
    }

    /**
     * Check whether an advisory with the given user and state can be viewed with the given credentials
     * @param userToCheck the advisory user to check
     * @param advisoryState the advisory workflow state to check
     * @param credentials the credentials for the check
     * @return true - info can be changed
     */
    static boolean canViewAdvisory(String userToCheck, WorkflowState advisoryState, Authentication credentials,
            String releaseDate) {

        boolean canBeViewed = isPublished(advisoryState, releaseDate);
        if (hasRole(AUTHOR, credentials)) {
            canBeViewed = isOwnAdvisory(userToCheck, credentials)
                    || isPublished(advisoryState, releaseDate);
        }
        if (hasRole(EDITOR, credentials)) {
            canBeViewed = true;
        }
        if (hasRole(PUBLISHER, credentials)) {
            canBeViewed = isInState(advisoryState, WorkflowState.Draft, WorkflowState.Approved,
                    WorkflowState.RfPublication) || isPublished(advisoryState, releaseDate);
        }
        if (hasRole(REVIEWER, credentials)) {
            canBeViewed |= (!isOwnAdvisory(userToCheck, credentials) && isInState(advisoryState, WorkflowState.Review))
                    || isPublished(advisoryState, releaseDate);
        }

        if (hasRole(AUDITOR, credentials)) {
            canBeViewed = true;
        }

        return canBeViewed;
    }

    /**
     * Check whether the workflow state of the given advisory can be changed with the given credentials
     * @param advisory the advisory to check
     * @param credentials the credentials for the check
     * @param newWorkflowState the state to change into
     * @return true - info can be deleted
     */
    public static boolean canChangeWorkflow(AdvisoryWrapper advisory, WorkflowState newWorkflowState, Authentication credentials) {

        return canChangeWorkflow(advisory.getOwner(), advisory.getWorkflowState(), newWorkflowState, credentials);
    }

    /**
     * Check whether the given advisory info can be viewed with the given credentials
     * @param response the advisory info to check
     * @param newWorkflowState the state to change into
     * @param credentials the credentials for the check
     * @return true - info can be changed
     */
    public static boolean canChangeWorkflow(AdvisoryInformationResponse response, WorkflowState newWorkflowState,
                                            Authentication credentials) {

        return canChangeWorkflow(response.getOwner(), response.getWorkflowState(), newWorkflowState, credentials);
    }

    /**
     * Check whether  the workflow state of an advisory with the given user and state can be changed
     * @param userToCheck the advisory user to check
     * @param oldWorkflowState the advisory workflow state to check
     * @param newWorkflowState the state to change into
     * @param credentials the credentials for the check
     * @return true - info can be changed
     */
    static boolean canChangeWorkflow(String userToCheck, WorkflowState oldWorkflowState,
                                     WorkflowState newWorkflowState, Authentication credentials) {

        boolean canBeChanged = false;
        if (oldWorkflowState == WorkflowState.Draft && newWorkflowState == WorkflowState.Review) {
            canBeChanged = hasRole(AUTHOR, credentials) && isOwnAdvisory(userToCheck, credentials)
                    ||  hasRole(EDITOR, credentials);
        }

        if (oldWorkflowState == WorkflowState.Review && newWorkflowState == WorkflowState.Draft) {
            canBeChanged = hasRole(REVIEWER, credentials);
        }

        if (oldWorkflowState == WorkflowState.Review && newWorkflowState == WorkflowState.Approved) {
            canBeChanged = hasRole(REVIEWER, credentials);
        }

        if (oldWorkflowState == WorkflowState.Approved && newWorkflowState == WorkflowState.RfPublication) {
            canBeChanged = hasRole(AUTHOR, credentials) && isOwnAdvisory(userToCheck, credentials)
                    ||  hasRole(EDITOR, credentials) || hasRole(PUBLISHER, credentials);
        }

        if (oldWorkflowState == WorkflowState.Approved && newWorkflowState == WorkflowState.Draft) {
            canBeChanged = hasRole(PUBLISHER, credentials);
        }

        if (oldWorkflowState == WorkflowState.RfPublication && newWorkflowState == WorkflowState.Published) {
            canBeChanged = hasRole(PUBLISHER, credentials);
        }

        return canBeChanged;
    }

    /**
     * Check whether a new version of the given advisory can be created
     * @param advisory the advisory to check
     * @return true - info can be deleted
     */
    public static boolean canCreateNewVersion(AdvisoryWrapper advisory) {

        return canCreateNewVersion(advisory.getWorkflowState());
    }

    /**
     * Check whether a new version of the given advisory can be created
     * @param advisory the advisory to check
     * @return true - info can be deleted
     */
    public static boolean canCreateNewVersion(AdvisoryInformationResponse advisory) {

        return canCreateNewVersion(advisory.getWorkflowState());
    }

    /**
     * Check whether a new version of the given advisory can be created
     * @param oldWorkflowState the advisory workflow state
     * @return true - info can be deleted
     */
    static boolean canCreateNewVersion(WorkflowState oldWorkflowState) {

        boolean canCreateNewVersion = false;
        if (oldWorkflowState == WorkflowState.Published) {
            canCreateNewVersion = true;
        }
        return canCreateNewVersion;
    }

    /**
         * Check whether comments or Answers can be added to the given advisory with the given credentials
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

    static boolean isPublished(WorkflowState advisoryState, String releaseDate) {
        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        return isInState(advisoryState, WorkflowState.Published)
                && (releaseDate != null && !releaseDate.isBlank()
                        && releaseDate.compareTo(now) < 0);
    }


    public static boolean isInStateDraft(WorkflowState stateToCheck) {
        return stateToCheck == WorkflowState.Draft;
    }

    public static boolean isInState(WorkflowState stateToCheck, WorkflowState ... allowedStates) {

        return Arrays.stream(allowedStates)
                .anyMatch(state -> stateToCheck == state);
    }

    public static Map<DbField, BiConsumer<AdvisoryInformationResponse, String>> advisoryReadFields() {

        return Map.of(
                AdvisoryField.WORKFLOW_STATE, AdvisoryInformationResponse::setWorkflowState,
                AdvisoryField.OWNER, AdvisoryInformationResponse::setOwner,
                AdvisorySearchField.DOCUMENT_TITLE, AdvisoryInformationResponse::setTitle,
                AdvisorySearchField.DOCUMENT_TRACKING_ID, AdvisoryInformationResponse::setDocumentTrackingId,
                CouchDbField.ID_FIELD, AdvisoryInformationResponse::setAdvisoryId,
                AdvisorySearchField.DOCUMENT_TRACKING_CURRENT_RELEASE_DATE, AdvisoryInformationResponse::setCurrentReleaseDate
        );
    }

    public static AdvisoryInformationResponse getAdvisoryForId(String advisoryId, CouchDbService couchDbService) throws CsafException {

        Map<DbField, BiConsumer<AdvisoryInformationResponse, String>> infoFields = AdvisoryWorkflowUtil.advisoryReadFields();
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

    public static PatchType getChangeType(AdvisoryWrapper oldAdvisoryNode, AdvisoryWrapper newAdvisory) {

        PatchType result = PatchType.PATCH;

        JsonNode oldCsaf = oldAdvisoryNode.getCsaf();
        JsonNode diffPatch = AdvisoryWrapper.calculateJsonDiff(oldCsaf, newAdvisory.getCsaf());
        String vulnerabRegEx = "/vulnerabilities/\\d+";
        String vulnerabFirstAffectedRegEx = "/vulnerabilities/\\d+/product_status/first_affected/\\d+";
        String vulnerabKnownAffectedRegEx = "/vulnerabilities/\\d+/product_status/known_affected/\\d+";
        String vulnerabLastAffectedRegEx = "/vulnerabilities/\\d+/product_status/last_affected/\\d+";

        for (JsonNode jsonNode : diffPatch) {

            String operation = jsonNode.get("op").asText();
            String path = jsonNode.get("path").asText();
            if (path.startsWith("/product_tree")) {
                result = PatchType.MAJOR;
                break;
            }
            if ("add".equals(operation) || "remove".equals(operation)) {
                if (path.matches(vulnerabRegEx) || path.matches(vulnerabFirstAffectedRegEx)
                    || path.matches(vulnerabKnownAffectedRegEx) || path.matches(vulnerabLastAffectedRegEx)) {
                    result = PatchType.MAJOR;
                    break;
                }
                result = PatchType.MINOR;
            }
            if ("replace".equals(operation)) {
                String value = jsonNode.get("value").asText();
                String oldValue = oldCsaf.at(path).asText();
                if (!isSpellingMistake(oldValue, value)) {
                    result = PatchType.MINOR;
                }
            }
        }

        return result;
    }

    public static boolean isSpellingMistake(String oldString, String newString) {

        LevenshteinResults distance = LevenshteinDetailedDistance.getDefaultInstance().apply(oldString, newString);

        if (newString.length() < 6) {
            return distance.getDistance() <= 2;
        } else if (newString.length() < 20) {
            return distance.getDistance() <= 4;
        } else {
            return distance.getDistance() <= 8;
        }
    }


}
