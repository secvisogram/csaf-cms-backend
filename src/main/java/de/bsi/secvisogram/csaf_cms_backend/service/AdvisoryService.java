package de.bsi.secvisogram.csaf_cms_backend.service;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.IdNotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.entity.*;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.json.*;
import de.bsi.secvisogram.csaf_cms_backend.model.*;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.AndExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.Expression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OrExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.TypeOfOperator;
import de.bsi.secvisogram.csaf_cms_backend.mustache.JavascriptExporter;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateCommentRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.*;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.AUDITOR;
import static de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey.*;
import static de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus.Final;
import static de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus.Interim;
import static de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryWorkflowUtil.*;
import static java.util.Collections.emptyList;
import static org.springframework.http.HttpStatus.*;

@Service
public class AdvisoryService {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryService.class);

    @Autowired
    private PostgresRepositoryService postgresService;

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

    @Value("${csaf.references.baseurl}")
    private String referencesBaseUrl;

    @Value("${csaf.trackingid.company}")
    private String trackingidCompany;

    @Value("${csaf.trackingid.digits}")
    private String trackingidDigits;

    @Value("${csaf.trackingid.assignment.phase}")
    private String trackingIdAssignmentPhaseValue;

    private TrackingIdAssignmentPhase trackingIdAssignmentPhase;

    @Autowired
    private CsafConfiguration configuration;

    @PostConstruct
    void validateTrackingIdAssignmentPhase() {
        try {
            this.trackingIdAssignmentPhase = TrackingIdAssignmentPhase.valueOf(
                    this.trackingIdAssignmentPhaseValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            LOG.error("Invalid value '{}' for property csaf.trackingid.assignment.phase. "
                            + "Allowed values are: {}. Falling back to default value '{}'.",
                    this.trackingIdAssignmentPhaseValue, Arrays.toString(TrackingIdAssignmentPhase.values()),
                    TrackingIdAssignmentPhase.RELEASE);
            this.trackingIdAssignmentPhase = TrackingIdAssignmentPhase.RELEASE;
        }
    }

    /**
     * get the tracking id assignment phase
     *
     * @return the tracking id assignment phase
     */
    public TrackingIdAssignmentPhase getTrackingIdAssignmentPhase() {
        return this.trackingIdAssignmentPhase;
    }

    @Autowired
    private BuildProperties buildProperties;

    /**
     * Get the total number of records across all tables (advisories, comments, audit trails, counters).
     * Used primarily by integration tests to verify correct creation/deletion of records.
     *
     * @return total count of all entities in the DB
     */
    public long getDocumentCount() {
        return postgresService.getTotalDocumentCount();
    }

    /**
     * get information on all advisories
     *
     * @return a list of information objects
     */
    @Secured({CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR})
    public List<AdvisoryInformationResponse> getAdvisoryInformations(String expression)
            throws IOException, CsafException {

        Authentication credentials = getAuthentication();
        Expression parsedExpr = parseFilterExpression(expression);

        Expression visibilityExpr = AdvisoryWorkflowUtil.buildVisibilityExpression(credentials);
        List<AdvisoryInformationResponse> allAdvisories =
                readAllAdvisories(parsedExpr, ObjectType.Advisory, visibilityExpr, credentials);
        for (AdvisoryInformationResponse response : allAdvisories) {
            enrichAdvisory(response, credentials);
        }
        List<AdvisoryInformationResponse> allResponses = new ArrayList<>(allAdvisories);

        if (hasRole(AUDITOR, credentials)) {
            List<AdvisoryInformationResponse> allAdvisoryVersions =
                    readAllAdvisoryVersions(parsedExpr, credentials);
            for (AdvisoryInformationResponse response : allAdvisoryVersions) {
                enrichAdvisoryVersion(response);
            }
            allResponses.addAll(allAdvisoryVersions);
        }
        return allResponses;
    }

    private Expression parseFilterExpression(String expression) throws CsafException {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            return AdvisorySearchUtil.json2Expression(expression);
        } catch (Exception e) {
            throw new CsafException("Invalid filter expression", InvalidFilterExpression, BAD_REQUEST);
        }
    }

    private List<AdvisoryInformationResponse> readAllAdvisories(
            Expression parsedExpr,
            ObjectType objectType,
            Expression visibilityExpr,
            Authentication credentials) {

        List<AdvisoryEntity> entities = postgresService.findAllAdvisories();
        return entities.stream()
                .filter(entity -> matchesCsafExpression(entity.getCsaf(), parsedExpr))
                .map(EntityConverter::toAdvisoryInfo)
                .filter(info -> matchesVisibility(info, visibilityExpr, credentials))
                .toList();
    }

    private List<AdvisoryInformationResponse> readAllAdvisoryVersions(
            Expression parsedExpr,
            Authentication credentials) {

        // Advisory versions are immutable snapshots in the advisory_versions table,
        // created each time a new version cycle begins via createNewCsafDocumentVersion.
        return postgresService.findAllAdvisoryVersions().stream()
                .filter(entity -> matchesCsafExpression(entity.getCsaf(), parsedExpr))
                .map(EntityConverter::toAdvisoryVersionInfo)
                .toList();
    }

    /**
     * Check whether an advisory info item passes the visibility expression filter.
     * When visibilityExpr is null the caller can see everything.
     */
    private boolean matchesVisibility(AdvisoryInformationResponse info,
            Expression visibilityExpr,
            Authentication credentials) {

        if (visibilityExpr == null) {
            return true;
        }
        // Re-use the canViewAdvisory logic which already encodes all visibility rules
        return AdvisoryWorkflowUtil.canViewAdvisory(
                info.getOwner(),
                info.getWorkflowState(),
                credentials,
                info.getCurrentReleaseDate()
        );
    }

    /**
     * Check whether the entity's full CSAF JSON matches an already-parsed filter expression.
     * This enables filtering by arbitrary deep paths (acknowledgments, vulnerabilities,
     * product_tree, etc.) that are not available in the flattened {@link AdvisoryInformationResponse}.
     *
     * <p>The expression is parsed once, up front, by {@link #parseFilterExpression} rather than
     * per entity here: a malformed expression must be rejected with a proper error, and a
     * checked {@link CsafException} cannot be thrown from inside a stream {@code Predicate}.</p>
     *
     * @param csafJson   the entity's CSAF JSON ({@code JsonNode})
     * @param parsedExpr the already-parsed expression, or {@code null} when no filter was supplied
     * @return {@code true} if the CSAF JSON matches the expression or no expression was supplied
     */
    private boolean matchesCsafExpression(JsonNode csafJson, Expression parsedExpr) {

        if (parsedExpr == null) {
            return true;
        }
        return evaluateCsafExpression(parsedExpr, csafJson);
    }

    /**
     * Recursively evaluate an {@link Expression} against the entity's CSAF JSON.
     */
    private boolean evaluateCsafExpression(Expression expr, JsonNode csafJson) {

        if (expr instanceof OperatorExpression opExpr) {
            return evaluateCsafOperator(opExpr, csafJson);
        }
        if (expr instanceof AndExpression andExpr) {
            for (Expression child : andExpr.getExpressions()) {
                if (!evaluateCsafExpression(child, csafJson)) {
                    return false;
                }
            }
            return true;
        }
        if (expr instanceof OrExpression orExpr) {
            for (Expression child : orExpr.getExpressions()) {
                if (evaluateCsafExpression(child, csafJson)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Evaluate a single operator expression against the CSAF JSON.
     * The selector path starts with "csaf" (e.g. ["csaf","document","title"]);
     * the "csaf" prefix is stripped and the remainder is used to navigate the JSON tree.
     * Array nodes are traversed recursively so that a match in any element qualifies.
     */
    private boolean evaluateCsafOperator(
            OperatorExpression opExpr,
            JsonNode csafJson) {

        String[] path = opExpr.getSelector();
        String filterValue = opExpr.getValue();

        // Strip "csaf" prefix; remaining segments address the CSAF JSON
        int startIdx = (path.length > 0 && "csaf".equals(path[0])) ? 1 : 0;
        String[] csafPath = Arrays.copyOfRange(path, startIdx, path.length);

        List<String> values = collectJsonValues(csafJson, csafPath, 0);
        if (values.isEmpty()) {
            // Path not found → cannot evaluate; pass through
            return true;
        }

        return values.stream().anyMatch(value -> matchesOperator(opExpr.getOperatorType(), value, filterValue));
    }

    /**
     * Collect all text values reachable by following the given path segments through the JSON tree.
     * When an array node is encountered at any level, the remaining path is applied to each element.
     */
    private List<String> collectJsonValues(JsonNode node, String[] pathSegments, int segmentIndex) {

        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (segmentIndex >= pathSegments.length) {
            // Reached the target depth — collect value(s)
            if (node.isArray()) {
                List<String> result = new ArrayList<>();
                for (JsonNode element : node) {
                    if (element.isValueNode()) {
                        result.add(element.asText());
                    }
                }
                return result;
            }
            if (node.isValueNode()) {
                return List.of(node.asText());
            }
            return List.of();
        }

        String segment = pathSegments[segmentIndex];
        if (node.isArray()) {
            // Apply current segment to each array element
            List<String> result = new ArrayList<>();
            for (JsonNode element : node) {
                result.addAll(collectJsonValues(element, pathSegments, segmentIndex));
            }
            return result;
        }
        // Navigate into the child
        JsonNode child = node.get(segment);
        return collectJsonValues(child, pathSegments, segmentIndex + 1);
    }

    /**
     * Check whether a single string value matches the filter using the given operator.
     */
    private boolean matchesOperator(
            TypeOfOperator operatorType,
            String fieldValue, String filterValue) {

        return switch (operatorType) {
            case Equal -> fieldValue.equals(filterValue);
            case NotEqual -> !fieldValue.equals(filterValue);
            case ContainsIgnoreCase -> fieldValue.toLowerCase().contains(filterValue.toLowerCase());
            case Greater -> fieldValue.compareTo(filterValue) > 0;
            case GreaterOrEqual -> fieldValue.compareTo(filterValue) >= 0;
            case Less -> fieldValue.compareTo(filterValue) < 0;
            case LessOrEqual -> fieldValue.compareTo(filterValue) <= 0;
        };
    }

    private void enrichAdvisory(AdvisoryInformationResponse response, Authentication credentials) {
        response.setDeletable(canDeleteAdvisory(response, credentials));
        response.setChangeable(canChangeAdvisory(response, credentials));
        response.setAllowedStateChanges(getAllowedStates(response, credentials));
        response.setCanCreateVersion(canCreateNewVersion(response, credentials));
    }

    private void enrichAdvisoryVersion(AdvisoryInformationResponse response) {
        response.setDeletable(false);
        response.setChangeable(false);
        response.setAllowedStateChanges(emptyList());
        response.setCanCreateVersion(false);
    }

    private List<WorkflowState> getAllowedStates(AdvisoryInformationResponse response,
            Authentication credentials) {

        final var allowOwnDocumentsApproved = configuration.getWorkflow().isAllowOwnDocumentsApproved();
        return Arrays.stream(WorkflowState.values())
                .filter(state -> AdvisoryWorkflowUtil.canChangeWorkflow(
                        response, state, credentials, allowOwnDocumentsApproved))
                .collect(Collectors.toList());
    }

    /**
     * Adds an advisory to the system
     *
     * @param newCsafJson the advisory as JSON String
     * @return a tuple of assigned id as UUID and the current revision for concurrent control
     */
    @Secured({CsafRoles.ROLE_AUTHOR})
    public IdAndRevision addAdvisory(CreateAdvisoryRequest newCsafJson) throws IOException, CsafException {

        LOG.debug("addAdvisory");
        Authentication credentials = getAuthentication();
        return addAdvisoryForCredentials(newCsafJson, credentials);
    }

    @Transactional
    IdAndRevision addAdvisoryForCredentials(CreateAdvisoryRequest newCsafJson, Authentication credentials)
            throws IOException, CsafException {

        if (newCsafJson.getSummary() == null || newCsafJson.getSummary().isBlank()) {
            throw new CsafException("Summary must not be empty", SummaryInHistoryEmpty, BAD_REQUEST);
        }

        AdvisoryWrapper emptyAdvisory = AdvisoryWrapper.createInitialEmptyAdvisoryForUser(credentials.getName());
        AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.createNewFromCsaf(newCsafJson, credentials.getName(),
                this.versioningStrategy);
        newAdvisoryNode.setDocumentTrackingGeneratorEngineName(buildProperties.getName());
        newAdvisoryNode.setDocumentTrackingGeneratorEngineVersion(buildProperties.getVersion());

        newAdvisoryNode.removeAllRevisionHistoryElements();
        String timestampNow = getCurrentTimestamp();
        newAdvisoryNode.addRevisionHistoryElement(newCsafJson, timestampNow);
        if (newAdvisoryNode.currentReleaseDateIsNotSetOrInPast(timestampNow)) {
            newAdvisoryNode.setDocumentTrackingCurrentReleaseDate(timestampNow);
        }

        if (this.trackingIdAssignmentPhase == TrackingIdAssignmentPhase.DRAFT) {
            setFinalTrackingIdAndUrl(newAdvisoryNode);
        } else {
            addTemporaryTrackingId(newAdvisoryNode);
        }

        // Persist advisory (ID generated by @GeneratedValue)
        AdvisoryEntity entity = EntityConverter.toEntity(newAdvisoryNode, null);
        AdvisoryEntity saved = postgresService.saveAdvisory(entity);

        // Persist audit trail (document diff)
        AdvisoryAuditTrailDiffWrapper diffWrapper =
                AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(emptyAdvisory, newAdvisoryNode);
        AuditTrailDocumentEntity auditEntity = EntityConverter.toAuditTrailDocumentEntity(
                saved,
                credentials.getName(),
                ChangeType.Create,
                diffWrapper.getDiffPatch(),
                diffWrapper.getOldDocVersion(),
                diffWrapper.getDocVersion()
        );
        postgresService.saveAuditTrailDocument(auditEntity);

        return new IdAndRevision(saved.getId().toString(), String.valueOf(saved.getVersion()));
    }

    /**
     * Import an advisory to the system for an authenticated user
     *
     * @param newCsafJson the advisory as JSON
     * @return a tuple of assigned id as UUID and the current revision for concurrent control
     */
    @Secured({CsafRoles.ROLE_PUBLISHER})
    public IdAndRevision importAdvisory(JsonNode newCsafJson) throws IOException, CsafException {

        LOG.debug("importAdvisory");
        Authentication credentials = getAuthentication();
        return importAdvisoryForCredentials(newCsafJson, credentials);
    }

    IdAndRevision importAdvisoryForCredentials(JsonNode nodeToImport, Authentication credentials)
            throws IOException, CsafException {

        return importAdvisoryForUser(nodeToImport, credentials.getName());
    }

    /**
     * Import an advisory to the system for a system user.
     * Should only be used for imports on application startup.
     *
     * @param nodeToImport the advisory as JSON
     * @return a tuple of ID and revision of the imported advisory
     * @throws IOException   when there are errors reading a file
     * @throws CsafException when there are errors processing the advisory
     */
    public IdAndRevision importAdvisoryForSystem(JsonNode nodeToImport) throws IOException, CsafException {
        return importAdvisoryForUser(nodeToImport, "_SYSTEM_IMPORT_");
    }

    @Transactional
    IdAndRevision importAdvisoryForUser(JsonNode nodeToImport, String userName) throws IOException, CsafException {

        if (!ValidatorServiceClient.isCsafValid(this.validationBaseUrl, nodeToImport)) {
            throw new CsafException("Advisory is no valid CSAF document",
                    AdvisoryValidationError, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        AdvisoryWrapper emptyAdvisory = AdvisoryWrapper.createInitialEmptyAdvisoryForUser(userName);
        AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.importNewFromCsaf(nodeToImport, userName);

        String documentTrackingStatus = newAdvisoryNode.getDocumentTrackingStatus();
        if (!documentTrackingStatus.equals(Interim.getCsafValue())
                && !documentTrackingStatus.equals(Final.getCsafValue())) {
            throw new CsafException("Advisory is not in state final or interim",
                    AdvisoryValidationError, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Check for duplicate tracking ID
        String trackingId = newAdvisoryNode.getDocumentTrackingId();
        if (postgresService.advisoryExistsByTrackingId(trackingId)) {
            throw new CsafException("Trying to import a duplicate advisory (identical tracking ID)",
                    DuplicateImport, UNPROCESSABLE_ENTITY);
        }

        // Persist advisory (ID generated by @GeneratedValue)
        AdvisoryEntity entity = EntityConverter.toEntity(newAdvisoryNode, null);
        AdvisoryEntity saved;
        try {
            saved = postgresService.saveAdvisory(entity);
        } catch (DataIntegrityViolationException e) {
            // in case of race condition the unique index on tracking-id would be the one avoiding inserting
            // CSAF doc with duplicated tracking-id
            throw new CsafException("Trying to import a duplicate advisory (identical tracking ID)",
                    DuplicateImport, UNPROCESSABLE_ENTITY);
        }

        // Persist audit trail
        AdvisoryAuditTrailDiffWrapper diffWrapper =
                AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(emptyAdvisory, newAdvisoryNode);
        AuditTrailDocumentEntity auditEntity = EntityConverter.toAuditTrailDocumentEntity(
                saved,
                userName,
                ChangeType.Create,
                diffWrapper.getDiffPatch(),
                diffWrapper.getOldDocVersion(),
                diffWrapper.getDocVersion()
        );
        postgresService.saveAuditTrailDocument(auditEntity);

        return new IdAndRevision(saved.getId().toString(), String.valueOf(saved.getVersion()));
    }

    /**
     * Insert a temporary tracking id in the advisory
     *
     * @param newAdvisoryNode node to set the id
     * @throws CsafException error creating counter
     */
    void addTemporaryTrackingId(AdvisoryWrapper newAdvisoryNode) throws CsafException {

        long sequentialNumber = getNewTrackingIdCounter(TrackingIdCounter.TMP_OBJECT_ID);
        newAdvisoryNode.setTemporaryTrackingId(this.trackingidCompany, this.trackingidDigits, sequentialNumber);
    }

    /**
     * Get the next unique tracking id from the db for the given counterId.
     *
     * @param counterId id of the counter
     * @return next id
     * @throws CsafException error creating counter
     */
    long getNewTrackingIdCounter(String counterId) throws CsafException {

        try {
            return postgresService.incrementAndGetCounter(counterId);
        } catch (Exception ex) {
            throw new CsafException("Error create new counter for tracking Id",
                    ErrorCreatingTrackingIdCounter, INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * get a specific advisory
     *
     * @param advisoryId the ID of the advisory to get
     * @return the requested advisory
     * @throws IdNotFoundException if there is no advisory with given ID
     */
    public AdvisoryResponse getAdvisory(String advisoryId) throws DatabaseException, CsafException {

        try {
            AdvisoryEntity entity = findAdvisoryEntityOrThrow(advisoryId);
            AdvisoryWrapper advisory = EntityConverter.toWrapper(entity);

            if (canViewAdvisory(advisory, getAuthentication())) {
                Authentication credentials = getAuthentication();
                boolean isVersion = advisory.getType() == ObjectType.AdvisoryVersion;

                AdvisoryResponse response = new AdvisoryResponse(advisoryId, advisory.getWorkflowState(),
                        advisory.getCsaf());
                response.setTitle(advisory.getDocumentTitle());
                response.setCurrentReleaseDate(advisory.getDocumentTrackingCurrentReleaseDate());
                response.setDocumentTrackingId(advisory.getDocumentTrackingId());
                response.setOwner(advisory.getOwner());
                response.setDeletable(!isVersion && canDeleteAdvisory(response, credentials));
                response.setChangeable(!isVersion && canChangeAdvisory(response, credentials));
                response.setCanCreateVersion(!isVersion && canCreateNewVersion(response, credentials));
                List<WorkflowState> allowedStateChanges =
                        (!isVersion) ? getAllowedStates(response, credentials) : emptyList();
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
     * Deletes an advisory with given id from the database.
     * Comments and audit trails are removed via ON DELETE CASCADE in the schema.
     *
     * @param advisoryId the ID of the advisory to delete
     * @param revision   the revision for concurrent control
     */
    @Secured({CsafRoles.ROLE_AUTHOR})
    @Transactional
    public void deleteAdvisory(String advisoryId, String revision) throws DatabaseException, IOException, CsafException {

        LOG.debug("deleteAdvisory");
        AdvisoryEntity entity = findAdvisoryEntityOrThrow(advisoryId);
        AdvisoryWrapper advisory = EntityConverter.toWrapper(entity);
        if (canDeleteAdvisory(advisory, getAuthentication())) {
            checkAdvisoryRevision(entity, revision);
            postgresService.deleteAdvisory(entity);
        } else {
            throw new AccessDeniedException("User has not the permission to delete the advisory");
        }
    }

    /**
     * @param advisoryId      the ID of the advisory to update
     * @param revision        the revision for concurrent control
     * @param changedCsafJson the updated csaf json
     * @return the new revision of the updated csaf document
     * @throws DatabaseException if there was an error updating the advisory in the DB
     */
    @Transactional
    public String updateAdvisory(String advisoryId, String revision, CreateAdvisoryRequest changedCsafJson)
            throws IOException, DatabaseException, CsafException {

        LOG.debug("updateAdvisory");
        AdvisoryEntity existingEntity = findAdvisoryEntityOrThrow(advisoryId);
        AdvisoryWrapper oldAdvisoryNode = EntityConverter.toWrapper(existingEntity);

        Authentication credentials = getAuthentication();
        if (canChangeAdvisory(oldAdvisoryNode, credentials)) {
            checkAdvisoryRevision(existingEntity, revision);

            if (changedCsafJson.getSummary() == null || changedCsafJson.getSummary().isBlank()) {
                throw new CsafException("Summary must not be empty", SummaryInHistoryEmpty, UNPROCESSABLE_ENTITY);
            }

            AdvisoryWrapper newAdvisoryNode = AdvisoryWrapper.updateFromExisting(oldAdvisoryNode, changedCsafJson);
            newAdvisoryNode.setRevision(revision);
            newAdvisoryNode.setDocumentTrackingGeneratorEngineName(buildProperties.getName());
            newAdvisoryNode.setDocumentTrackingGeneratorEngineVersion(buildProperties.getVersion());
            PatchType changeType = AdvisoryWorkflowUtil.getChangeType(oldAdvisoryNode, newAdvisoryNode,
                    configuration.getVersioning().getLevenshtein());
            String nextVersion = oldAdvisoryNode.getVersioningStrategy().getNextVersion(changeType,
                    oldAdvisoryNode.getDocumentTrackingVersion(), oldAdvisoryNode.getLastVersion());
            newAdvisoryNode.setDocumentTrackingVersion(nextVersion);
            String timestampNow = getCurrentTimestamp();
            if (newAdvisoryNode.currentReleaseDateIsNotSetOrInPast(timestampNow)) {
                newAdvisoryNode.setDocumentTrackingCurrentReleaseDate(timestampNow);
            }
            if (oldAdvisoryNode.usesSemanticVersioning()
                    && newAdvisoryNode.versionIsUntilIncludingInitialPublication()
                    && !oldAdvisoryNode.getDocumentTrackingVersion().equals(nextVersion)) {
                newAdvisoryNode.addRevisionHistoryElement(changedCsafJson, timestampNow);
            } else {
                newAdvisoryNode.editLastRevisionHistoryElement(changedCsafJson, timestampNow);
            }

            AdvisoryEntity updated = EntityConverter.toEntity(newAdvisoryNode, existingEntity);
            AdvisoryEntity saved = postgresService.saveAdvisory(updated);

            // Persist audit trail
            AdvisoryAuditTrailDiffWrapper diffWrapper =
                    AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(oldAdvisoryNode, newAdvisoryNode);
            AuditTrailDocumentEntity auditEntity = EntityConverter.toAuditTrailDocumentEntity(
                    saved,
                    credentials.getName(),
                    ChangeType.Update,
                    diffWrapper.getDiffPatch(),
                    diffWrapper.getOldDocVersion(),
                    diffWrapper.getDocVersion()
            );
            postgresService.saveAuditTrailDocument(auditEntity);

            return String.valueOf(saved.getVersion());
        } else {
            throw new CsafException("User has no permission to edit the advisory", NoPermissionForAdvisory, UNAUTHORIZED);
        }
    }

    /**
     * Manually assign the final tracking id for an advisory, if none has been assigned yet.
     *
     * @param advisoryId the ID of the advisory to assign the tracking id for
     * @param revision   the revision for concurrent control
     * @return the new revision of the updated csaf document
     * @throws IOException       if there was an error reading the advisory from the DB
     * @throws DatabaseException if the advisory with the given id does not exist
     * @throws CsafException     if the user has no permission to edit the advisory or if the advisory already has a final tracking id assigned
     */
    @Transactional
    public String assignTrackingId(String advisoryId, String revision) throws IOException, DatabaseException, CsafException {

        LOG.debug("assignTrackingId");
        AdvisoryEntity existingEntity = findAdvisoryEntityOrThrow(advisoryId);
        AdvisoryWrapper existingAdvisoryNode = EntityConverter.toWrapper(existingEntity);
        Authentication credentials = getAuthentication();
        if (!canChangeAdvisory(existingAdvisoryNode, credentials)) {
            throw new CsafException("User has no permission to edit the advisory", NoPermissionForAdvisory, UNAUTHORIZED);
        }

        if (existingAdvisoryNode.isFinalTrackingIdAssigned()) {
            throw new CsafException("Advisory already has a final tracking id assigned",
                    TrackingIdAlreadyAssigned, CONFLICT);
        }

        checkAdvisoryRevision(existingEntity, revision);

        AdvisoryWrapper oldAdvisoryNode = AdvisoryWrapper.createCopy(existingAdvisoryNode);
        setFinalTrackingIdAndUrl(existingAdvisoryNode);
        existingAdvisoryNode.setRevision(revision);

        AdvisoryEntity updated = EntityConverter.toEntity(existingAdvisoryNode, existingEntity);
        AdvisoryEntity saved = postgresService.saveAdvisory(updated);

        AdvisoryAuditTrailDiffWrapper diffWrapper =
                AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(oldAdvisoryNode, existingAdvisoryNode);
        AuditTrailDocumentEntity auditEntity = EntityConverter.toAuditTrailDocumentEntity(
                saved,
                credentials.getName(),
                ChangeType.Update,
                diffWrapper.getDiffPatch(),
                diffWrapper.getOldDocVersion(),
                diffWrapper.getDocVersion()
        );
        postgresService.saveAuditTrailDocument(auditEntity);

        return String.valueOf(saved.getVersion());
    }

    /**
     * Export the Advisory with the given advisoryId in the given format.
     *
     * @param advisoryId the id of the advisory that should be exported
     * @param format     the format in which the export should be written (default JSON on null)
     * @return the path to the temporary file that contains the export
     */
    @Secured({CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR})
    public Path exportAdvisory(
            @Nonnull final String advisoryId,
            @Nullable final ExportFormat format)
            throws IOException, CsafException {

        try {
            AdvisoryEntity entity = findAdvisoryEntityOrThrow(advisoryId);
            AdvisoryWrapper advisoryNode = EntityConverter.toWrapper(entity);
            final JsonNode csaf = advisoryNode.getCsaf();
            RemoveIdHelper.removeCommentIds(csaf);
            final String csafDocument = csaf.toString();

            final String filename = advisoryNode.getDocumentTrackingId() == null ? "advisory__" : advisoryNode.getDocumentTrackingId();

            // if format is JSON - write it to temporary file and return the path
            if (format == ExportFormat.JSON || format == null) {
                final Path jsonFile = Files.createTempFile(filename, ".json");
                Files.writeString(jsonFile, csafDocument);
                return jsonFile;
            } else {
                final String htmlExport = javascriptExporter.createHtml(csafDocument);
                final Path htmlFile = Files.createTempFile(filename, ".html");
                Files.writeString(htmlFile, htmlExport);
                if (format == ExportFormat.HTML) {
                    return htmlFile;
                } else if (format == ExportFormat.Markdown && pandocService.isReady()) {
                    final Path markdownFile = Files.createTempFile(filename, ".md");
                    pandocService.convert(htmlFile, markdownFile);
                    Files.delete(htmlFile);
                    return markdownFile;
                } else if (format == ExportFormat.PDF && weasyprintService.isReady()) {
                    final Path pdfFile = Files.createTempFile(filename, ".pdf");
                    weasyprintService.convert(htmlFile, pdfFile);
                    Files.delete(htmlFile);
                    return pdfFile;
                }
                throw new CsafException("Unknown export format: " + format,
                        CsafExceptionKey.UnknownExportFormat, BAD_REQUEST);
            }
        } catch (IdNotFoundException e) {
            throw new CsafException("Can not find advisory with ID " + advisoryId,
                    CsafExceptionKey.AdvisoryNotFound, NOT_FOUND);
        } catch (DatabaseException e) {
            throw new CsafException("Database error reading advisory " + advisoryId,
                    CsafExceptionKey.AdvisoryNotFound, NOT_FOUND);
        }
    }

    /**
     * Export the Advisory with the given advisoryId and perform release activities on the document
     * The export will be written to a temporary file and the path to the file will be returned.
     *
     * @param advisoryId the id of the advisory that should be exported
     * @return the path to the temporary file that contains the export
     * @throws CsafException        if the advisory with the given id does not exist or the export format is unknown
     * @throws IOException          on any error regarding writing/reading from disk
     * @throws InterruptedException if the export did take too long and thus timed out
     */
    @Secured({CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR})
    public Path exportAdvisoryForAutoPublish(
            @Nonnull final String advisoryId)
            throws IOException, CsafException {
        // read the advisory form the database
        try {
            final JsonNode csaf = this.postgresService.readDocumentAsJsonNode(advisoryId);

            RemoveIdHelper.removeCommentIds(csaf);
            final String csafDocument = csaf.toString();

            // if format is JSON - write it to temporary file and return the path
            final Path jsonFile = Files.createTempFile(advisoryId, ".json");
            Files.writeString(jsonFile, csafDocument);
            return jsonFile;
        } catch (IdNotFoundException e) {
            throw new CsafException("Can not find advisory with ID " + advisoryId,
                    CsafExceptionKey.AdvisoryNotFound, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Changes the workflow state of the advisory to the given new WorkflowState.
     *
     * @param advisoryId              the ID of the advisory to update the workflow state of
     * @param revision                the revision for concurrent control
     * @param newWorkflowState        the new workflow state to set
     * @param proposedTime            optional proposed publication time
     * @param documentTrackingStatus  optional new document tracking status
     * @return the new revision of the updated csaf document
     */
    @Transactional
    public String changeAdvisoryWorkflowState(String advisoryId, String revision,
            WorkflowState newWorkflowState, String proposedTime,
            DocumentTrackingStatus documentTrackingStatus)
            throws IOException, DatabaseException, CsafException {

        Authentication credentials = getAuthentication();
        AdvisoryEntity existingEntity = findAdvisoryEntityOrThrow(advisoryId);
        AdvisoryWrapper existingAdvisoryNode = EntityConverter.toWrapper(existingEntity);

        final var allowOwnDocumentsApproved = configuration.getWorkflow().isAllowOwnDocumentsApproved();
        if (canChangeWorkflow(existingAdvisoryNode, newWorkflowState, credentials, allowOwnDocumentsApproved)) {
            checkAdvisoryRevision(existingEntity, revision);

            WorkflowState previousWorkflowState = existingAdvisoryNode.getWorkflowState();
            String previousVersion = existingAdvisoryNode.getDocumentTrackingVersion();
            String workflowStateChangeMsg = "Status changed from " + previousWorkflowState
                    + " to " + newWorkflowState;

            existingAdvisoryNode.setWorkflowState(newWorkflowState);
            if (documentTrackingStatus != null) {
                existingAdvisoryNode.setDocumentTrackingStatus(documentTrackingStatus);
            }

            if (newWorkflowState == WorkflowState.Approved) {
                String nextVersion = existingAdvisoryNode.getVersioningStrategy()
                        .getNextApprovedVersion(existingAdvisoryNode.getDocumentTrackingVersion());
                existingAdvisoryNode.setDocumentTrackingVersion(nextVersion);
                String timestampNow = getCurrentTimestamp();
                if (existingAdvisoryNode.currentReleaseDateIsNotSetOrInPast(timestampNow)) {
                    existingAdvisoryNode.setDocumentTrackingCurrentReleaseDate(timestampNow);
                }
                if (existingAdvisoryNode.usesSemanticVersioning()
                        && existingAdvisoryNode.versionIsUntilIncludingInitialPublication()) {
                    existingAdvisoryNode.addRevisionHistoryElement(workflowStateChangeMsg, "", timestampNow);
                } else if (existingAdvisoryNode.usesIntegerVersioning() && "0".equals(previousVersion)) {
                    String lastRevSummary = existingAdvisoryNode.getLastRevisionHistoryElementSummary();
                    existingAdvisoryNode.addRevisionHistoryElement(lastRevSummary, "", timestampNow);
                } else {
                    existingAdvisoryNode.setLastRevisionHistoryElementNumberAndDate(nextVersion, timestampNow);
                }
            }

            if (newWorkflowState == WorkflowState.Draft) {
                String nextVersion = existingAdvisoryNode.getVersioningStrategy()
                        .getNextDraftVersion(existingAdvisoryNode.getDocumentTrackingVersion());
                existingAdvisoryNode.setDocumentTrackingVersion(nextVersion);
                String timestampNow = getCurrentTimestamp();
                if (existingAdvisoryNode.currentReleaseDateIsNotSetOrInPast(timestampNow)) {
                    existingAdvisoryNode.setDocumentTrackingCurrentReleaseDate(timestampNow);
                }
                if (existingAdvisoryNode.usesSemanticVersioning()
                        && existingAdvisoryNode.versionIsUntilIncludingInitialPublication()) {
                    existingAdvisoryNode.addRevisionHistoryElement(workflowStateChangeMsg, "", timestampNow);
                } else {
                    existingAdvisoryNode.setLastRevisionHistoryElementNumberAndDate(nextVersion, timestampNow);
                }
            }

            if (newWorkflowState == WorkflowState.Review
                    && this.trackingIdAssignmentPhase == TrackingIdAssignmentPhase.REVIEW) {
                setFinalTrackingIdAndUrl(existingAdvisoryNode);
            }

            if (newWorkflowState == WorkflowState.RfPublication) {
                createReleaseReadyAdvisoryAndValidate(existingAdvisoryNode, proposedTime);
            }

            if (newWorkflowState == WorkflowState.AutoPublish) {
                if (proposedTime == null) {
                	proposedTime = existingAdvisoryNode.getDocumentTrackingCurrentReleaseDate();
                	if (proposedTime == null) {
                		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000000000Z");
                		proposedTime = sdf.format(new Date());
                	}
                }

                if (documentTrackingStatus == null) {
                	existingAdvisoryNode.setDocumentTrackingStatus(DocumentTrackingStatus.Final);
                }

                if (existingAdvisoryNode.getDocumentDistributionTlp() == null) {
                	throw new CsafException("TLP-Level missing", CsafExceptionKey.AdvisoryValidationError, BAD_REQUEST);
                }
                //TODO: Check, if further checks for upload are needed

                existingAdvisoryNode = createReleaseReadyAdvisoryAndValidate(existingAdvisoryNode, proposedTime);
                if (existingAdvisoryNode.getLastMajorVersion() < 1) {
                    setFinalTrackingIdAndUrl(existingAdvisoryNode);
                }
            }

            if (newWorkflowState == WorkflowState.Published && (previousWorkflowState != WorkflowState.AutoPublish)) {

                existingAdvisoryNode = createReleaseReadyAdvisoryAndValidate(existingAdvisoryNode, proposedTime);
                if (existingAdvisoryNode.getLastMajorVersion() < 1) {
                    setFinalTrackingIdAndUrl(existingAdvisoryNode);
                }
            }

            // Persist workflow audit trail
            AuditTrailWorkflowEntity workflowAudit = EntityConverter.toAuditTrailWorkflowEntity(
                    existingEntity,
                    credentials.getName(),
                    previousWorkflowState.name(),
                    newWorkflowState.name(),
                    previousVersion,
                    existingAdvisoryNode.getDocumentTrackingVersion()
            );
            postgresService.saveAuditTrailWorkflow(workflowAudit);

            existingAdvisoryNode.setRevision(revision);
            AdvisoryEntity updated = EntityConverter.toEntity(existingAdvisoryNode, existingEntity);
            AdvisoryEntity saved = postgresService.saveAdvisory(updated);
            return String.valueOf(saved.getVersion());
        } else {
            throw new CsafException(
                    "User has not the permission to change the workflow state of the advisory",
                    NoPermissionForAdvisory, UNAUTHORIZED);
        }
    }

    /**
     * Set the final tracking id in the advisory and a DocumentReferencesNode with the url.
     *
     * @param advisoryNode the node to set the tracking id
     * @throws CsafException error creating counter
     */
    void setFinalTrackingIdAndUrl(AdvisoryWrapper advisoryNode) throws CsafException {

        if (advisoryNode.isFinalTrackingIdAssigned()) {
            return;
        }

        final long sequentialNumber = getNewTrackingIdCounter(TrackingIdCounter.FINAL_OBJECT_ID);
        final boolean createHtmlReference = this.configuration.getWorkflow() != null
                && this.configuration.getWorkflow().isCreateHtmlReference();
        advisoryNode.setFinalTrackingIdAndUrl(this.referencesBaseUrl, this.trackingidCompany, this.trackingidDigits, sequentialNumber, createHtmlReference);
    }

    private AdvisoryWrapper createReleaseReadyAdvisoryAndValidate(AdvisoryWrapper advisory,
            String releaseDate) throws CsafException, IOException {

        AdvisoryWrapper advisoryCopy = AdvisoryWrapper.createCopy(advisory);

        String versionWithoutSuffix = advisoryCopy.getVersioningStrategy()
                .removeVersionSuffix(advisoryCopy.getDocumentTrackingVersion());
        advisoryCopy.setDocumentTrackingVersion(versionWithoutSuffix);

        String currentReleaseDate = advisoryCopy.getDocumentTrackingCurrentReleaseDate();
        String timestampNow = getCurrentTimestamp();
        if (releaseDate == null) {
            if (currentReleaseDate != null && timestampIsBefore(timestampNow, currentReleaseDate)) {
                releaseDate = currentReleaseDate;
            } else {
                releaseDate = timestampNow;
            }
        } else if (currentReleaseDate != null && timestampIsBefore(releaseDate, currentReleaseDate)) {
            releaseDate = currentReleaseDate;
        }

        advisoryCopy.setDocumentTrackingCurrentReleaseDate(releaseDate);

        String summary = configuration.getSummary().getPublication();
        if (advisory.versionIsAfterInitialPublication()) {
            summary = advisory.getLastRevisionHistoryElementSummary();
        }
        advisoryCopy.removeAllPrereleaseVersions();
        if (advisoryCopy.usesSemanticVersioning()) {
            advisoryCopy.addRevisionHistoryElement(summary, "", releaseDate);
        } else {
            advisoryCopy.editLastRevisionHistoryElement(summary, "", releaseDate);
        }

        if (advisoryCopy.getLastMajorVersion() == 0) {
            advisoryCopy.setDocumentTrackingInitialReleaseDate(releaseDate);
        }

        if (!ValidatorServiceClient.isAdvisoryValid(this.validationBaseUrl, advisoryCopy)) {
            throw new CsafException("Advisory is no valid CSAF document",
                    CsafExceptionKey.AdvisoryValidationError, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return advisoryCopy;
    }

    /**
     * Adds a new version of the document in Draft workflow state.
     *
     * @param advisoryId the ID of the advisory to create a new version of
     * @param revision   the revision for concurrent control
     * @return the revision of the updated CSAF document
     */
    @Transactional
    public String createNewCsafDocumentVersion(String advisoryId, String revision)
            throws IOException, DatabaseException, CsafException {

        LOG.debug("createNewCsafDocumentVersion");
        Authentication credentials = getAuthentication();
        AdvisoryEntity existingEntity = findAdvisoryEntityOrThrow(advisoryId);
        AdvisoryWrapper existingAdvisoryNode = EntityConverter.toWrapper(existingEntity);

        if (canCreateNewVersion(existingAdvisoryNode, credentials)) {
            checkAdvisoryRevision(existingEntity, revision);

            // Persist a version snapshot of the current Published state
            AdvisoryWrapper advisoryVersionBackup = AdvisoryWrapper.createVersionFrom(existingAdvisoryNode);
            AdvisoryVersionEntity versionEntity =
                    EntityConverter.toVersionEntity(advisoryVersionBackup, existingEntity);
            postgresService.saveAdvisoryVersion(versionEntity);

            // Capture pre-mutation state for the audit trail before any mutations occur
            String previousWorkflowState = existingAdvisoryNode.getWorkflowState().name();
            String previousDocVersion = existingAdvisoryNode.getDocumentTrackingVersion();

            // Update the advisory to Draft for new version editing
            existingAdvisoryNode.setLastVersion(existingAdvisoryNode.getDocumentTrackingVersion());
            existingAdvisoryNode.setWorkflowState(WorkflowState.Draft);
            existingAdvisoryNode.setDocumentTrackingStatus(DocumentTrackingStatus.Draft);
            existingAdvisoryNode.setDocumentTrackingVersion(existingAdvisoryNode.getVersioningStrategy()
                    .getNewDocumentVersion(existingAdvisoryNode.getDocumentTrackingVersion()));
            String timestampNow = getCurrentTimestamp();
            existingAdvisoryNode.setDocumentTrackingCurrentReleaseDate(timestampNow);
            existingAdvisoryNode.addRevisionHistoryElement("New Version", "", timestampNow);
            existingAdvisoryNode.setRevision(revision);

            // Persist workflow audit trail using pre-mutation values
            AuditTrailWorkflowEntity workflowAudit = EntityConverter.toAuditTrailWorkflowEntity(
                    existingEntity,
                    credentials.getName(),
                    previousWorkflowState,
                    WorkflowState.Draft.name(),
                    previousDocVersion,
                    existingAdvisoryNode.getDocumentTrackingVersion()
            );
            postgresService.saveAuditTrailWorkflow(workflowAudit);

            // Delete existing comments (new version starts clean)
            postgresService.deleteCommentsByAdvisoryId(UUID.fromString(advisoryId));

            AdvisoryEntity updated = EntityConverter.toEntity(existingAdvisoryNode, existingEntity);
            AdvisoryEntity saved = postgresService.saveAdvisory(updated);
            return String.valueOf(saved.getVersion());
        } else {
            throw new CsafException("User has not the permission to create a new Version in this state",
                    NoPermissionForAdvisory, UNAUTHORIZED);
        }
    }

    /**
     * Adds a comment to the advisory.
     *
     * @param advisoryId the ID of the advisory to add the comment to
     * @param comment    the comment to add as JSON string, requires a commentText
     * @return a tuple of ID and revision of the added comment
     */
    @Secured({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER})
    @Transactional
    public IdAndRevision addComment(String advisoryId, CreateCommentRequest comment)
            throws DatabaseException, CsafException {

        LOG.debug("addComment");
        Authentication credentials = getAuthentication();
        AdvisoryInformationResponse advisoryInfo = getAdvisoryInfoForId(advisoryId);

        if (AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisoryInfo, credentials)) {

            CommentWrapper newComment = CommentWrapper.createNew(advisoryId, comment);
            newComment.setOwner(credentials.getName());

            AdvisoryEntity advisoryEntity;
            try {
                advisoryEntity = findAdvisoryEntityOrThrow(advisoryId);
            } catch (IOException e) {
                throw new DatabaseException(e);
            }
            CommentEntity commentEntity = EntityConverter.toEntity(newComment, advisoryEntity, null);
            CommentEntity savedComment = postgresService.saveComment(commentEntity);

            AuditTrailCommentEntity auditEntity = EntityConverter.toAuditTrailCommentEntity(
                    savedComment, credentials.getName(), ChangeType.Create, newComment.getText());
            postgresService.saveAuditTrailComment(auditEntity);

            return new IdAndRevision(savedComment.getId().toString(), String.valueOf(savedComment.getVersion()));
        } else {
            throw new AccessDeniedException("User has not the permission to add a comment to the advisory");
        }
    }

    /**
     * Get a specific comment (or answer).
     *
     * @param commentId the ID of the comment to get
     * @return the requested comment
     */
    @Secured({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_AUDITOR})
    public CommentResponse getComment(String commentId) throws DatabaseException, CsafException {

        CommentEntity commentEntity = findCommentEntityOrThrow(commentId);
        Authentication credentials = getAuthentication();
        String advisoryId = commentEntity.getAdvisory().getId().toString();
        AdvisoryInformationResponse advisoryInfo = getAdvisoryInfoForId(advisoryId);

        if (AdvisoryWorkflowUtil.canViewComment(advisoryInfo, credentials)) {
            String answerTo = (commentEntity.getAnswerTo() != null)
                    ? commentEntity.getAnswerTo().getId().toString() : null;
            return new CommentResponse(
                    commentId,
                    String.valueOf(commentEntity.getVersion()),
                    advisoryId,
                    commentEntity.getOwner(),
                    commentEntity.getCommentText(),
                    commentEntity.getCsafNodeId(),
                    commentEntity.getFieldName(),
                    answerTo
            );
        } else {
            throw new CsafException("User has not the permission to view comment from the advisory",
                    NoPermissionForAdvisory, UNAUTHORIZED);
        }
    }

    /**
     * Retrieves all comments for a given advisory.
     *
     * @param advisoryId the ID of the advisory to get comments of
     * @return a list of information on all comments for the requested advisory
     */
    @Secured({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_AUDITOR})
    public List<CommentInformationResponse> getComments(String advisoryId) throws IOException, CsafException {

        Authentication credentials = getAuthentication();
        AdvisoryInformationResponse advisoryInfo = getAdvisoryInfoForId(advisoryId);

        if (AdvisoryWorkflowUtil.canViewComment(advisoryInfo, credentials)) {
            return postgresService.findCommentsByAdvisoryId(UUID.fromString(advisoryId)).stream()
                    .map(EntityConverter::toCommentInfo)
                    .toList();
        } else {
            throw new AccessDeniedException("User has not the permission to add a comment to the advisory");
        }
    }

    /**
     * Deletes a comment without its answers from the database.
     *
     * @param commentId       the ID of the comment to remove
     * @param commentRevision the comment's revision for concurrent control
     */
    @Transactional
    void deleteComment(String commentId, String commentRevision) throws DatabaseException, IOException, CsafException {

        CommentEntity entity = findCommentEntityOrThrow(commentId);
        checkCommentRevision(entity, commentRevision);
        // Audit trail entries are removed via ON DELETE CASCADE in the schema
        postgresService.deleteComment(entity);
    }

    /**
     * Updates the text of a comment (or answer).
     *
     * @param commentId the ID of the comment to update
     * @param revision  the revision for concurrent control
     * @param newText   the updated text of the comment
     * @return the new revision of the updated comment
     */
    @Secured({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER})
    @Transactional
    public String updateComment(String advisoryId, String commentId, String revision, String newText)
            throws IOException, DatabaseException, CsafException {

        Authentication credentials = getAuthentication();
        CommentEntity commentEntity = findCommentEntityOrThrow(commentId);
        final String commentOwner = commentEntity.getOwner();
        if (commentOwner == null || !commentOwner.equals(credentials.getName())) {
            throw new AccessDeniedException("User has not the permission to change the comment");
        }
        checkCommentRevision(commentEntity, revision);
        commentEntity.setCommentText(newText);
        CommentEntity saved = postgresService.saveComment(commentEntity);

        AuditTrailCommentEntity auditEntity = EntityConverter.toAuditTrailCommentEntity(
                saved, credentials.getName(), ChangeType.Update, newText);
        postgresService.saveAuditTrailComment(auditEntity);

        return String.valueOf(saved.getVersion());
    }

    /**
     * Adds an answer to a comment.
     *
     * @param commentId   the ID of the comment to add the answer to
     * @param commentText the answer to add, requires a commentText
     * @return a tuple of ID and revision of the added comment
     */
    @Secured({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER})
    @Transactional
    public IdAndRevision addAnswer(String advisoryId, String commentId, String commentText)
            throws DatabaseException, CsafException {

        Authentication credentials = getAuthentication();
        AdvisoryInformationResponse advisoryInfo = getAdvisoryInfoForId(advisoryId);

        if (AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisoryInfo, credentials)) {

            AdvisoryEntity advisoryEntity;
            try {
                advisoryEntity = findAdvisoryEntityOrThrow(advisoryId);
            } catch (IOException e) {
                throw new DatabaseException(e);
            }
            CommentEntity parentComment = findCommentEntityOrThrow(commentId);

            CommentWrapper newAnswer = CommentWrapper.createNewAnswerFromJson(advisoryId, commentId, commentText);
            newAnswer.setOwner(credentials.getName());

            CommentEntity answerEntity = EntityConverter.toEntity(newAnswer, advisoryEntity, null);
            answerEntity.setAnswerTo(parentComment);
            CommentEntity savedAnswer = postgresService.saveComment(answerEntity);

            AuditTrailCommentEntity auditEntity = EntityConverter.toAuditTrailCommentEntity(
                    savedAnswer, credentials.getName(), ChangeType.Create, commentText);
            postgresService.saveAuditTrailComment(auditEntity);

            return new IdAndRevision(savedAnswer.getId().toString(), String.valueOf(savedAnswer.getVersion()));
        } else {
            throw new AccessDeniedException("User has not the permission to add a comment to the advisory");
        }
    }

    /**
     * Retrieves all answers for a given comment.
     *
     * @param commentId the ID of the comment to get answers of
     * @return a list of information on all answers for the requested comment
     */
    @Secured({CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_AUDITOR})
    public List<AnswerInformationResponse> getAnswers(String advisoryId, String commentId)
            throws IOException, CsafException {

        Authentication credentials = getAuthentication();
        AdvisoryInformationResponse advisoryInfo = getAdvisoryInfoForId(advisoryId);

        if (AdvisoryWorkflowUtil.canViewComment(advisoryInfo, credentials)) {
            return postgresService.findAnswersByCommentId(UUID.fromString(commentId)).stream()
                    .map(EntityConverter::toAnswerInfo)
                    .toList();
        } else {
            throw new AccessDeniedException("User has not the permission to view comments of the advisory");
        }
    }

    /**
     * Deletes an answer from the database.
     *
     * @param answerId       the ID of the comment to remove
     * @param answerRevision the comment's revision for concurrent control
     */
    @Transactional
    void deleteAnswer(String answerId, String answerRevision) throws DatabaseException, IOException, CsafException {

        CommentEntity entity = findCommentEntityOrThrow(answerId);
        checkCommentRevision(entity, answerRevision);
        // Audit trail entries are removed via ON DELETE CASCADE
        postgresService.deleteComment(UUID.fromString(answerId));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Load an advisory info response for the given ID without requiring a full wrapper load.
     * Used for permission checks before comment operations.
     */
    private AdvisoryInformationResponse getAdvisoryInfoForId(String advisoryId) throws CsafException {

        Optional<AdvisoryEntity> optionalEntity = postgresService.findAdvisoryById(UUID.fromString(advisoryId));
        if (optionalEntity.isEmpty()) {
            throw new CsafException("Advisory not found", CsafExceptionKey.AdvisoryNotFound, NOT_FOUND);
        }
        return EntityConverter.toAdvisoryInfo(optionalEntity.get());
    }

    /**
     * Load an advisory entity by ID, throwing a DatabaseException when not found.
     */
    private AdvisoryEntity findAdvisoryEntityOrThrow(String advisoryId)
            throws IOException, DatabaseException {

        try {
            UUID id = UUID.fromString(advisoryId);
            return postgresService.findAdvisoryById(id)
                    .orElseThrow(() -> new IdNotFoundException("Advisory not found: " + advisoryId));
        } catch (IllegalArgumentException e) {
            throw new IdNotFoundException("Invalid advisory ID format: " + advisoryId);
        }
    }

    /**
     * Load a comment entity by ID, throwing a DatabaseException when not found.
     */
    private CommentEntity findCommentEntityOrThrow(String commentId) throws DatabaseException {

        try {
            UUID id = UUID.fromString(commentId);
            return postgresService.findCommentById(id)
                    .orElseThrow(() -> new IdNotFoundException("Comment not found: " + commentId));
        } catch (IllegalArgumentException e) {
            throw new IdNotFoundException("Invalid comment ID format: " + commentId);
        }
    }

    /**
     * Verify that the client-supplied revision still matches the advisory's current
     * optimistic-lock version before allowing a mutation to proceed.
     *
     * @param entity   the freshly loaded advisory entity
     * @param revision the revision the client last saw
     * @throws CsafException with {@link CsafExceptionKey#CsafHasWrongRevision} (HTTP 409) if the
     *                        advisory was changed by someone else in the meantime
     */
    private void checkAdvisoryRevision(AdvisoryEntity entity, String revision) throws CsafException {
        if (!String.valueOf(entity.getVersion()).equals(revision)) {
            throw new CsafException(
                    "The advisory was changed by someone else in the meantime, please reload and try again",
                    CsafHasWrongRevision, CONFLICT);
        }
    }

    /**
     * Verify that the client-supplied revision still matches the comment's current
     * optimistic-lock version before allowing a mutation to proceed.
     *
     * @param entity   the freshly loaded comment entity
     * @param revision the revision the client last saw
     * @throws CsafException with {@link CsafExceptionKey#CsafHasWrongRevision} (HTTP 409) if the
     *                        comment was changed by someone else in the meantime
     */
    private void checkCommentRevision(CommentEntity entity, String revision) throws CsafException {
        if (!String.valueOf(entity.getVersion()).equals(revision)) {
            throw new CsafException(
                    "The comment was changed by someone else in the meantime, please reload and try again",
                    CsafHasWrongRevision, CONFLICT);
        }
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private String getCurrentTimestamp() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }
}
