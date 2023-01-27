package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.*;
import static de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus.Draft;
import static de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus.Final;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test for the workflow in the Advisory service. The required CouchDB container is started in the CouchDBExtension.
 */
@SpringBootTest(properties = "csaf.document.versioning=Integer")
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@ContextConfiguration
public class AdvisoryWorkflowIntegerVersioningTest {

    @Autowired
    private AdvisoryService advisoryService;

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void workflowTest_approveToDraft() throws IOException, DatabaseException, CsafException {
        final String csafJson = csafJsonCategoryTitleId("Category1", "Title1", "TrackingOne");
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        var readAdvisory = advisoryService.getAdvisory(idRev.getId());
        ((ObjectNode) readAdvisory.getCsaf().at("/document")).put("title", "UpdatedTitle");
        CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
        request.setSummary("UpdateSummary");
        String revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request);
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
        advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);

        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1"));
        assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0", "1"),
                "change of workflow state to approved should introduce version 1");
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void workflowTest_revisionHistory() throws IOException, DatabaseException, CsafException {
        try (final MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class)) {

            final String csafJson = csafJsonCategoryTitleId("Category1", "Title1", "TrackingOne");
            IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
            var readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0"),
                    "creating the advisory should initialize revision history for version 0");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
            request.setSummary("UpdateSummary");
            String revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(1));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0"),
                    "change of advisory itself should not add revision history element");
            assertEquals("UpdateSummary", readAdvisory.getCsaf().at("/document/tracking/revision_history/0/summary").asText(),
                    "the revision history element summary should be updated");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0"),
                    "change of workflow state to review should not add revision history element");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0", "1"),
                    "change of workflow state to approved should introduce version 1");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // for change of workflow state to RfPublication the advisory must be valid
            validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0", "1"),
                    "change of workflow state to RfPublication should not introduce new revision history element");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1"),
                    "change of workflow state to Published should remove revision history element with version 0");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("2"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "creating new version should add revision history element with next version");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
            request.setSummary("UpdateSummary");
            revision = advisoryService.updateAdvisory(idRev.getId(), readAdvisory.getRevision(), request);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("2"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "change of advisory itself should not add revision history element");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("2"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "change of workflow state to review should not introduce new revision history element");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("2"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "change of workflow state to approved should not result in version raise after first publication");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("2"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "change of workflow state to RfPublication should not introduce new revision history element");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("2"));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "change of workflow state to Published should not introduce new revision history element");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow new Version
            advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("3"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2", "3"),
                    "creating new version should add revision history element with next version");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

        }
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void workflowTest_revisionHistory_allStateChanges() throws IOException, DatabaseException, CsafException {

        final ObjectMapper jacksonMapper = new ObjectMapper();

        try (final MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class)) {
            validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

            final String csafJson = csafMinimalValidDoc(Draft, "0");
            IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));

            AdvisoryResponse currentAdvisory = advisoryService.getAdvisory(idRev.getId());
            ((ObjectNode) currentAdvisory.getCsaf().at("/document")).put("title", "Pre-release Title");
            CreateAdvisoryRequest request = csafToRequest(currentAdvisory.getCsaf().toPrettyString());
            request.setSummary("updated title in pre-release draft");
            String revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request);

            AdvisoryResponse readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0"),
                    "change of title should not trigger a version raise");

            currentAdvisory = advisoryService.getAdvisory(idRev.getId());
            ObjectNode emptyProductTree = jacksonMapper.createObjectNode();
            ((ObjectNode) currentAdvisory.getCsaf()).set("product_tree", emptyProductTree);
            request = csafToRequest(currentAdvisory.getCsaf().toPrettyString());
            request.setSummary("added empty product_tree");
            revision = advisoryService.updateAdvisory(idRev.getId(), revision, request);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0"),
                    "change of product_tree should not trigger a version raise");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Draft, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0"),
                    "going back from Review to Draft should not trigger a version change");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0", "1"),
                    "going to Approved should raise version and add a revision history entry");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Draft, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0", "1"),
                    "going back from Approved to Draft should not trigger a version raise");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0", "1"),
                    "going to Approved should not trigger a version raise");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0", "1"),
                    "going to RfPublication should not add revision history element");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);

            assertEquals("added empty product_tree",
                    readAdvisory.getCsaf().at("/document/tracking/revision_history/1/summary").asText(),
                    "The last revision history element's summary should be copied from the preceding revision history element");

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1"),
                    "Publishing the advisory for the first time should delete all prerelease version entries and set version 1");

            revision = advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "creating new version should add new version");

            currentAdvisory = advisoryService.getAdvisory(idRev.getId());
            ((ObjectNode) currentAdvisory.getCsaf().at("/document")).put("title", "Updated Title After Release");
            request = csafToRequest(currentAdvisory.getCsaf().toPrettyString());
            request.setSummary("updated title after release");
            revision = advisoryService.updateAdvisory(idRev.getId(), revision, request);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "change of title should not trigger version raise");

            currentAdvisory = advisoryService.getAdvisory(idRev.getId());
            ((ObjectNode) currentAdvisory.getCsaf()).remove("product_tree");
            request = csafToRequest(currentAdvisory.getCsaf().toPrettyString());
            request.setSummary("removed product_tree");
            revision = advisoryService.updateAdvisory(idRev.getId(), revision, request);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "change of product_tree should not trigger a version raise");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Draft, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Draft, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "workflow changes should not trigger version changes");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                    "publishing the advisory should not add a version entry");

            assertEquals("removed product_tree",
                    readAdvisory.getCsaf().at("/document/tracking/revision_history/1/summary").asText(),
                    "The last revision history element's summary should be copied/kept throughout all state changes");

        }
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void workflowTest_importCsafDocument() throws IOException, DatabaseException, CsafException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        try (final MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class)) {
            validatorMock.when(() -> ValidatorServiceClient.isCsafValid(any(), any())).thenReturn(Boolean.TRUE);

            try (final InputStream csafStream = csafToInputstream(csafMinimalValidDoc(Final, "1"))) {
                final JsonNode csafRootNode = jacksonMapper.readValue(csafStream, JsonNode.class);
                // 1.Import
                IdAndRevision idRev = advisoryService.importAdvisory(csafRootNode);
                // 2. Create new Dokument
                advisoryService.createNewCsafDocumentVersion(idRev.getId(), idRev.getRevision());

                AdvisoryResponse readAdvisory = advisoryService.getAdvisory(idRev.getId());
                assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1", "2"),
                        "creating new version should add new version");
            }
        }
    }

    private void assertRevisionHistoryVersionsMatch(AdvisoryResponse advisory, List<String> expectedVersions, String message) {
        List<String> revisionHistoryVersions = getRevisionHistoryVersions(advisory);
        assertEquals(expectedVersions, revisionHistoryVersions, message);
    }

    private List<String> getRevisionHistoryVersions(AdvisoryResponse advisory) {
        List<String> versionNumbers = new ArrayList<>();
        advisory.getCsaf().at("/document/tracking/revision_history").forEach(
                revHistElem -> versionNumbers.add(revHistElem.at("/number").asText())
            );
        return versionNumbers;
    }

    private void assertRevisionHistorySummariesNonEmpty(AdvisoryResponse advisory) {
        advisory.getCsaf().at("/document/tracking/revision_history").forEach(
                revHistoryNode -> assertNotEquals("", revHistoryNode.get("summary").asText(),
                        "summary must not be empty!")
        );
    }

}
