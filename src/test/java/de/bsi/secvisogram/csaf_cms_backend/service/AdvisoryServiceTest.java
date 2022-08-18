package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryAuditTrailField.*;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField.CHANGE_TYPE;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField.CREATED_AT;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.TYPE_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static de.bsi.secvisogram.csaf_cms_backend.json.VersioningType.Semantic;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.equal;
import static java.util.Comparator.comparing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.*;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.ObjectType;
import de.bsi.secvisogram.csaf_cms_backend.model.ChangeType;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateCommentRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.*;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test for the Advisory service. The required CouchDB container is started in the CouchDBExtension.
 */
@SpringBootTest(properties = "csaf.document.templates.companyLogoPath=./src/test/resources/eXXcellent_solutions.png")
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@ContextConfiguration
@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "False positives on multiline format strings")
public class AdvisoryServiceTest {

    @Autowired
    private AdvisoryService advisoryService;

    @MockBean
    private PandocService pandocService;

    @MockBean
    private WeasyprintService weasyprintService;


    private static final String csafJson = """
            {
                "document": {
                    "category": "CSAF_BASE"
                }
            }""";

    private static final String advisoryTemplateString = """
            {
                "owner": "John Doe",
                "type": "Advisory",
                "workflowState": "Draft",
                "csaf": %s
            }
            """;

    private static final String advisoryJsonString = String.format(advisoryTemplateString, csafJson);
    private static final String answerText = "This is an answer.";


    @Test
    public void contextLoads() {
        Assertions.assertNotNull(advisoryService);
    }

