package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToInputstream;
import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import de.bsi.secvisogram.csaf_cms_backend.PostgreSQLExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.db.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.model.ChangeType;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for importing several versions of one and the same advisory.
 */
@SpringBootTest(properties = {"csaf.workflow.allowOwnDocumentsApproved=true"})
@ExtendWith(PostgreSQLExtension.class)
@DirtiesContext
@SpringJUnitConfig
public class AdvisoryImportVersionsTest {

    private static final String TRACKING_ID = "CSAF-CMS-103";

    @Autowired
    private AdvisoryService advisoryService;

    @Autowired
    private JdbcTemplate jdbc;

    // ---------------------------------------------------------------------------------------------------------------
    // importing a newer version of an already imported advisory
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void importNewerVersionUpdatesTheExistingAdvisoryAndKeepsThePreviousVersionAsSnapshot()
            throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {

            IdAndRevision firstImport = importAdvisory(TRACKING_ID, "1.0.0");
            IdAndRevision secondImport = importAdvisory(TRACKING_ID, "1.1.0");

            assertEquals(firstImport.getId(), secondImport.getId(),
                    "importing a newer version should update the existing advisory instead of creating a second one");

            AdvisoryResponse live = advisoryService.getAdvisory(firstImport.getId());
            assertEquals("1.1.0", trackingVersionOf(live), "the newer version should be the live advisory");
            assertEquals(TRACKING_ID, live.getCsaf().at("/document/tracking/id").asString(),
                    "the tracking ID should be kept");
            assertEquals(WorkflowState.Published, live.getWorkflowState(),
                    "an imported version is already published");

            assertEquals(1, countLiveAdvisories(),
                    "there should be exactly one live advisory document for the tracking ID");

            List<JsonNode> snapshots = readAllAdvisoryVersionsFromDb();
            assertEquals(1, snapshots.size(), "the replaced version should be kept as an AdvisoryVersion snapshot");
            assertEquals("1.0.0", trackingVersionOf(snapshots.get(0)),
                    "the snapshot should hold the version that was replaced");
            assertEquals(firstImport.getId(), snapshots.get(0).get("advisoryReference").asString(),
                    "the snapshot should reference the live advisory");

            List<AuditTrailRow> auditTrails = readAllAdvisoryAuditTrailsFromDb();
            auditTrails.sort(comparing(AuditTrailRow::createdAt));
            assertEquals(2, auditTrails.size(), "both imports should be recorded in the audit trail");
            assertEquals(ChangeType.Create.name(), auditTrails.get(0).changeType(),
                    "the first import creates the advisory");
            assertEquals(ChangeType.Update.name(), auditTrails.get(1).changeType(),
                    "importing a newer version updates the existing advisory");
            assertEquals("1.1.0", auditTrails.get(1).docVersion(),
                    "the audit trail should record the newly imported version");
        }
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void importSeveralNewerVersionsKeepsAllReplacedVersionsAsSnapshots()
            throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {

            IdAndRevision firstImport = importAdvisory(TRACKING_ID, "1.0.0");
            importAdvisory(TRACKING_ID, "1.1.0");
            importAdvisory(TRACKING_ID, "2.0.0");

            assertEquals("2.0.0", trackingVersionOf(advisoryService.getAdvisory(firstImport.getId())),
                    "the newest imported version should be the live advisory");

            List<String> snapshotVersions = readAllAdvisoryVersionsFromDb().stream()
                    .map(AdvisoryImportVersionsTest::trackingVersionOf)
                    .sorted()
                    .toList();
            assertEquals(List.of("1.0.0", "1.1.0"), snapshotVersions,
                    "every replaced version should be kept as an AdvisoryVersion snapshot");
        }
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void importNewerVersionWithIntegerVersioningComparesNumerically()
            throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {

            IdAndRevision firstImport = importAdvisory(TRACKING_ID, "9");
            IdAndRevision secondImport = importAdvisory(TRACKING_ID, "10");

            assertEquals(firstImport.getId(), secondImport.getId(),
                    "integer versioning should be handled the same way as semantic versioning");
            assertEquals("10", trackingVersionOf(advisoryService.getAdvisory(firstImport.getId())),
                    "version 10 is newer than version 9 - versions must not be compared as strings");
        }
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void importNewerVersionSavesTheContentOfTheNewVersion()
            throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {

            IdAndRevision firstImport = importAdvisory(csafDocumentWithContent(TRACKING_ID, "1.0.0",
                    "The first release", "CVE-2022-00001"));
            importAdvisory(csafDocumentWithContent(TRACKING_ID, "2.0.0",
                    "The second release", "CVE-2022-99999"));

            AdvisoryResponse live = advisoryService.getAdvisory(firstImport.getId());
            assertEquals("The second release", live.getCsaf().at("/document/title").asString(),
                    "the document of the newer version should have been saved");
            assertEquals("CVE-2022-99999", live.getCsaf().at("/vulnerabilities/0/cve").asString(),
                    "the whole CSAF document should be replaced, not only the tracking node");
            assertEquals("The second release", live.getTitle(),
                    "the title of the advisory should be taken from the newer version");

            List<JsonNode> snapshots = readAllAdvisoryVersionsFromDb();
            assertEquals(1, snapshots.size());
            JsonNode snapshot = snapshots.get(0);
            assertEquals("The first release", snapshot.at("/csaf/document/title").asString(),
                    "the snapshot should still hold the content of the version it replaced");
            assertEquals("CVE-2022-00001", snapshot.at("/csaf/vulnerabilities/0/cve").asString(),
                    "the snapshot should still hold the content of the version it replaced");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // guards: imports that must keep being rejected once #103 is implemented
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void importOfAnOlderVersionIsRejectedAndLeavesTheAdvisoryUntouched()
            throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {

            IdAndRevision firstImport = importAdvisory(TRACKING_ID, "2.0.0");

            assertThrows(CsafException.class, () -> importAdvisory(TRACKING_ID, "1.0.0"),
                    "importing an older version than the existing one should be rejected");

            assertEquals("2.0.0", trackingVersionOf(advisoryService.getAdvisory(firstImport.getId())),
                    "the live advisory must not be replaced by an older version");
            assertEquals(0, readAllAdvisoryVersionsFromDb().size(),
                    "a rejected import must not create a version snapshot");
        }
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void importOfTheSameVersionIsStillRejectedAsDuplicate() throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {

            importAdvisory(TRACKING_ID, "1.0.0");

            assertThrows(CsafException.class, () -> importAdvisory(TRACKING_ID, "1.0.0"),
                    "importing the very same version twice is still a duplicate");

            assertEquals(1, countLiveAdvisories(), "the duplicate must not be stored");
            assertEquals(0, readAllAdvisoryVersionsFromDb().size(),
                    "a rejected import must not create a version snapshot");
        }
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void importOfAVersionWithADifferentVersioningSchemeIsRejected()
            throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {

            IdAndRevision firstImport = importAdvisory(TRACKING_ID, "1");

            assertThrows(CsafException.class, () -> importAdvisory(TRACKING_ID, "2.0.0"),
                    "a semantic version cannot be a newer version of an integer versioned advisory");

            assertEquals("1", trackingVersionOf(advisoryService.getAdvisory(firstImport.getId())),
                    "the live advisory must not be replaced across versioning schemes");
        }
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void importOfANewerVersionIsRejectedWhileTheAdvisoryIsNotPublished()
            throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {

            IdAndRevision firstImport = importAdvisory(TRACKING_ID, "1.0.0");
            // somebody started to work on the next version in the CMS
            advisoryService.createNewCsafDocumentVersion(firstImport.getId(), firstImport.getRevision());
            AdvisoryResponse draft = advisoryService.getAdvisory(firstImport.getId());
            assertEquals(WorkflowState.Draft, draft.getWorkflowState());
            String draftVersion = trackingVersionOf(draft);
            assertNotEquals("1.0.0", draftVersion);

            CsafException rejected = assertThrows(CsafException.class, () -> importAdvisory(TRACKING_ID, "1.1.0"),
                    "an import must not overwrite an advisory that is not published");
            assertEquals(CsafExceptionKey.AdvisoryNotPublished, rejected.getExceptionKey());
            assertTrue(rejected.getMessage().contains(WorkflowState.Draft.name()),
                    "the message should name the state the advisory is actually in, so that an operator reading "
                    + "the .err file of a failed import is not sent looking for an editor that does not exist - "
                    + "the state can just as well be Approved, RfPublication or AutoPublish");

            AdvisoryResponse stillDraft = advisoryService.getAdvisory(firstImport.getId());
            assertEquals(WorkflowState.Draft, stillDraft.getWorkflowState(),
                    "a rejected import must leave the unpublished advisory's state untouched");
            assertEquals(draftVersion, trackingVersionOf(stillDraft),
                    "a rejected import must leave the unpublished advisory's version untouched");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------------------------

    private static MockedStatic<ValidatorServiceClient> mockValidCsaf() {

        MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class);
        validatorMock.when(() -> ValidatorServiceClient.isCsafValid(any(), any())).thenReturn(Boolean.TRUE);
        validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);
        return validatorMock;
    }

