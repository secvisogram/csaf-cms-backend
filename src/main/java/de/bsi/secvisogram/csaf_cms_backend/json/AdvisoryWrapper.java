package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryField.CSAF;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.ID_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.REVISION_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey.InvalidObjectType;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.BiConsumer;
import org.springframework.http.HttpStatus;

/**
 * Wrapper around JsonNode to read and write advisory objects from/to the CouchDB
 */
public class AdvisoryWrapper {

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
     * @param advisoryToClone the stream
     * @return the wrapper
     * @throws IOException error in processing the input stream
     */
    public static AdvisoryWrapper createVersionFrom(AdvisoryWrapper advisoryToClone) throws IOException {

        final ObjectMapper objMapper = new ObjectMapper();
        String jsonStr = objMapper.writeValueAsString(advisoryToClone.advisoryNode);

        AdvisoryWrapper newAdvisory = new AdvisoryWrapper(objMapper.readValue(jsonStr, ObjectNode.class))
                .setType(ObjectType.AdvisoryVersion)
                .setAdvisoryReference(advisoryToClone.getAdvisoryId());
        RemoveIdHelper.removeCommentIds(newAdvisory.getCsaf());
        return newAdvisory;
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
                .setLastVersion("0.0.0")
                .setVersioningType(versioning.getVersioningType())
                .setType(ObjectType.Advisory)
                .setDocumentTrackingVersion(versioning.getInitialVersion())
                .setDocumentTrackingStatus(DocumentTrackingStatus.Draft);
        wrapper.checkCurrentReleaseDateIsSet();

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
     * set reference form AdvisoryVersion to source advisory
     * @param advisoryId the id of the referenced advisory
     * @return this
     */
    private AdvisoryWrapper setAdvisoryReference(String advisoryId) {

        this.advisoryNode.put(AdvisoryField.ADVISORY_REFERENCE.getDbName(), advisoryId);
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

    public int getLastMajorVersion() {

        String lastVersion = getTextFor(AdvisoryField.LAST_VERSION);
        return new Semver(lastVersion).getMajor();
    }

    public String getLastVersion() {

        return getTextFor(AdvisoryField.LAST_VERSION);
    }

    public AdvisoryWrapper setLastVersion(String version) {

        this.advisoryNode.put(AdvisoryField.LAST_VERSION.getDbName(), version);
        return this;
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

        JsonNode versionNode = this.at(AdvisorySearchField.DOCUMENT_TRACKING_STATUS);
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


    /**
     * Set tracking field in the document tracking node.
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

    public AdvisoryWrapper addRevisionHistoryEntry(CreateAdvisoryRequest changedCsafJson) {

        return this.addRevisionHistoryEntry(changedCsafJson.getSummary(), changedCsafJson.getLegacyVersion());
    }

    public AdvisoryWrapper addEntryForNewCreatedVersion(String summary, String legacyVersion) {

        ArrayNode historyNode = getOrCreateHistoryNode();
        ObjectNode entry = historyNode.addObject();
        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        entry.put("date", now);
        if (legacyVersion != null && !legacyVersion.isBlank()) {
            entry.put("legacy_revision", legacyVersion);
        }
        entry.put("number", this.getDocumentTrackingVersion());
        entry.put("summary", summary);
        return this;
    }


    public AdvisoryWrapper addRevisionHistoryEntry(String summary, String legacyVersion) {

        ArrayNode historyNode = getOrCreateHistoryNode();
        if (getVersioningStrategy().getVersioningType() == VersioningType.Semantic && isInitialPublicReleaseOrEarlier()) {
            ObjectNode entry = historyNode.addObject();
            String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            entry.put("date", now);
            if (legacyVersion != null && !legacyVersion.isBlank()) {
                entry.put("legacy_revision", legacyVersion);
            }
            entry.put("number", this.getDocumentTrackingVersion());
            entry.put("summary", summary);
        } else {
            ObjectNode latestEntry = getLatestEntryInHistoryAfterPrerelease(historyNode);
            if (latestEntry == null) {
                latestEntry = historyNode.addObject();
            }
            latestEntry.put("date", this.getDocumentTrackingCurrentReleaseDate());
            if (legacyVersion != null && !legacyVersion.isBlank()) {
                latestEntry.put("legacy_revision", legacyVersion);
            }
            latestEntry.put("number", this.getDocumentTrackingVersion());
            latestEntry.put("summary", summary);
        }
        return this;
    }

    public void removeAllRevisionHistoryEntries() {

        ArrayNode historyNode = getOrCreateHistoryNode();
        historyNode.removeAll();
    }

    public void removeAllPrereleaseVersions() {

        if (getVersioningStrategy().getVersioningType() == VersioningType.Semantic &&  isInitialPublicReleaseOrEarlier()) {
            ArrayNode historyNode = getOrCreateHistoryNode();
            historyNode.removeAll();
        }
    }

    private ObjectNode getLatestEntryInHistoryAfterPrerelease(ArrayNode historyNode) {

        if (historyNode.size() > 0) {

            ObjectNode node = (ObjectNode) historyNode.get(historyNode.size() - 1);
            if (getLastVersion().equals(node.get("number").asText())) {
                return null;
            } else {
               return node;
            }
        } else {
            return null;
        }
    }

    public boolean isInitialPublicReleaseOrEarlier() {

        Semver version = new Semver(this.getDocumentTrackingVersion());
        return (version.getMajor() < 1)
                || ((version.getMajor() == 1) && (version.getMinor() == 0) && (version.getPatch() == 0));
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

        final ObjectMapper jacksonMapper = new ObjectMapper();
        ObjectNode versionNode = (ObjectNode) this.at(AdvisorySearchField.DOCUMENT);
        ObjectNode trackingNode = (ObjectNode) versionNode.get("tracking");
        if (trackingNode == null) {
            trackingNode = jacksonMapper.createObjectNode();
            versionNode.set("tracking", trackingNode);
        }
        return trackingNode;
    }

    /**
     * Set current_release_date in document tracking node.
     * Create nodes when they not exist.
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
     * The current_release_date must always be filled.
     * When saving, the system always checks whether the current_release_date is in the past. In this case the date is set to the current date. In all other cases (date in the future) this remains.
     * @throws CsafException thrown when  date is invalid
     */
    public void checkCurrentReleaseDateIsSet() throws CsafException {

        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        if (this.getDocumentTrackingCurrentReleaseDate() == null
                || this.getDocumentTrackingCurrentReleaseDate().compareTo(now) < 0) {
            this.setDocumentTrackingCurrentReleaseDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        }
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




}
