package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.AUTHOR;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.*;
import static de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus.Draft;
import static de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus.Final;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
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
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test for the workflow in the Advisory service. The required CouchDB container is started in the CouchDBExtension.
 */
@SpringBootTest
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@ContextConfiguration
public class AdvisoryWorkflowSemanticVersioningTest {

    private static final String EMPTY_PASSWD = "";

    @Autowired
    private AdvisoryService advisoryService;

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void addAdvisoryTest() throws IOException, DatabaseException, CsafException {

        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());

        String dateNowMinutes = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).substring(0, 16);
        assertThat(advisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.1"));
        assertThat(advisory.getCsaf().at("/document/tracking/status").asText(), equalTo("draft"));
        assertThat(advisory.getCsaf().at("/document/tracking/current_release_date").asText(), startsWith(dateNowMinutes));
    }

    @Test
    @WithMockUser(username = "reviewer1", authorities = {CsafRoles.ROLE_REVIEWER})
    public void addAdvisoryTest_InvalidRole() {

        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        Assertions.assertThrows(AccessDeniedException.class, () -> advisoryService.addAdvisory(csafToRequest(csafJson)));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void readAdvisoryTest_AuthorOwn() throws IOException, DatabaseException, CsafException {

        final String userName = "author1";
        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisoryForCredentials(csafToRequest(csafJson), createAuthentication(userName, AUTHOR.getRoleName()));
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(advisory.isChangeable(), is(true));
        assertThat(advisory.isDeletable(), is(true));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void readAdvisoryTest_AuthorNotOwn() throws IOException, CsafException {

        final String advisoryUser = "John";
        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisoryForCredentials(csafToRequest(csafJson), createAuthentication(advisoryUser, AUTHOR.getRoleName()));
        assertThrows(CsafException.class, () -> advisoryService.getAdvisory(idRev.getId()));
    }

    @Test
    @WithMockUser(username = "editor1", authorities = {CsafRoles.ROLE_EDITOR})
    public void readAdvisoryTest_EditorNotOwn() throws IOException, DatabaseException, CsafException {

        final String advisoryUser = "John";
        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisoryForCredentials(csafToRequest(csafJson), createAuthentication(advisoryUser, AUTHOR.getRoleName()));
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());

        assertThat(advisory.isChangeable(), is(true));
        assertThat(advisory.isDeletable(), is(true));
    }

    @Test
    @WithMockUser(username = "editor1", authorities = { CsafRoles.ROLE_EDITOR})
    public void workflowTest_disallowedStateChange() throws IOException, CsafException {

        final String advisoryUser = "John";
        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisoryForCredentials(csafToRequest(csafJson), createAuthentication(advisoryUser, AUTHOR.getRoleName()));

        assertThrows(CsafException.class, () -> advisoryService.changeAdvisoryWorkflowState(idRev.getId(), idRev.getRevision(), WorkflowState.Published, null, null));
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void workflowTest() throws IOException, DatabaseException, CsafException {
        try (final MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class)) {
            final String csafJson = csafJsonCategoryTitleId("Category1", "Title1", "TrackingOne");
            IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
            var readAdvisory = advisoryService.getAdvisory(idRev.getId());
            ((ObjectNode) readAdvisory.getCsaf().at("/document")).put("title", "UpdatedTitle");
            CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
            request.setSummary("UpdateSummary");
            String revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);

            validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);
            advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
            List<AdvisoryInformationResponse> advisories = advisoryService.getAdvisoryInformations(null);
            assertThat(advisories.size(), is(1));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            SwitchUserGrantedAuthority auditorAuthority = new SwitchUserGrantedAuthority(CsafRoles.ROLE_AUDITOR, auth);

            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("auditor", null, Collections.singletonList(auditorAuthority)));
            // the advisory and on backup version of the advisory
            // only auditor can see all versions
            List<AdvisoryInformationResponse> advisoriesAuditor = advisoryService.getAdvisoryInformations(null);
            assertThat(advisoriesAuditor.size(), is(2));
        }
    }

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
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
        advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Draft, null, null);

        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.0-1.1"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(4));
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    public void workflowTest_revisionHistory() throws IOException, DatabaseException, CsafException {
        try (final MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class)) {
            // create advisory
            final String csafJson = csafJsonCategoryTitleId("Category1", "Title1", "TrackingOne");
            IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
            var readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.1"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("0.0.1"));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            //update advisory
            CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
            request.setSummary("UpdateSummary");
            String revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.2"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("0.0.1"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("0.0.2"));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow to review
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.2"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow to approved
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.0-1.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("0.0.1"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/2/number").asText(), equalTo("1.0.0-1.0"));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

            // workflow to RfPublication
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.0-1.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow to Published
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(1));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow new Version
            advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1-1.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("1.0.1-1.0"));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            //update advisory 2
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
            request.setSummary("UpdateSummary");
            revision = advisoryService.updateAdvisory(idRev.getId(), readAdvisory.getRevision(), request);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1-1.1"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("1.0.1-1.1"));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow to review 2
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1-1.1"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow to approved 2
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1-2.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("1.0.1-2.0"));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow to RfPublication 2
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1-2.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow to Published 2
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("1.0.1"));
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            // workflow new Version
            advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.2-1.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("1.0.1"));
            assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/2/number").asText(), equalTo("1.0.2-1.0"));
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

            final String csafJson = csafMinimalValidDoc(Draft, "0.0.1");
            IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));

            AdvisoryResponse currentAdvisory = advisoryService.getAdvisory(idRev.getId());
            ((ObjectNode) currentAdvisory.getCsaf().at("/document")).put("title", "Pre-release Title");
            CreateAdvisoryRequest request = csafToRequest(currentAdvisory.getCsaf().toPrettyString());
            request.setSummary("updated title in pre-release draft");
            String revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request);

            AdvisoryResponse readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0.0.1", "0.0.2"),
                    "in pre-release stage change of title should trigger a patch version raise");

            currentAdvisory = advisoryService.getAdvisory(idRev.getId());
            ObjectNode emptyProductTree = jacksonMapper.createObjectNode();
            ((ObjectNode) currentAdvisory.getCsaf()).set("product_tree", emptyProductTree);
            request = csafToRequest(currentAdvisory.getCsaf().toPrettyString());
            request.setSummary("added empty product_tree");
            revision = advisoryService.updateAdvisory(idRev.getId(), revision, request);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0.0.1", "0.0.2", "0.1.0"),
                    "in pre-release stage change of product_tree should trigger a minor version raise");

            // going through multiple workflow state changes should add revision history elements for
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Draft, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0.0.1", "0.0.2", "0.1.0", "0.1.0-1.0"),
                    "going back from Review to Draft should add pre-release counter");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0.0.1", "0.0.2", "0.1.0", "0.1.0-1.0", "1.0.0-1.0"),
                    "going to Approved should raise major version");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Draft, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0.0.1", "0.0.2", "0.1.0", "0.1.0-1.0", "1.0.0-1.0", "1.0.0-1.1"),
                    "going back from Approved to Draft should increment second part of pre-release counter");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("0.0.1", "0.0.2", "0.1.0", "0.1.0-1.0", "1.0.0-1.0", "1.0.0-1.1", "1.0.0-2.0"),
                    "going to Approved should increment pre-release counter");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertEquals(7, readAdvisory.getCsaf().at("/document/tracking/revision_history").size(),
                    "going to RfPublication should not add revision history element");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1.0.0"),
                    "Publishing the advisory for the first time should delete all prerelease version entries and set version 1.0.0");

            revision = advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1.0.0", "1.0.1-1.0"),
                    "creating new version should raise patch version and add pre-release counter");

            currentAdvisory = advisoryService.getAdvisory(idRev.getId());
            ((ObjectNode) currentAdvisory.getCsaf().at("/document")).put("title", "Updated Title After Release");
            request = csafToRequest(currentAdvisory.getCsaf().toPrettyString());
            request.setSummary("updated title after release");
            revision = advisoryService.updateAdvisory(idRev.getId(), revision, request);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1.0.0", "1.1.0-1.1"),
                    "after release change of title should trigger minor version raise, including pre-release part");

            currentAdvisory = advisoryService.getAdvisory(idRev.getId());
            ((ObjectNode) currentAdvisory.getCsaf()).remove("product_tree");
            request = csafToRequest(currentAdvisory.getCsaf().toPrettyString());
            request.setSummary("removed product_tree");
            revision = advisoryService.updateAdvisory(idRev.getId(), revision, request);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1.0.0", "2.0.0-1.2"),
                    "after release stage change of product_tree should trigger a major version raise");

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Draft, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Draft, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1.0.0", "2.0.0-3.0"),
                    "after release stage workflow changes should only update the pre-release part of the existing revision history element");
            assertRevisionHistorySummariesNonEmpty(readAdvisory);

            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);

            readAdvisory = advisoryService.getAdvisory(idRev.getId());
            assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1.0.0", "2.0.0"),
                    "publishing the advisory should remove the pre-release part");

            assertEquals("removed product_tree",
                    readAdvisory.getCsaf().at("/document/tracking/revision_history/1/summary").asText(),
                    "The last revision history element's summary should be copied/kept since the last change. " +
                    "Workflow state changes should not edit the summary after initial publication");

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

            try (final InputStream csafStream = csafToInputstream(csafMinimalValidDoc(Final, "1.0.0"))) {
                final JsonNode csafRootNode = jacksonMapper.readValue(csafStream, JsonNode.class);
                // 1.Import
                IdAndRevision idRev = advisoryService.importAdvisory(csafRootNode);
                // 2. Create new Dokument
                advisoryService.createNewCsafDocumentVersion(idRev.getId(), idRev.getRevision());
                AdvisoryResponse readAdvisory = advisoryService.getAdvisory(idRev.getId());
                assertRevisionHistoryVersionsMatch(readAdvisory, List.of("1.0.0", "1.0.1-1.0"),
                        "creating new version should raise patch version and add pre-release counter");
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


    private Authentication createAuthentication(String userName, String... roles) {

        var authority = new SimpleGrantedAuthority(roles[0]);
        var principal = new User(userName, EMPTY_PASSWD, singletonList(authority));
        return new TestingAuthenticationToken(principal, null, roles);
    }
}
