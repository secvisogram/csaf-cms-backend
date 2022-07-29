package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.AUTHOR;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.*;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "False positives on multiline format strings")

public class AdvisoryWorkflowTest {

    private static final String EMPTY_PASSWD = "";

    @Autowired
    private AdvisoryService advisoryService;

    private static MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class);


    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void addAdvisoryTest() throws IOException, DatabaseException, CsafException {

        final String csafJson = csafJsonCategoryTitle("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());

        String dateNowMinutes =  DateTimeFormatter.ISO_INSTANT.format(Instant.now()).substring(0, 16);
        assertThat(advisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.1"));
        assertThat(advisory.getCsaf().at("/document/tracking/status").asText(), equalTo("draft"));
        assertThat(advisory.getCsaf().at("/document/tracking/current_release_date").asText(), startsWith(dateNowMinutes));
    }

    @Test
    @WithMockUser(username = "reviewer1", authorities = { CsafRoles.ROLE_REVIEWER})
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
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void workflowTest() throws IOException, DatabaseException, CsafException {

        final String csafJson = csafJsonCategoryTitleId("Category1", "Title1", "TrackingOne");
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        var readAdvisory = advisoryService.getAdvisory(idRev.getId());
        ((ObjectNode) readAdvisory.getCsaf().at("/document")).put("title", "UpdatedTitle");
        CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
        request.setSummary("UpdateSummary");
        String revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request);
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);

        validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);
        advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
        List<AdvisoryInformationResponse> advisories = advisoryService.getAdvisoryInformations(null);
        assertThat(advisories.size(), is(1));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SwitchUserGrantedAuthority auditorAuthority =  new SwitchUserGrantedAuthority(CsafRoles.ROLE_AUDITOR, auth);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("auditor", null, Collections.singletonList(auditorAuthority)));
        // the advisory and on backup version of the adivsory
        // only auditor can see all versions
        List<AdvisoryInformationResponse> advisoriesAuditor = advisoryService.getAdvisoryInformations(null);
        assertThat(advisoriesAuditor.size(), is(2));
    }

    @Test
    @WithMockUser(username = "manager", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR,
            CsafRoles.ROLE_MANAGER, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void workflowTest_revisionHistory() throws IOException, DatabaseException, CsafException {

        // create advisory
        final String csafJson = csafJsonCategoryTitleId("Category1", "Title1", "TrackingOne");
        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        var readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.1"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("0.0.1"));

        //update advisory
        CreateAdvisoryRequest request = csafToRequest(readAdvisory.getCsaf().toPrettyString());
        request.setSummary("UpdateSummary");
        String revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), request);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.2"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("0.0.1"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("0.0.2"));

        // workflow to review
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.2"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));

        // workflow to approved
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.0-1.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("0.0.1"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/2/number").asText(), equalTo("1.0.0-1.0"));

        // workflow to RfPublication
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.0-1.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));

        // workflow to Published
        validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(1));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));

        // workflow new Version
        advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1-1.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("1.0.1-1.0"));

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

        // workflow to review 2
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1-1.1"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));

        // workflow to approved 2
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1-2.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("1.0.1-2.0"));

        // workflow to RfPublication 2
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1-2.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));

        // workflow to Published 2
        revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.1"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("1.0.1"));

        // workflow new Version
        advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
        readAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertThat(readAdvisory.getCsaf().at("/document/tracking/version").asText(), equalTo("1.0.2-1.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/0/number").asText(), equalTo("1.0.0"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/1/number").asText(), equalTo("1.0.1"));
        assertThat(readAdvisory.getCsaf().at("/document/tracking/revision_history/2/number").asText(), equalTo("1.0.2-1.0"));

    }


    private Authentication createAuthentication(String userName, String ... roles) {

        var authority = new SimpleGrantedAuthority(roles[0]);
        var principal =  new User(userName, EMPTY_PASSWD, singletonList(authority));
        return new TestingAuthenticationToken(principal, null, roles);
    }
}
