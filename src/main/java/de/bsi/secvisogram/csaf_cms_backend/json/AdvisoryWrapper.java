package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryField.CSAF;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.ID_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.REVISION_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey.InvalidObjectType;
import static java.time.LocalDateTime.from;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import com.vdurmont.semver4j.Semver;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.*;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Wrapper around JsonNode to read and write advisory objects from/to the CouchDB
 */
public class AdvisoryWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryWrapper.class);

    public static final String emptyCsafDocument = """
            { "document": {
               }
            }""";


    /**
     * Convert an input stream from the couch db to an AdvisoryWrapper
     *
     * @param advisoryStream the stream
     * @return the wrapper
     * @throws IOException error in processing the input stream
     */
    public static AdvisoryWrapper createFromCouchDb(InputStream advisoryStream) throws IOException, CsafException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        AdvisoryWrapper advisoryFromDb = new AdvisoryWrapper(jacksonMapper.readValue(advisoryStream, ObjectNode.class));
        if (advisoryFromDb.getType() != ObjectType.Advisory) {
            throw new CsafException("Object for id is not of type Advisory", InvalidObjectType, BAD_REQUEST);
        }

        return advisoryFromDb;
    }

    /**
     * Create a copy of the advisory and convert it to a AdvisoryVersion
     *
     * @param advisoryToClone the advisory to copy and convert
     * @return the copied and converted AdvisoryWrapper
     * @throws IOException error in processing the input stream
     */
    public static AdvisoryWrapper createVersionFrom(AdvisoryWrapper advisoryToClone) throws IOException {

        final ObjectMapper objMapper = new ObjectMapper();
        String jsonStr = objMapper.writeValueAsString(advisoryToClone.advisoryNode);

        AdvisoryWrapper newAdvisory = new AdvisoryWrapper(objMapper.readValue(jsonStr, ObjectNode.class))
                .setType(ObjectType.AdvisoryVersion)
                .setAdvisoryReference(advisoryToClone.getAdvisoryId());
        RemoveIdHelper.removeCommentIds(newAdvisory.getCsaf());
       
        RemoveIdHelper.removeIds(newAdvisory.advisoryNode, "_rev");
        
        return newAdvisory;
    }

    /**
     * Create a copy of the advisory
     *
     * @param advisoryToClone the advisory to copy
     * @return the copied AdvisoryWrapper
     * @throws IOException error in processing the input stream
     */
    public static AdvisoryWrapper createCopy(AdvisoryWrapper advisoryToClone) throws IOException {
        final ObjectMapper objMapper = new ObjectMapper();
        String jsonStr = objMapper.writeValueAsString(advisoryToClone.advisoryNode);
        return new AdvisoryWrapper(objMapper.readValue(jsonStr, ObjectNode.class));
    }

    private static ObjectNode createAdvisoryNodeFromRequest(CreateAdvisoryRequest csafJson) throws CsafException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        JsonNode csafRootNode = csafJson.getCsaf();
        if (csafRootNode == null || !csafRootNode.has("document")) {
            throw new CsafException("Csaf contains no document entry", CsafExceptionKey.CsafHasNoDocumentNode,
                    HttpStatus.BAD_REQUEST);
        }

        ObjectNode rootNode = jacksonMapper.createObjectNode();
        rootNode.set(CSAF.getDbName(), csafRootNode);
        return rootNode;
    }

    private static ObjectNode createAdvisoryNodeFromRequest(JsonNode csafJson) throws CsafException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        if (csafJson == null || !csafJson.has("document")) {
            throw new CsafException("Csaf contains no document entry", CsafExceptionKey.CsafHasNoDocumentNode,
                    HttpStatus.BAD_REQUEST);
        }

        ObjectNode rootNode = jacksonMapper.createObjectNode();
        rootNode.set(CSAF.getDbName(), csafJson);
        return rootNode;
    }


    private static ObjectNode createAdvisoryNodeFromString(String csafJson) throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        try (final InputStream csafStream = new ByteArrayInputStream(csafJson.getBytes(StandardCharsets.UTF_8))) {
            JsonNode csafRootNode = jacksonMapper.readValue(csafStream, JsonNode.class);
            if (!csafRootNode.has("document")) {
                throw new IllegalArgumentException("Csaf contains no document entry");
            }

            ObjectNode rootNode = jacksonMapper.createObjectNode();
            rootNode.set(CSAF.getDbName(), csafRootNode);
            return rootNode;
        }
    }

    /**
     * Create an initial empty AdvisoryWrapper for the given user
     *
     * @param userName the user
     * @return the wrapper
     * @throws IOException exception in handling json string
     */
    public static AdvisoryWrapper createInitialEmptyAdvisoryForUser(String userName) throws IOException {

        AdvisoryWrapper wrapper = new AdvisoryWrapper(createAdvisoryNodeFromString(emptyCsafDocument));
        wrapper.setOwner(userName)
                .setWorkflowState(WorkflowState.Draft)
                .setType(ObjectType.Advisory);
        return wrapper;
    }

    /**
     * Convert an CSAF document to an initial AdvisoryWrapper for a given user.
     * The wrapper has no id and revision.
     *
     * @param newCsafJson        the csaf string
     * @param userName           the user
     * @param versioningStrategy the configured versioning strategy
     * @return the wrapper
     * @throws CsafException exception in handling json string
     */
    public static AdvisoryWrapper createNewFromCsaf(CreateAdvisoryRequest newCsafJson, String userName, String versioningStrategy) throws CsafException {

        AdvisoryWrapper wrapper = new AdvisoryWrapper(createAdvisoryNodeFromRequest(newCsafJson));
        Versioning versioning = Versioning.getStrategy(versioningStrategy);
        wrapper.setCreatedAtToNow()
                .setOwner(userName)
                .setWorkflowState(WorkflowState.Draft)
                .setLastVersion(versioning.getZeroVersion())
                .setVersioningType(versioning.getVersioningType())
                .setType(ObjectType.Advisory)
                .setDocumentTrackingVersion(versioning.getInitialVersion())
                .setDocumentTrackingStatus(DocumentTrackingStatus.Draft);

        return wrapper;
    }

    /**
     * Convert an CSAF document to an initial AdvisoryWrapper for a given user.
     * The wrapper has no id and revision.
     *
     * @param newCsafJson        the csaf string
     * @param userName           the user
     * @return the wrapper
     * @throws CsafException exception in handling json string
     */
    public static AdvisoryWrapper importNewFromCsaf(JsonNode newCsafJson, String userName) throws CsafException {

        AdvisoryWrapper wrapper = new AdvisoryWrapper(createAdvisoryNodeFromRequest(newCsafJson));
        String documentTrackingVersion = wrapper.getDocumentTrackingVersion();
        Versioning versioning = Versioning.detectStrategy(documentTrackingVersion);
        wrapper.setCreatedAtToNow()
                .setWorkflowState(WorkflowState.Published)
                .setOwner(userName)
                .setLastVersion(documentTrackingVersion)
                .setVersioningType(versioning.getVersioningType())
                .setType(ObjectType.Advisory);

        return wrapper;
    }

    /**
     * Creates a new AdvisoryWrapper based on the given one and set its CSAF document to the changed CSAF document
     *
     * @param existing        the base AdvisoryWrapper
     * @param changedCsafJson the new CSAF document
     * @return the new AdvisoryWrapper
     * @throws CsafException exception in handling json
     */
    public static AdvisoryWrapper updateFromExisting(AdvisoryWrapper existing, CreateAdvisoryRequest changedCsafJson) throws CsafException {

        ObjectNode rootNode = createAdvisoryNodeFromRequest(changedCsafJson);
        AdvisoryWrapper wrapper = new AdvisoryWrapper(rootNode)
                .setAdvisoryId(existing.getAdvisoryId())
                .setOwner(existing.getOwner())
                .setWorkflowState(existing.getWorkflowState())
                .setVersioningType(existing.getVersioningType())
                .setLastVersion(existing.getLastVersion())
                .setType(ObjectType.Advisory)
                .setDocumentTrackingVersion(existing.getDocumentTrackingVersion())
                .setDocumentTrackingStatus(existing.getDocumentTrackingStatus());

        if (existing.getDocumentTrackingInitialReleaseDate() != null) {
            wrapper.setDocumentTrackingInitialReleaseDate(existing.getDocumentTrackingInitialReleaseDate());
        }

        return wrapper;
    }

    private final ObjectNode advisoryNode;

    private AdvisoryWrapper(ObjectNode advisoryNode) {

        this.advisoryNode = advisoryNode;
    }

    private JsonNode getAdvisoryNode() {
        return advisoryNode;
    }

    public String getWorkflowStateString() {

        return getTextFor(AdvisoryField.WORKFLOW_STATE);
    }

    public String getOwner() {

        return getTextFor(AdvisoryField.OWNER);
    }

    public AdvisoryWrapper setOwner(String newValue) {

        this.advisoryNode.put(AdvisoryField.OWNER.getDbName(), newValue);
        return this;
    }

    public ObjectType getType() {

        try {
            String typeText = getTextFor(CouchDbField.TYPE_FIELD);
            return (typeText != null) ? ObjectType.valueOf(typeText) : null;
        } catch (IllegalArgumentException ex) {
            return null; // unknown type
        }
    }

    private AdvisoryWrapper setType(ObjectType newValue) {

        this.advisoryNode.put(CouchDbField.TYPE_FIELD.getDbName(), newValue.name());
        return this;
    }

    /**
     * set reference form AdvisoryVersion to source advisory in the advisory metadata
     *
     * @param advisoryId the id of the referenced advisory
     * @return this
     */
    private AdvisoryWrapper setAdvisoryReference(String advisoryId) {

        this.advisoryNode.put(AdvisoryField.ADVISORY_REFERENCE.getDbName(), advisoryId);
        return this;
    }

    /**
     * get the temporary tracking id from the metadata
     *
      * @return this
     */
    public String getTempTrackingIdInFromMeta() {

        return this.getTextFor(AdvisoryField.TMP_TRACKING_ID);
    }

    /**
     * set the temporary tracking id in the metadata
     *
     * @param tempTrackingId the temporary tracking id
     * @return this
     */
    private AdvisoryWrapper setTempTrackingIdInMeta(String tempTrackingId) {

        this.advisoryNode.put(AdvisoryField.TMP_TRACKING_ID.getDbName(), tempTrackingId);
        return this;
    }

    public WorkflowState getWorkflowState() {

        String stateText = getTextFor(AdvisoryField.WORKFLOW_STATE);
        return WorkflowState.valueOf(stateText);
    }


    public AdvisoryWrapper setWorkflowState(WorkflowState newState) {

        this.advisoryNode.put(AdvisoryField.WORKFLOW_STATE.getDbName(), newState.name());
        return this;
    }

    public boolean usesSemanticVersioning() {
        return this.getVersioningStrategy().getVersioningType() == VersioningType.Semantic;
    }

    public boolean usesIntegerVersioning() {
        return this.getVersioningStrategy().getVersioningType() == VersioningType.Integer;
    }

    public int getLastMajorVersion() {
        String lastVersion = getTextFor(AdvisoryField.LAST_VERSION);
        if (usesSemanticVersioning()) {
            return new Semver(lastVersion).getMajor();
        } else {
            return Integer.parseInt(lastVersion);
        }
    }

    public String getLastVersion() {

        return getTextFor(AdvisoryField.LAST_VERSION);
    }

    public AdvisoryWrapper setLastVersion(String version) {

        this.advisoryNode.put(AdvisoryField.LAST_VERSION.getDbName(), version);
        return this;
    }

    public boolean versionIsAfterInitialPublication() {
        if (usesSemanticVersioning()) {
            Semver semver = new Semver(this.getDocumentTrackingVersion());
            return semver.isGreaterThan(new Semver("1.0.0"));
        } else {
            return Integer.parseInt(this.getDocumentTrackingVersion()) > 1;
        }
    }

    public boolean versionIsUntilIncludingInitialPublication() {
        return !versionIsAfterInitialPublication();
    }

    public Versioning getVersioningStrategy() {
        return Versioning.getStrategy(getVersioningType());
    }

    public String getVersioningType() {

        return getTextFor(AdvisoryField.VERSIONING_TYPE);
    }

    public AdvisoryWrapper setVersioningType(VersioningType versionType) {

        this.advisoryNode.put(AdvisoryField.VERSIONING_TYPE.getDbName(), versionType.name());
        return this;
    }

    private AdvisoryWrapper setVersioningType(String versionType) {

        this.advisoryNode.put(AdvisoryField.VERSIONING_TYPE.getDbName(), versionType);
        return this;
    }

    public String getRevision() {

        return getTextFor(REVISION_FIELD);
    }

    public AdvisoryWrapper setRevision(String newValue) {

        this.advisoryNode.put(REVISION_FIELD.getDbName(), newValue);
        return this;
    }

    public String getAdvisoryId() {

        return getTextFor(ID_FIELD);
    }

    private AdvisoryWrapper setAdvisoryId(String newValue) {

        this.advisoryNode.put(ID_FIELD.getDbName(), newValue);
        return this;
    }

    String getTextFor(DbField dbField) {

        return (advisoryNode.has(dbField.getDbName())) ? advisoryNode.get(dbField.getDbName()).asText() : null;
    }


    public JsonNode getCsaf() {

        return this.advisoryNode.get(AdvisoryField.CSAF.getDbName());
    }

    public AdvisoryWrapper setCreatedAtToNow() {

        this.advisoryNode.put(AuditTrailField.CREATED_AT.getDbName(), DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        return this;
    }


    public JsonNode at(String jsonPtrExpr) {
        return this.advisoryNode.at(jsonPtrExpr);
    }

    /**
     * Get the node in the wrapped advisory node specified by given dbField.
     *
     * @param dbField dbField converted to JSON pointer instance
     * @return Node that matches given JSON Pointer: if no match exists, will return a node for which TreeNode.isMissingNode() returns true.
     */
    public JsonNode at(DbField dbField) {

        String jsonPtrExpr = String.join("/", dbField.getFieldPath());
        return this.advisoryNode.at("/" + jsonPtrExpr);
    }


    public String getDocumentTitle() {

        JsonNode titleNode = this.at(AdvisorySearchField.DOCUMENT_TITLE);
        return (titleNode.isMissingNode()) ? "" : titleNode.asText();
    }

    public String getDocumentTrackingId() {

        JsonNode idNode = this.at(AdvisorySearchField.DOCUMENT_TRACKING_ID);
        return (idNode.isMissingNode()) ? "" : idNode.asText();
    }

    public String getDocumentTrackingVersion() {

        JsonNode versionNode = this.at(AdvisorySearchField.DOCUMENT_TRACKING_VERSION);
        return (versionNode.isMissingNode()) ? "" : versionNode.asText();
    }

    public String getDocumentTrackingStatus() {

        JsonNode statusNode = this.at(AdvisorySearchField.DOCUMENT_TRACKING_STATUS);
        return (statusNode.isMissingNode()) ? "" : statusNode.asText();
    }

    public String getDocumentTrackingGeneratorEngineName() {

        JsonNode nameNode = this.at(AdvisorySearchField.DOCUMENT_TRACKING_GENERATOR_ENGINE_NAME);
        return (nameNode.isMissingNode()) ? "" : nameNode.asText();
    }


    public String getDocumentTrackingGeneratorEngineVersion() {

        JsonNode versionNode = this.at(AdvisorySearchField.DOCUMENT_TRACKING_GENERATOR_ENGINE_VERSION);
        return (versionNode.isMissingNode()) ? "" : versionNode.asText();
    }

    public String getDocumentTrackingCurrentReleaseDate() {

        JsonNode versionNode = this.at(AdvisorySearchField.DOCUMENT_TRACKING_CURRENT_RELEASE_DATE);
        return (versionNode.isMissingNode()) ? null : versionNode.asText();
    }

    public String getDocumentTrackingInitialReleaseDate() {

        JsonNode versionNode = this.at(AdvisorySearchField.DOCUMENT_TRACKING_INITIAL_RELEASE_DATE);
        return (versionNode.isMissingNode()) ? null : versionNode.asText();
    }

    public String getDocumentPublisherName() {

        JsonNode publisherNameNode = this.at("/csaf/document/publisher/name");
        return (publisherNameNode.isMissingNode()) ? null : publisherNameNode.asText();
    }

    /**
     *  Get the tlp label for the tracking id. Use WHITE as default
     * @return the tlp label
     */
    public String getDocumentDistributionTlp() {

        JsonNode tlpNode = this.at("/csaf/document/distribution/tlp");
        return (tlpNode.isMissingNode()) ? null : tlpNode.asText();
    }

    /**
     * Set version field in the document tracking node.
     * Create nodes when they not exist.
     *
     * @param newVersion the new version
     * @return this
     */
    public AdvisoryWrapper setDocumentTrackingVersion(String newVersion) {

        ObjectNode trackingNode = getOrCreateTrackingNode();
        trackingNode.put("version", newVersion);
        return this;
    }

    /**
     * Set id field in the document tracking node.
     * Create nodes when they not exist.
     *
     * @param newId the new version
     * @return this
     */
    public AdvisoryWrapper setDocumentTrackingId(String newId) {

        ObjectNode trackingNode = getOrCreateTrackingNode();
        trackingNode.put("id", newId);
        return this;
    }

    /**
     * Set status field in the document tracking node.
     * Create nodes when they not exist.
     *
     * @param newState the new state
     * @return this
     */
    public AdvisoryWrapper setDocumentTrackingStatus(DocumentTrackingStatus newState) {

        return setDocumentTrackingStatus(newState.getCsafValue());
    }

    private AdvisoryWrapper setDocumentTrackingStatus(String newState) {

        ObjectNode trackingNode = getOrCreateTrackingNode();
        trackingNode.put("status", newState);
        return this;
    }

    /**
     * Set name of the generator engine node.
     * Create nodes when they not exist.
     *
     * @return this
     */
    public AdvisoryWrapper setDocumentTrackingGeneratorEngineName(String engineName) {
        ObjectNode engineNode = getOrCreateObjectNode(
                getOrCreateTrackingNode(), List.of("generator", "engine")
        );
        engineNode.put("name", engineName);
        return this;
    }

    /**
     * Set version of the generator engine node.
     * Create nodes when they not exist.
     *
     * @return this
     */
    public AdvisoryWrapper setDocumentTrackingGeneratorEngineVersion(String engineVersion) {
        ObjectNode engineNode = getOrCreateObjectNode(
                getOrCreateTrackingNode(), List.of("generator", "engine")
        );
        engineNode.put("version", engineVersion);
        return this;
    }

    private ObjectNode getOrCreateObjectNode(ObjectNode node, List<String> ptr) {
        if (ptr.isEmpty()) {
            return node;
        }
        final ObjectMapper jacksonMapper = new ObjectMapper();
        ObjectNode nextNode = (ObjectNode) node.get(ptr.get(0));
        if (nextNode == null) {
            nextNode = jacksonMapper.createObjectNode();
            node.set(ptr.get(0), nextNode);
        }
        return getOrCreateObjectNode(nextNode, ptr.subList(1, ptr.size()));
    }

    public AdvisoryWrapper addRevisionHistoryElement(CreateAdvisoryRequest changedCsafJson, String timestamp) {
        return this.addRevisionHistoryElement(changedCsafJson.getSummary(), changedCsafJson.getLegacyVersion(), timestamp);
    }

    public AdvisoryWrapper addRevisionHistoryElement(String summary, String legacyVersion, String timestamp) {
        ArrayNode historyNode = getOrCreateHistoryNode();
        ObjectNode entry = historyNode.addObject();
        entry.put("date", timestamp);
        if (legacyVersion != null && !legacyVersion.isBlank()) {
            entry.put("legacy_version", legacyVersion);
        }
        entry.put("number", this.getDocumentTrackingVersion());
        entry.put("summary", summary);
        return this;
    }

    public AdvisoryWrapper editLastRevisionHistoryElement(CreateAdvisoryRequest changedCsafJson, String timestamp) {
        return this.editLastRevisionHistoryElement(changedCsafJson.getSummary(), changedCsafJson.getLegacyVersion(), timestamp);
    }

    public AdvisoryWrapper editLastRevisionHistoryElement(String summary, String legacyVersion, String timestamp) {
        ObjectNode lastHistoryNode = getLastHistoryElementByDate();
        if (legacyVersion != null && !legacyVersion.isBlank()) {
            lastHistoryNode.put("legacy_version", legacyVersion);
        }
        lastHistoryNode.put("number", this.getDocumentTrackingVersion());
        lastHistoryNode.put("summary", summary);
        lastHistoryNode.put("date", timestamp);
        return this;
    }

    public String getLastRevisionHistoryElementSummary() {
        ObjectNode lastHistoryNode = getLastHistoryElementByDate();
        return lastHistoryNode.get("summary").asText();
    }

    public AdvisoryWrapper setLastRevisionHistoryElementNumberAndDate(String newNumber, String newDate) {
        ObjectNode lastHistoryNode = getLastHistoryElementByDate();
        lastHistoryNode.put("number", newNumber);
        lastHistoryNode.put("date", newDate);
        return this;
    }

    private ObjectNode getLastHistoryElementByDate() {

        ArrayNode historyNode = getOrCreateHistoryNode();

        ObjectNode[] lastNode = {null};
        String[] lastDate = {null};
        historyNode.forEach(jsonNode -> {
            String nodeDate = jsonNode.get("date").asText();
            if (lastDate[0] == null || timestampIsBefore(lastDate[0], nodeDate)) {
                lastNode[0] = (ObjectNode) jsonNode;
                lastDate[0] = nodeDate;
            }
        });

        return lastNode[0];

    }

    public void removeAllRevisionHistoryElements() {

        ArrayNode historyNode = getOrCreateHistoryNode();
        historyNode.removeAll();
    }

    public void removeAllPrereleaseVersions() {
        ArrayNode historyNode = getOrCreateHistoryNode();
        final ObjectMapper jacksonMapper = new ObjectMapper();
        ArrayNode newHistoryNode = jacksonMapper.createArrayNode();

        if (usesSemanticVersioning()) {
            historyNode.forEach(historyItem -> {
                Semver historyItemVersion = new Semver(historyItem.get("number").asText());
                if (!SemanticVersioning.getDefault().isPrerelease(historyItemVersion)) {
                    newHistoryNode.add(historyItem);
                }
            });
        } else {
            historyNode.forEach(historyItem -> {
                String historyItemVersion = historyItem.get("number").asText();
                if (!("0".equals(historyItemVersion))) {
                    newHistoryNode.add(historyItem);
                }
            });
        }
        getOrCreateTrackingNode().set("revision_history", newHistoryNode);
    }

    /**
     * Set current_release_date in document tracking node.
     * Create nodes when they not exist.
     *
     * @param newDate the new date as ISO-8601 string
     * @return this
     */
    public AdvisoryWrapper setDocumentTrackingCurrentReleaseDate(String newDate) throws CsafException {

        try {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(newDate);
        } catch (DateTimeParseException ex) {
            throw new CsafException("Invalid format for document - tracking - current_release_date",
                    CsafExceptionKey.InvalidDateTimeFormat, HttpStatus.BAD_REQUEST);
        }

        ObjectNode trackingNode = getOrCreateTrackingNode();
        trackingNode.put("current_release_date", newDate);
        return this;
    }

    private ArrayNode getOrCreateHistoryNode() {

        final String revHistory = "revision_history";
        ObjectNode trackingNode = getOrCreateTrackingNode();
        ArrayNode historyNode = (ArrayNode) trackingNode.get(revHistory);
        if (historyNode == null) {
            final ObjectMapper jacksonMapper = new ObjectMapper();
            historyNode = jacksonMapper.createArrayNode();
            trackingNode.set(revHistory, historyNode);
        }
        return historyNode;
    }

    private ObjectNode getOrCreateTrackingNode() {
        return getOrCreateObjectNode(this.advisoryNode, List.of("csaf", "document", "tracking"));
    }

    /**
     * Set current_release_date in document tracking node.
     * Create nodes when they not exist.
     *
     * @param newDate the new date as ISO-8601 string
     * @return this
     */
    public AdvisoryWrapper setDocumentTrackingInitialReleaseDate(String newDate) throws CsafException {

        try {
            if (newDate != null && !newDate.isBlank()) {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(newDate);
            }
        } catch (DateTimeParseException ex) {
            throw new CsafException("Invalid format for document - tracking - initial_release_date",
                    CsafExceptionKey.InvalidDateTimeFormat, HttpStatus.BAD_REQUEST);
        }

        ObjectNode trackingNode = getOrCreateTrackingNode();
        trackingNode.put("initial_release_date", newDate);
        return this;
    }

    public String advisoryAsString() {

        return this.advisoryNode.toString();
    }

    public static AdvisoryInformationResponse convertToAdvisoryInfo(JsonNode doc, Map<DbField,
            BiConsumer<AdvisoryInformationResponse, String>> infoFields) {
        String advisoryId = doc.get(ID_FIELD.getDbName()).asText();
        final AdvisoryInformationResponse response = new AdvisoryInformationResponse(advisoryId);
        infoFields.forEach((key, value) -> setValueInResponse(response, key, doc, value));

        return response;
    }

    private static void setValueInResponse(AdvisoryInformationResponse response, DbField field, JsonNode doc, BiConsumer<AdvisoryInformationResponse, String> advisorySetter) {

        String value;
        if (field.equals(ID_FIELD)) {
            value = doc.get(ID_FIELD.getDbName()).asText();
        } else if (field.equals(REVISION_FIELD)) {
            value = doc.get(REVISION_FIELD.getDbName()).asText();
        } else {
            String jsonPtrExpr = String.join("/", field.getFieldPath());
            value = doc.at("/" + jsonPtrExpr).asText();
        }
        advisorySetter.accept(response, value);
    }

    /**
     * This utility method checks if the current_release_date of the advisory is set and if it lies in the past
     * This is helpful for checking if the current_release_date must be updated or not
     *
     * @param comparedTo the timestamp to compare the current_release_date to
     * @return true if the current release date is not set, or it is in the past
     */
    public boolean currentReleaseDateIsNotSetOrInPast(String comparedTo) {
        return (this.getDocumentTrackingCurrentReleaseDate() == null
                || this.getDocumentTrackingCurrentReleaseDate().isEmpty()
                || timestampIsBefore(this.getDocumentTrackingCurrentReleaseDate(), comparedTo));
    }


    /**
     * Calculate the JavaScript Object Notation (JSON) Patch according to RFC 6902.
     * Computes and returns a JSON patch from source to target
     * Further, if resultant patch is applied to source, it will yield target
     *
     * @param target either valid JSON objects or arrays or values
     * @return the resultant patch
     */
    public JsonNode calculateDiffTo(AdvisoryWrapper target) {

        return calculateJsonDiff(this.getAdvisoryNode(), target.getAdvisoryNode());
    }

    public AdvisoryWrapper applyJsonPatch(JsonNode patch) {

        ObjectNode patched = (ObjectNode) applyJsonPatchToNode(patch, this.getAdvisoryNode());
        return new AdvisoryWrapper(patched);
    }


    /**
     * Calculate the JavaScript Object Notation (JSON) Patch according to RFC 6902.
     * Computes and returns a JSON patch from source to target
     * Further, if resultant patch is applied to source, it will yield target
     *
     * @param source either valid JSON objects or arrays or values
     * @param target either valid JSON objects or arrays or values
     * @return the resultant patch
     */
    public static JsonNode calculateJsonDiff(JsonNode source, JsonNode target) {

        return JsonDiff.asJson(source, target);
    }

    /**
     * Apply patch to JsonNode
     *
     * @param patch  the patch to apply
     * @param source the JsonNode the patch is applied to
     * @return the patched JsonNode
     */
    public static JsonNode applyJsonPatchToNode(JsonNode patch, JsonNode source) {

        return JsonPatch.apply(patch, source);
    }

    /**
     * compares two timestamps if the first is chronologically before the second
     * will be false if the timestamps are exactly the same
     *
     * @param timestamp1 the first timestamp
     * @param timestamp2 the second timestamp
     * @return true if timestamp1 is chronologically before timestamp2, false otherwise
     */
    private static boolean timestampIsBefore(String timestamp1, String timestamp2) {
        LocalDateTime t1 = from(ISO_DATE_TIME.parse(timestamp1));
        LocalDateTime t2 = from(ISO_DATE_TIME.parse(timestamp2));
        return t1.isBefore(t2);
    }

    /**
     * add new node to the document references with category 'sef'
     * @param summary summary of the node
     * @param url url of the node
     * @return this wrapper
     */
    public AdvisoryWrapper addDocumentReferencesNode(String summary, String url) {

        ObjectNode documentNode = getOrCreateObjectNode(this.advisoryNode, List.of("csaf", "document"));
        ArrayNode referencesNode = (ArrayNode) documentNode.get("references");
        if (referencesNode == null) {
            final ObjectMapper jacksonMapper = new ObjectMapper();
            referencesNode = jacksonMapper.createArrayNode();
            documentNode.set("references", referencesNode);
        }

        ObjectNode entry = referencesNode.addObject();
        entry.put("category", "self");

        entry.put("summary", summary);
        entry.put("url", url);
        return this;
    }

    public void setTemporaryTrackingId(String trackingidCompany, String trackingidDigits, long sequentialNumber) {

        String companyName = calculateCompanyName(trackingidCompany);
        String formatted = formatNumber(trackingidDigits, sequentialNumber);
        setDocumentTrackingId(companyName + "-TEMP-" + formatted);
    }

    /**
     * Set the final tracking id in the advisory and a DocumentReferencesNode with the url of the tracking id
     * @param baseUrl the configured base url
     * @param trackingIdCompany the configured company for the name of the tracking id
     * @param trackingIdDigits the count of leading zeros to which the sequentialNumber is filled with
     * @param sequentialNumber the next sequentialNumber
     */
    public void setFinalTrackingIdAndUrl(String baseUrl, String trackingIdCompany, String trackingIdDigits, long sequentialNumber) {

        setTempTrackingIdInMeta(getDocumentTrackingId());

        String companyName = calculateCompanyName(trackingIdCompany);
        String formatted = formatNumber(trackingIdDigits, sequentialNumber);
        int year = calculatePublishYear();
        String trackingId = companyName + "-" + year + "-" + formatted;
        setDocumentTrackingId(trackingId);

        if (baseUrl != null && !baseUrl.isBlank()) {
            String referenceUrl = calculateReferenceUrl(baseUrl, trackingId);
            this.addDocumentReferencesNode("URL generated by system", referenceUrl);
        }
    }

    /**
     * Generate the url for the given baseUrl and tracking id.
     * Format: BaseURL/TLPLabel/YearOfPublication/trackingId.json
     * @param baseUrl configured base url
     * @param trackingId the trackingid
     * @return the calculated url
     */
    String calculateReferenceUrl(String baseUrl, String trackingId) {

        int year = calculatePublishYear();
        String fileName = calculateFileName(trackingId);
        String tlpLabel = getDocumentDistributionTlp() != null ? getDocumentDistributionTlp() : "WHITE";
        return baseUrl + "/" + tlpLabel + "/" + year + "/" + fileName;
    }

    /**
     * If trackingidCompany is  not set then we generate the abbreviation from the Publisher Name.
     * Always the first letters of the words separated by spaces
     * @param trackingidCompany configured company short name
     * @return the calculated name
     */
    String calculateCompanyName(String trackingidCompany) {
        String companyName = trackingidCompany;
        if (trackingidCompany == null || trackingidCompany.isBlank()) {
            String publisherName = this.getDocumentPublisherName();
            publisherName = (publisherName != null) ? publisherName.trim() : "";
            companyName = (publisherName.indexOf(' ') >= 0) ? publisherName.substring(0, publisherName.indexOf(' ')) : publisherName;
        }
        return companyName;
    }

    /**
     * Calculate the year of publishing based on the initialReleaseDate
     * @return the calculated year
     */
    int calculatePublishYear() {

        String date = this.getDocumentTrackingInitialReleaseDate();
        return (date != null) ? from(ISO_DATE_TIME.parse(date)).getYear() : LocalDateTime.now().getYear();
    }

    /**
     * add leading Zeros
     * @param trackingidDigits number of digits
     * @param sequentialNumber generated sequenital number
     * @return number with leading zeros
     */
    static String formatNumber(String trackingidDigits, long sequentialNumber) {
        int digits;
        try {
            digits = Integer.parseInt(trackingidDigits);
        } catch (NumberFormatException ex) {
            LOG.warn("csaf.trackingid.digits is not an integer {}", ex.getMessage());
            digits = 5;
        }
        return String.format("%0" + digits + "d", sequentialNumber);
    }

    /**
     * Generate CSAF filename from trackingId
     * according to  <a href="https://docs.oasis-open.org/csaf/csaf/v2.0/os/csaf-v2.0-os.html#51-filename">CSAF 5.1</a>
     * @param trackingId the value from /document/tracking/id
     * @return the filename
     */
    static String calculateFileName(String trackingId) {

        String documentName = trackingId.toLowerCase(Locale.ENGLISH).replaceAll("[^+\\-a-z0-9]+", "_");
        return documentName + ".json";
    }
}