    @Test
    public void getAdvisoryCount_empty() {
        assertEquals(0, this.advisoryService.getDocumentCount());
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryCount() throws IOException, CsafException {
        this.advisoryService.addAdvisory(csafToRequest(csafJson));
        // creates advisory and 1 audit trail
        assertEquals(2, this.advisoryService.getDocumentCount());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryIdsTest_empty() throws IOException, CsafException {
        List<AdvisoryInformationResponse> ids = this.advisoryService.getAdvisoryInformations(null);
        assertEquals(0, ids.size());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "Ok for test")
    public void getAdvisoryIdsTest() throws IOException, CsafException {
        IdAndRevision idRev1 = this.advisoryService.addAdvisory(csafToRequest(csafJson));
        IdAndRevision idRev2 = this.advisoryService.addAdvisory(csafToRequest(csafJson));
        List<AdvisoryInformationResponse> infos = this.advisoryService.getAdvisoryInformations(null);
        List<String> expectedIDs = List.of(idRev1.getId(), idRev2.getId());
        List<String> expectedRevisions = List.of(idRev1.getRevision(), idRev2.getRevision());
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();
        List<String> revisions = infos.stream().map(AdvisoryInformationResponse::getRevision).toList();

        assertEquals(ids.size(), expectedIDs.size());
        assertTrue(ids.containsAll(expectedIDs));
        assertTrue(expectedIDs.containsAll(ids));

        assertEquals(revisions.size(), expectedRevisions.size());
        assertTrue(revisions.containsAll(expectedRevisions));
        assertTrue(expectedRevisions.containsAll(revisions));
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_AUTHOR})
    public void addAdvisoryTest_invalidJson() {
        String invalidJson = "no json string";

        assertThrows(JsonProcessingException.class, () -> this.advisoryService.addAdvisory(csafToRequest(invalidJson)));
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_AUTHOR})
    public void addAdvisoryTest_invalidAdvisory() {
        String invalidAdvisory = "{\"no\": \"CSAF document\"}";

        assertThrows(CsafException.class, () -> this.advisoryService.addAdvisory(csafToRequest(invalidAdvisory)));
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_AUTHOR})
    public void addAdvisoryTest() throws IOException, CsafException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        Assertions.assertNotNull(idRev);
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void addAdvisoryTest_noSummary() throws IOException {

        CreateAdvisoryRequest request = csafToRequest(csafDocumentJson("Category2", "Title2")).setSummary("");
        assertThrows(CsafException.class,
                () -> advisoryService.addAdvisory(request));
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void exportAdvisoryTest() throws IOException, CsafException {

        when(this.pandocService.isReady()).thenReturn(Boolean.TRUE);
        when(this.weasyprintService.isReady()).thenReturn(Boolean.TRUE);
        doNothing().when(this.pandocService).convert(any(), any());
        doNothing().when(this.weasyprintService).convert(any(), any());

        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        Path jsonExport = advisoryService.exportAdvisory(idRev.getId(), ExportFormat.JSON);
        Assertions.assertNotNull(jsonExport);
        Path pdfExport = advisoryService.exportAdvisory(idRev.getId(), ExportFormat.PDF);
        Assertions.assertNotNull(pdfExport);
        Path htmlExport = advisoryService.exportAdvisory(idRev.getId(), ExportFormat.HTML);
        Assertions.assertNotNull(htmlExport);
        Path mdExport = advisoryService.exportAdvisory(idRev.getId(), ExportFormat.Markdown);
        Assertions.assertNotNull(mdExport);
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void exportAdvisoryTest_IdNotFound() throws IOException, CsafException {

        advisoryService.addAdvisory(csafToRequest(csafJson));
        assertThrows(CsafException.class, () -> advisoryService.exportAdvisory("wrong Id", ExportFormat.JSON));
    }

    @Test
    public void getAdvisoryTest_notPresent() {
        UUID noAdvisoryId = UUID.randomUUID();
        assertThrows(IdNotFoundException.class, () -> advisoryService.getAdvisory(noAdvisoryId.toString()));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryTest() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());
        assertEquals(idRev.getId(), advisory.getAdvisoryId());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void deleteAdvisoryTest_notPresent() {
        UUID noAdvisoryId = UUID.randomUUID();
        assertThrows(IdNotFoundException.class, () -> advisoryService.deleteAdvisory(noAdvisoryId.toString(), "redundant-revision"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void deleteAdvisoryTest_badRevision() throws IOException, CsafException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        // creates advisory and 1 audit trail
        assertEquals(2, advisoryService.getDocumentCount());
        String revision = "bad revision";
        assertThrows(DatabaseException.class, () -> this.advisoryService.deleteAdvisory(idRev.getId(), revision));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void deleteAdvisoryTest() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        assertEquals(2, advisoryService.getDocumentCount(), "there should be one advisory and one audit trail");
        this.advisoryService.deleteAdvisory(idRev.getId(), idRev.getRevision());
        assertEquals(0, advisoryService.getDocumentCount(), "the advisory and audit trail should be deleted");
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void deleteAdvisoryTest_NoPermission() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        advisoryService.changeAdvisoryWorkflowState(idRev.getId(), idRev.getRevision(), WorkflowState.Review, null, null);
        // author could only delete advisory in workflow state draft
        assertThrows(AccessDeniedException.class, () -> this.advisoryService.deleteAdvisory(idRev.getId(), idRev.getRevision()));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "Ok for test")
    public void deleteAdvisoryTest_twoAdvisories() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        advisoryService.addAdvisory(csafToRequest(csafJson));
        assertEquals(4, advisoryService.getDocumentCount(), "there should be two advisories with an audit trail each");
        this.advisoryService.deleteAdvisory(idRev.getId(), idRev.getRevision());
        assertEquals(2, advisoryService.getDocumentCount(), "one advisory and one audit trail should be deleted");
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void deleteAdvisoryTest_withComments() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("This is a comment", UUID.randomUUID().toString());
        advisoryService.addComment(idRev.getId(), comment);
        assertEquals(4, advisoryService.getDocumentCount(), "there should be one advisory and one comment each with an audit trail");
        this.advisoryService.deleteAdvisory(idRev.getId(), idRev.getRevision());
        assertEquals(0, advisoryService.getDocumentCount(), "the comment and its audit trail should also be deleted");
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void deleteAdvisoryTest_withCommentsAndAnswers() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("This is a comment", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText);

        assertEquals(6, advisoryService.getDocumentCount(), "there should be one advisory one comment and one answer each with an audit trail");
        this.advisoryService.deleteAdvisory(idRevAdvisory.getId(), idRevAdvisory.getRevision());
        assertEquals(0, advisoryService.getDocumentCount(), "the comment and answer and their audit trails should also be deleted");
    }


    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void updateAdvisoryTest() throws IOException, DatabaseException, CsafException {

        final String testCsafJson = """
                {
                    "document": {
                        "category": "CSAF_BASE"
                    }
                }""";

        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(testCsafJson));
        var readAdvisory = advisoryService.getAdvisory(idRev.getId());
        ((ObjectNode) readAdvisory.getCsaf().at("/document")).put("title", "UpdatedTitle");
        CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
        request.setSummary("UpdateSummary");
        advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request);

        // an advisory and 2 audit trails are created
        assertEquals(3, advisoryService.getDocumentCount());
        AdvisoryResponse updatedAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertEquals("UpdatedTitle", updatedAdvisory.getCsaf().at("/document/title").asText());
        assertEquals("UpdateSummary", updatedAdvisory.getCsaf().at("/document/tracking/revision_history/1/summary").asText());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void updateAdvisoryTest_auditTrail() throws IOException, DatabaseException, CsafException {

        var idRev = advisoryService.addAdvisory(csafToRequest(csafDocumentJson("Category1", "Title1")));
        var revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), csafToRequest(csafDocumentJson("Category2", "Title2")));
        revision = advisoryService.updateAdvisory(idRev.getId(), revision, csafToRequest(csafDocumentJson("Category3", "Title3")));
        advisoryService.updateAdvisory(idRev.getId(), revision, csafToRequest(csafDocumentJson("Category4", "Title4")));
        // an advisory and 4 audit trail are created
        assertEquals(5, advisoryService.getDocumentCount());
        advisoryService.getAdvisory(idRev.getId());

        List<JsonNode> auditTrails = readAllAuditTrailDocumentsFromDb();

        assertEquals(4, auditTrails.size());
        auditTrails.sort(comparing(CREATED_AT::stringVal));
        assertThat(CHANGE_TYPE.stringVal(auditTrails.get(0)), equalTo(ChangeType.Create.name()));
        assertThat(CHANGE_TYPE.stringVal(auditTrails.get(1)), equalTo(ChangeType.Update.name()));
        // recreate Advisory from diffs
        AdvisoryWrapper rootWrapper = AdvisoryWrapper.createNewFromCsaf(csafToRequest(AdvisoryWrapper.emptyCsafDocument), "", Semantic.name());
        JsonNode patch0 = auditTrails.get(0).get(DIFF.getDbName());
        AdvisoryWrapper node1 = rootWrapper.applyJsonPatch(patch0);
        assertThat(node1.at(AdvisorySearchField.DOCUMENT_TITLE).asText(), equalTo("Title1"));
        AdvisoryWrapper node2 = node1.applyJsonPatch(auditTrails.get(1).get(DIFF.getDbName()));
        assertThat(node2.at(AdvisorySearchField.DOCUMENT_TITLE).asText(), equalTo("Title2"));
        AdvisoryWrapper node3 = node2.applyJsonPatch(auditTrails.get(2).get(DIFF.getDbName()));
        assertThat(node3.at(AdvisorySearchField.DOCUMENT_TITLE).asText(), equalTo("Title3"));
        AdvisoryWrapper node4 = node2.applyJsonPatch(auditTrails.get(3).get(DIFF.getDbName()));
        assertThat(node4.at(AdvisorySearchField.DOCUMENT_TITLE).asText(), equalTo("Title4"));
    }

    private List<JsonNode> readAllAuditTrailDocumentsFromDb() throws IOException {

        Collection<DbField> fields = Arrays.asList(CouchDbField.ID_FIELD, ADVISORY_ID, CREATED_AT,
                CHANGE_TYPE, DIFF, DOC_VERSION);
        Map<String, Object> selector = expr2CouchDBFilter(equal(ObjectType.AuditTrailDocument.name(), TYPE_FIELD.getDbName()));
        return advisoryService.findDocuments(selector, fields);
    }

    @Test
    public void updateAdvisoryTest_badData() {
        UUID noAdvisoryId = UUID.randomUUID();
        Exception expectedException = assertThrows(DatabaseException.class, () -> advisoryService.updateAdvisory(noAdvisoryId.toString(),
                "redundant", csafToRequest(advisoryJsonString)));
        assertThat(expectedException.getMessage(), containsString("No element with such an ID"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void updateAdvisoryTest_noSummary() throws IOException, CsafException {

        var idRev = advisoryService.addAdvisory(csafToRequest(csafDocumentJson("Category1", "Title1")));
        CreateAdvisoryRequest request = csafToRequest(csafDocumentJson("Category2", "Title2")).setSummary("");
        Exception expectedException = assertThrows(CsafException.class,
                () -> advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request));
        assertThat(expectedException.getMessage(), containsString("Summary must not be empty"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void updateAdvisoryTest_invalidId() throws IOException, DatabaseException, CsafException {

        final String testCsafJson = """
                {
                    "document": {
                        "category": "CSAF_BASE"
                    }
                }""";

        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(testCsafJson));
        var readAdvisory = advisoryService.getAdvisory(idRev.getId());
        ((ObjectNode) readAdvisory.getCsaf().at("/document")).put("title", "UpdatedTitle");
        CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
        request.setSummary("UpdateSummary");
        Exception expectedException = assertThrows(IdNotFoundException.class,
                () -> advisoryService.updateAdvisory("InvalidId", idRev.getRevision(), request));
        assertThat(expectedException.getMessage(), containsString("No element with such an ID"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void updateAdvisoryTest_accessDenied() throws IOException, DatabaseException, CsafException {

        final String testCsafJson = """
                {
                    "document": {
                        "category": "CSAF_BASE"
                    }
                }""";

        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(testCsafJson));
        advisoryService.changeAdvisoryWorkflowState(idRev.getId(), idRev.getRevision(), WorkflowState.Review, null, null);
        var readAdvisory = advisoryService.getAdvisory(idRev.getId());
        ((ObjectNode) readAdvisory.getCsaf().at("/document")).put("title", "UpdatedTitle");
        CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
        request.setSummary("UpdateSummary");
        Exception expectedException = assertThrows(CsafException.class,
                () -> advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request));
        assertThat(expectedException.getMessage(), containsString("no permission"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void updateAdvisoryTest_invalidType() throws IOException, DatabaseException, CsafException {

        final String testCsafJson = """
                {
                    "document": {
                        "category": "CSAF_BASE"
                    }
                }""";

        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(testCsafJson));
        CreateCommentRequest comment = new CreateCommentRequest("CommentText", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRev.getId(), comment);

        var readAdvisory = advisoryService.getAdvisory(idRev.getId());
        ((ObjectNode) readAdvisory.getCsaf().at("/document")).put("title", "UpdatedTitle");
        CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
        request.setSummary("UpdateSummary");

        Exception expectedException = assertThrows(CsafException.class,
                () -> advisoryService.updateAdvisory(idRevComment.getId(), idRevComment.getRevision(), request));
        assertThat(expectedException.getMessage(), containsString("not of type Advisory"));
    }

    @Test
    @WithMockUser(username = "editor1", authorities = { CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER})
    public void changeAdvisoryWorkflowStateTest() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        advisoryService.changeAdvisoryWorkflowState(idRev.getId(), idRev.getRevision(), WorkflowState.Review, null, null);
        // an advisory and 2 audit trails are created
        assertEquals(3, advisoryService.getDocumentCount());
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());
        assertEquals(WorkflowState.Review, advisory.getWorkflowState());
    }

    @Test
    @WithMockUser(username = "editor1", authorities = { CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void createNewCsafDocumentVersionTest() throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class)) {

            validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

            IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
            String revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), idRev.getRevision(), WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);
            advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
            AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());
            assertEquals(WorkflowState.Draft, advisory.getWorkflowState());
        }
    }

    @Test
    @WithMockUser(username = "editor1", authorities = { CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void createNewCsafDocumentVersionTest_accessDenied() throws IOException, CsafException {

        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        assertThrows(CsafException.class,
                () ->  advisoryService.createNewCsafDocumentVersion(idRev.getId(), idRev.getRevision()));
    }

    @Test
    @WithMockUser(username = "editor1", authorities = { CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void createNewCsafDocumentVersionTest_invalidId() throws IOException, CsafException {

        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        assertThrows(DatabaseException.class,
                () ->  advisoryService.createNewCsafDocumentVersion("Invalid Id", idRev.getRevision()));
    }

    private String csafDocumentJson(String documentCategory, String documentTitle) {

        return """
                { "document": {
                      "category": "%s",
                      "title":"%s"
                   }
                }""".formatted(documentCategory, documentTitle);
    }


    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void getCommentsTest_empty() throws IOException, CsafException {
        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        List<CommentInformationResponse> comments = advisoryService.getComments(idRevAdvisory.getId());
        Assertions.assertEquals(0, comments.size());
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void getCommentsTest_accessDenied() throws IOException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SwitchUserGrantedAuthority registeredAuthority = new SwitchUserGrantedAuthority(CsafRoles.ROLE_AUTHOR, auth);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("author2", null, Collections.singletonList(registeredAuthority)));
        assertThrows(AccessDeniedException.class,
                () -> advisoryService.getComments(idRevAdvisory.getId()));
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    void getCommentsTest() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        CreateCommentRequest anotherComment = new CreateCommentRequest("another comment text", UUID.randomUUID().toString());

        IdAndRevision idRevComment1 = advisoryService.addComment(idRevAdvisory.getId(), comment);
        IdAndRevision idRevComment2 = advisoryService.addComment(idRevAdvisory.getId(), anotherComment);

        List<CommentInformationResponse> commentInfos = this.advisoryService.getComments(idRevAdvisory.getId());

        List<String> expectedIDs = List.of(idRevComment1.getId(), idRevComment2.getId());
        List<String> ids = commentInfos.stream().map(CommentInformationResponse::getCommentId).toList();
        assertTrue(ids.size() == expectedIDs.size()
                && ids.containsAll(expectedIDs)
                && expectedIDs.containsAll(ids));

    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void addCommentTest_oneComment() throws DatabaseException, IOException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        String commentText = "This is a comment";

        CreateCommentRequest comment = new CreateCommentRequest(commentText, UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);

        Assertions.assertEquals(4, advisoryService.getDocumentCount(),
                "There should be 1 advisory, 1 comment and an audit trail entry for both");

        CommentResponse commentResp = advisoryService.getComment(idRevComment.getId());
        Assertions.assertEquals(commentText, commentResp.getCommentText());
        Assertions.assertEquals(idRevComment.getId(), commentResp.getCommentId());
        Assertions.assertEquals(idRevAdvisory.getId(), commentResp.getAdvisoryId());
        Assertions.assertEquals(idRevComment.getRevision(), commentResp.getRevision());
        Assertions.assertEquals(comment.getCsafNodeId(), commentResp.getCsafNodeId());
        Assertions.assertEquals("author1", commentResp.getCreatedBy());
        Assertions.assertEquals(null, commentResp.getAnswerToId());

    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void addCommentTest_accessDenied() throws DatabaseException, IOException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        String commentText = "This is a comment";

        CreateCommentRequest comment = new CreateCommentRequest(commentText, UUID.randomUUID().toString());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SwitchUserGrantedAuthority registeredAuthority = new SwitchUserGrantedAuthority(CsafRoles.ROLE_AUTHOR, auth);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("author2", null, Collections.singletonList(registeredAuthority)));
        assertThrows(AccessDeniedException.class,
                () -> advisoryService.addComment(idRevAdvisory.getId(), comment));
    }


    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void addCommentTest_leafNode() throws DatabaseException, IOException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        String commentText = "This is a leaf node comment";

        CreateCommentRequest comment = new CreateCommentRequest(commentText, UUID.randomUUID().toString(), "category");
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);

        Assertions.assertEquals(4, advisoryService.getDocumentCount(),
                "There should be 1 advisory, 1 comment and an audit trail entry for both");

        CommentResponse commentResp = advisoryService.getComment(idRevComment.getId());
        Assertions.assertEquals(commentText, commentResp.getCommentText());
        Assertions.assertEquals("category", commentResp.getFieldName());

    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void addCommentTest_twoComments() throws DatabaseException, IOException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));

        CreateCommentRequest commentOne = new CreateCommentRequest("This is a comment for a field", UUID.randomUUID().toString());
        advisoryService.addComment(idRevAdvisory.getId(), commentOne);
        CreateCommentRequest commentTwo = new CreateCommentRequest("This is another comment for the document", UUID.randomUUID().toString());
        advisoryService.addComment(idRevAdvisory.getId(), commentTwo);

        Assertions.assertEquals(6, advisoryService.getDocumentCount(),
                "There should be 1 advisory, 2 comments and an audit trail entry for each comment");

        List<CommentInformationResponse> commentInfos = advisoryService.getComments(idRevAdvisory.getId());
        Assertions.assertEquals(2, commentInfos.size());

    }

    @Test
    public void deleteComment_notPresent() {
        assertThrows(IdNotFoundException.class,
                () -> advisoryService.deleteComment("not present", "no revision"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void deleteComment_badRevision() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));

        CreateCommentRequest comment = new CreateCommentRequest("a comment", UUID.randomUUID().toString());

        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);

        assertThrows(DatabaseException.class,
                () -> advisoryService.deleteComment(idRevComment.getId(), "bad revision"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void deleteComment() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));

        CreateCommentRequest comment = new CreateCommentRequest("a comment", UUID.randomUUID().toString());

        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);

        Assertions.assertEquals(4, advisoryService.getDocumentCount(),
                "There should be 1 advisory, 1 comment and an audit trail entry for both before deletion");

        advisoryService.deleteComment(idRevComment.getId(), idRevComment.getRevision());

        Assertions.assertEquals(2, advisoryService.getDocumentCount(),
                "There should be 1 advisory and 1 audit trail entry left after deletion");

    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void deleteCommentWithAnswer() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("a comment", UUID.randomUUID().toString());

        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        IdAndRevision idRevAnswer = advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText);

        Assertions.assertEquals(6, advisoryService.getDocumentCount(),
                "There should be 1 advisory, 1 comment, 1 answer and an audit trail entry for each before deletion");

        advisoryService.deleteComment(idRevComment.getId(), idRevComment.getRevision());
        advisoryService.deleteComment(idRevAnswer.getId(), idRevAnswer.getRevision());

        Assertions.assertEquals(2, advisoryService.getDocumentCount(),
                "There should be 1 advisory and 1 audit trail entry left after deletion");

    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    void updateCommentTest() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));

        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());

        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);

        assertEquals(4, advisoryService.getDocumentCount(), "there should be one advisory and one comment each with an audit trail");

        CommentResponse commentResp = advisoryService.getComment(idRevComment.getId());
        Assertions.assertEquals("comment text", commentResp.getCommentText());

        advisoryService.updateComment(idRevAdvisory.getId(), idRevComment.getId(), idRevComment.getRevision(), "updated comment text");

        assertEquals(5, advisoryService.getDocumentCount(), "there should be an additional audit trail for the comment update");

        CommentResponse newComment = advisoryService.getComment(idRevComment.getId());
        Assertions.assertEquals("updated comment text", newComment.getCommentText());
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    void getCommentTest_accessDenied() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        assertEquals(4, advisoryService.getDocumentCount(), "there should be one advisory and one comment each with an audit trail");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SwitchUserGrantedAuthority registeredAuthority = new SwitchUserGrantedAuthority(CsafRoles.ROLE_AUTHOR, auth);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("author2", null, Collections.singletonList(registeredAuthority)));
        assertThrows(CsafException.class,
                () -> advisoryService.getComment(idRevComment.getId()));
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    void updateCommentTest_invalidId() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        assertThrows(DatabaseException.class,
                () -> advisoryService.updateComment(idRevAdvisory.getId(), "InvalidId", idRevComment.getRevision(),
                        "updated comment text"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    void updateCommentTest_accessDenied() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));

        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());

        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);

        assertEquals(4, advisoryService.getDocumentCount(), "there should be one advisory and one comment each with an audit trail");

        CommentResponse commentResp = advisoryService.getComment(idRevComment.getId());
        Assertions.assertEquals("comment text", commentResp.getCommentText());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SwitchUserGrantedAuthority registeredAuthority = new SwitchUserGrantedAuthority(CsafRoles.ROLE_AUTHOR, auth);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("author2", null, Collections.singletonList(registeredAuthority)));
        assertThrows(AccessDeniedException.class,
                () -> advisoryService.updateComment(idRevAdvisory.getId(), idRevComment.getId(), idRevComment.getRevision(),
                        "updated comment text"));

    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void getAnswersTest_empty() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        List<AnswerInformationResponse> answers = advisoryService.getAnswers(idRevAdvisory.getId(), idRevComment.getId());
        Assertions.assertEquals(0, answers.size());
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void getAnswersTest() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        IdAndRevision idRevAnswer = advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText);
        List<AnswerInformationResponse> answers = advisoryService.getAnswers(idRevAdvisory.getId(), idRevComment.getId());
        Assertions.assertEquals(1, answers.size());
        Assertions.assertEquals(idRevAnswer.getId(), answers.get(0).getAnswerId());
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void getAnswersTest_accessDeniedException() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SwitchUserGrantedAuthority registeredAuthority = new SwitchUserGrantedAuthority(CsafRoles.ROLE_AUTHOR, auth);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("author2", null, Collections.singletonList(registeredAuthority)));
        assertThrows(AccessDeniedException.class,
                () -> advisoryService.getAnswers(idRevAdvisory.getId(), idRevComment.getId()));
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void addAnswerTest_oneAnswer() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        IdAndRevision idRevAnswer = advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText);

        Assertions.assertEquals(6, advisoryService.getDocumentCount(),
                "There should be 1 advisory, 1 comment, 1 answer and an audit trail entry for each");

        List<AnswerInformationResponse> answers = advisoryService.getAnswers(idRevAdvisory.getId(), idRevComment.getId());
        Assertions.assertEquals(1, answers.size());

        CommentResponse answer = advisoryService.getComment(idRevAnswer.getId());
        Assertions.assertEquals(answerText, answer.getCommentText());
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void addAnswerTest_twoAnswersSameComment() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText);
        advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), "This is answer  2");

        Assertions.assertEquals(8, advisoryService.getDocumentCount(),
                "There should be 1 advisory, 1 comment, 2 answers and an audit trail entry for each");

        List<AnswerInformationResponse> answers = advisoryService.getAnswers(idRevAdvisory.getId(), idRevComment.getId());
        Assertions.assertEquals(2, answers.size(), "There should be two answers to the comment");
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void addAnswerTest_accessDenied() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SwitchUserGrantedAuthority registeredAuthority = new SwitchUserGrantedAuthority(CsafRoles.ROLE_AUTHOR, auth);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("author2", null, Collections.singletonList(registeredAuthority)));
        assertThrows(AccessDeniedException.class,
                () -> advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText));
    }

    @Test
    public void deleteAnswer_notPresent() {
        assertThrows(IdNotFoundException.class,
                () -> advisoryService.deleteAnswer("not present", "no revision"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void deleteAnswer_badRevision() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        IdAndRevision idRevAnswer = advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText);

        assertThrows(DatabaseException.class,
                () -> advisoryService.deleteAnswer(idRevAnswer.getId(), "bad revision"));
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    public void deleteAnswer() throws IOException, DatabaseException, CsafException {

        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        IdAndRevision idRevAnswer = advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText);

        Assertions.assertEquals(6, advisoryService.getDocumentCount(),
                "There should be 1 advisory, 1 comment, 1 answer and an audit trail entry for each");

        advisoryService.deleteAnswer(idRevAnswer.getId(), idRevAnswer.getRevision());

        Assertions.assertEquals(4, advisoryService.getDocumentCount(),
                "There should be 1 advisory and 1 comment and an audit trail entry for each left after deletion");
    }

    @Test
    @WithMockUser(username = "author1", authorities = { CsafRoles.ROLE_AUTHOR})
    void updateAnswerTest() throws IOException, DatabaseException, CsafException {
        IdAndRevision idRevAdvisory = advisoryService.addAdvisory(csafToRequest(csafJson));
        CreateCommentRequest comment = new CreateCommentRequest("comment text", UUID.randomUUID().toString());
        IdAndRevision idRevComment = advisoryService.addComment(idRevAdvisory.getId(), comment);
        IdAndRevision idRevAnswer = advisoryService.addAnswer(idRevAdvisory.getId(), idRevComment.getId(), answerText);

        Assertions.assertEquals(6, advisoryService.getDocumentCount(),
                "There should be 1 advisory, 1 comment, 1 answer and an audit trail entry for each");

        CommentResponse commentResp = advisoryService.getComment(idRevAnswer.getId());
        Assertions.assertEquals(answerText, commentResp.getCommentText());

        advisoryService.updateComment(idRevAdvisory.getId(), idRevAnswer.getId(), idRevAnswer.getRevision(), "updated answer text");

        assertEquals(7, advisoryService.getDocumentCount(), "there should be an additional audit trail for the answer update");

        CommentResponse newAnswer = advisoryService.getComment(idRevAnswer.getId());
        Assertions.assertEquals("updated answer text", newAnswer.getCommentText());

    }

}