    private IdAndRevision importAdvisory(String trackingId, String version)
            throws IOException, DatabaseException, CsafException {

        final ObjectMapper jacksonMapper = new JsonMapper();
        try (final InputStream csafStream = csafToInputstream(csafDocument(trackingId, version))) {
            return advisoryService.importAdvisory(jacksonMapper.readValue(csafStream, JsonNode.class));
        }
    }

    private IdAndRevision importAdvisory(String csafDocument) throws IOException, DatabaseException, CsafException {

        final ObjectMapper jacksonMapper = new JsonMapper();
        try (final InputStream csafStream = csafToInputstream(csafDocument)) {
            return advisoryService.importAdvisory(jacksonMapper.readValue(csafStream, JsonNode.class));
        }
    }

    private static String trackingVersionOf(AdvisoryResponse advisory) {
        return advisory.getCsaf().at("/document/tracking/version").asString();
    }

    private static String trackingVersionOf(JsonNode versionSnapshot) {
        return versionSnapshot.at("/csaf/document/tracking/version").asString();
    }

    private int countLiveAdvisories() {
        return jdbc.queryForObject("SELECT count(*) FROM advisories", Integer.class);
    }

    /** The replaced versions, each as a node with the {@code advisoryReference} and the {@code csaf} document. */
    private List<JsonNode> readAllAdvisoryVersionsFromDb() {

        ObjectMapper mapper = new JsonMapper();
        return jdbc.query("SELECT advisory_id::text AS advisory_id, csaf::text AS csaf FROM advisory_versions",
                (rs, rowNum) -> mapper.createObjectNode()
                        .put("advisoryReference", rs.getString("advisory_id"))
                        .set("csaf", mapper.readTree(rs.getString("csaf"))));
    }

    private record AuditTrailRow(Instant createdAt, String changeType, String docVersion) {
    }

    private List<AuditTrailRow> readAllAdvisoryAuditTrailsFromDb() {

        return new java.util.ArrayList<>(jdbc.query(
                "SELECT created_at, change_type, doc_version FROM audit_trail_documents",
                (rs, rowNum) -> new AuditTrailRow(rs.getTimestamp("created_at").toInstant(),
                        rs.getString("change_type"), rs.getString("doc_version"))));
    }

    private static String csafDocumentWithContent(String trackingId, String version, String title, String cve) {

        return csafDocument(trackingId, version)
                .replace("\"title\": \"Import of different versions from one advisory\"", "\"title\": \"" + title + "\"")
                .replaceFirst("\\}$", ",\n  \"vulnerabilities\": [{\"cve\": \"" + cve + "\"}]\n}");
    }

    private static String csafDocument(String trackingId, String version) {

        return """
               {
                 "document": {
                   "category": "CSAF Base",
                   "csaf_version": "2.0",
                   "title": "Import of different versions from one advisory",
                   "lang": "en",
                   "distribution": {
                     "tlp": {
                       "label": "WHITE"
                     }
                   },
                   "publisher": {
                     "category": "other",
                     "name": "Secvisogram Automated Tester",
                     "namespace": "https://github.com/secvisogram/secvisogram"
                   },
                   "tracking": {
                     "current_release_date": "2022-09-08T12:33:45.678Z",
                     "id": "%s",
                     "initial_release_date": "2022-09-08T12:33:45.678Z",
                     "revision_history": [
                       {
                         "number": "%s",
                         "date": "2022-09-08T12:33:45.678Z",
                         "summary": "imported version %s"
                       }
                     ],
                     "status": "final",
                     "version": "%s"
                   }
                 }
               }""".formatted(trackingId, version, version, version);
    }
}
