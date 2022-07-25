package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.*;
import static de.bsi.secvisogram.csaf_cms_backend.json.VersioningType.Semantic;
import static de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState.*;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AdvisoryWorkflowUtilTest {

    private static final String EMPTY_PASSWD = "";

    @Test
    public void canDeleteAdvisoryTest() {

        final boolean own = true;
        final boolean notOwn = false;
        canDeleteAdvisory(REGISTERED, own, Draft, false);
        canDeleteAdvisory(AUTHOR, own, Draft, true);
        canDeleteAdvisory(AUTHOR, notOwn, Draft, false);
        canDeleteAdvisory(AUTHOR, notOwn, Approved, false);
        canDeleteAdvisory(EDITOR, notOwn, Draft, true);
        canDeleteAdvisory(EDITOR, notOwn, Approved, false);
        canDeleteAdvisory(MANAGER, notOwn, Approved, true);
    }

    private void canDeleteAdvisory(Role role, boolean own, WorkflowState advisoryState, boolean canDelete) {

        var userName = "John";
        Authentication credentials = createCredentials(role, userName, own);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(userName, advisoryState, credentials), is(canDelete));
    }

    @Test
    public void canChangeAdvisoryTest() {

        final boolean own = true;
        final boolean notOwn = false;
        canChangeAdvisory(REGISTERED, own, Draft, false);
        canChangeAdvisory(AUTHOR, own, Draft, true);
        canChangeAdvisory(AUTHOR, notOwn, Draft, false);
        canChangeAdvisory(AUTHOR, notOwn, Approved, false);
        canChangeAdvisory(EDITOR, notOwn, Draft, true);
        canChangeAdvisory(EDITOR, notOwn, Approved, false);
        canChangeAdvisory(MANAGER, notOwn, Approved, false);
    }

    private void canChangeAdvisory(Role role, boolean own, WorkflowState advisoryState, boolean canChange) {

        var userName = "John";
        Authentication credentials = createCredentials(role, userName, own);
        assertThat(AdvisoryWorkflowUtil.canChangeAdvisory(userName, advisoryState, credentials), is(canChange));
    }

    @Test
    public void canViewAdvisoryTest() {

        final boolean own = true;
        final boolean notOwn = false;
        String dateInPast = DateTimeFormatter.ISO_INSTANT.format(Instant.now().minus(1, ChronoUnit.HOURS));
        String dateInFuture = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plus(1, ChronoUnit.HOURS));


        canViewAdvisory(REGISTERED, own, Draft, null, false);
        canViewAdvisory(REGISTERED, own, Published, dateInPast, true);
        canViewAdvisory(REGISTERED, own, Published, dateInFuture, false);
        canViewAdvisory(AUTHOR, own, Draft, null, true);
        canViewAdvisory(AUTHOR, notOwn, Draft, null, false);
        canViewAdvisory(AUTHOR, notOwn, Approved, null, false);
        canViewAdvisory(AUTHOR, own, Approved, null, true);
        canViewAdvisory(AUTHOR, own, RfPublication, null, true);
        canViewAdvisory(EDITOR, notOwn, Draft, null, true);
        canViewAdvisory(EDITOR, notOwn, Approved, null, true);
        canViewAdvisory(EDITOR, notOwn, RfPublication, null, true);
        canViewAdvisory(MANAGER, notOwn, Approved, null, true);
    }

    private void canViewAdvisory(Role role, boolean own, WorkflowState advisoryState, String releaseDate, boolean canView) {

        var userName = "John";
        Authentication credentials = createCredentials(role, userName, own);
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(userName, advisoryState, credentials, releaseDate), is(canView));
    }

    @Test
    public void canViewComment_registeredDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        var credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(false));
    }

    @Test
    public void canViewComment_authorDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        var credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(true));
    }

    @Test
    public void canViewComment_authorDraftNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        var credentials = createAuthentication(otherName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(false));
    }

    @Test
    public void canViewComment_editorDraftNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(true));
    }

    @Test
    public void canViewComment_editorReviewNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Review).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(false));
    }

    @Test
    public void canViewComment_reviewerReviewNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Review).setOwner(userName);
        var credentials = createAuthentication(otherName, REVIEWER);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(true));
    }

    @Test
    public void canViewComment_reviewerPublishedNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Published).setOwner(userName);
        var credentials = createAuthentication(otherName, REVIEWER);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(false));
    }

    @Test
    public void canViewComment_auditorPublishedNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Published).setOwner(userName);
        var credentials = createAuthentication(otherName, AUDITOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(false));
    }

    @Test
    public void canViewComment_auditorApprovedNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Approved).setOwner(userName);
        var credentials = createAuthentication(otherName, AUDITOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(true));
    }

    @Test
    public void canAddComment_registeredDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        var credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canAddComment_authorDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        var credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canAddComment_authorDraftNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        var credentials = createAuthentication(otherName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canAddComment_editorDraftNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canAddComment_editorReviewNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Review).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canAddComment_reviewerReviewNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Review).setOwner(userName);
        var credentials = createAuthentication(otherName, REVIEWER);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canAddComment_reviewerPublishedNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Published).setOwner(userName);
        var credentials = createAuthentication(otherName, REVIEWER);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canAddComment_auditorPublishedNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Published).setOwner(userName);
        var credentials = createAuthentication(otherName, AUDITOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canAddComment_auditorApprovedNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Approved).setOwner(userName);
        var credentials = createAuthentication(otherName, AUDITOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canCreateNewVersionTest()  {

        final boolean own = true;
        final boolean notOwn = false;

        canCreateNewVersion(REGISTERED, own, Draft, false);
        canCreateNewVersion(AUTHOR, own, Draft, false);
        canCreateNewVersion(AUTHOR, notOwn, Draft, false);
        canCreateNewVersion(AUTHOR, notOwn, Approved, false);
        canCreateNewVersion(AUTHOR, own, Published, true);
        canCreateNewVersion(AUTHOR, notOwn, Published, false);
        canCreateNewVersion(EDITOR, notOwn, Draft, false);
        canCreateNewVersion(EDITOR, notOwn, Approved, false);
        canCreateNewVersion(EDITOR, own, Published, true);
        canCreateNewVersion(EDITOR, notOwn, Published, true);
        canCreateNewVersion(MANAGER, notOwn, Approved, false);
        canCreateNewVersion(MANAGER, notOwn, Published, true);
    }


    private void canCreateNewVersion(Role role, boolean own, WorkflowState advisoryState, boolean canDelete) {

        var userName = "John";
        Authentication credentials = createCredentials(role, userName, own);
        assertThat(AdvisoryWorkflowUtil.canCreateNewVersion(userName, advisoryState, credentials), is(canDelete));
    }


    @Test
    public void canChangeWorkflows() {

        final boolean own = true;
        final boolean notOwn = false;

        canChangeWorkflow(Draft, Draft, AUTHOR, own, false);
        canChangeWorkflow(Draft, Review, AUTHOR, own, true);
        canChangeWorkflow(Draft, Review, AUTHOR, notOwn, false);
        canChangeWorkflow(Draft, Approved, AUTHOR, own, false);
        canChangeWorkflow(Draft, RfPublication, AUTHOR, own, false);
        canChangeWorkflow(Draft, Published, AUTHOR, own, false);
        canChangeWorkflow(Review, Draft, AUTHOR, own, false);
        canChangeWorkflow(Review, Approved, AUTHOR, own, false);
        canChangeWorkflow(Review, RfPublication, AUTHOR, own, false);
        canChangeWorkflow(Review, Published, AUTHOR, own, false);
        canChangeWorkflow(Approved, Draft, AUTHOR, own, false);
        canChangeWorkflow(Approved, Review, AUTHOR, own, false);
        canChangeWorkflow(Approved, RfPublication, AUTHOR, own, true);
        canChangeWorkflow(Approved, RfPublication, AUTHOR, notOwn, false);
        canChangeWorkflow(Approved, Published, AUTHOR, own, false);
        canChangeWorkflow(RfPublication, Draft, AUTHOR, own, false);
        canChangeWorkflow(RfPublication, Review, AUTHOR, own, false);
        canChangeWorkflow(RfPublication, Approved, AUTHOR, own, false);
        canChangeWorkflow(RfPublication, Published, AUTHOR, own, false);
        canChangeWorkflow(Published, Draft, AUTHOR, own, false);
        canChangeWorkflow(Published, Review, AUTHOR, own, false);
        canChangeWorkflow(Published, Approved, AUTHOR, own, false);
        canChangeWorkflow(Published, RfPublication, AUTHOR, own, false);

        canChangeWorkflow(Draft, Draft, EDITOR, own, false);
        canChangeWorkflow(Draft, Review, EDITOR, own, true);
        canChangeWorkflow(Draft, Review, EDITOR, notOwn, true);
        canChangeWorkflow(Draft, Approved, EDITOR, own, false);
        canChangeWorkflow(Draft, RfPublication, EDITOR, own, false);
        canChangeWorkflow(Draft, Published, EDITOR, own, false);
        canChangeWorkflow(Review, Draft, EDITOR, own, false);
        canChangeWorkflow(Review, Approved, EDITOR, own, false);
        canChangeWorkflow(Review, RfPublication, EDITOR, own, false);
        canChangeWorkflow(Review, Published, EDITOR, own, false);
        canChangeWorkflow(Approved, Draft, EDITOR, own, false);
        canChangeWorkflow(Approved, Review, EDITOR, own, false);
        canChangeWorkflow(Approved, RfPublication, EDITOR, own, true);
        canChangeWorkflow(Approved, RfPublication, EDITOR, notOwn, true);
        canChangeWorkflow(Approved, Published, EDITOR, own, false);
        canChangeWorkflow(RfPublication, Draft, EDITOR, own, false);
        canChangeWorkflow(RfPublication, Review, EDITOR, own, false);
        canChangeWorkflow(RfPublication, Approved, EDITOR, own, false);
        canChangeWorkflow(RfPublication, Published, EDITOR, own, false);
        canChangeWorkflow(Published, Draft, EDITOR, own, false);
        canChangeWorkflow(Published, Review, EDITOR, own, false);
        canChangeWorkflow(Published, Approved, EDITOR, own, false);
        canChangeWorkflow(Published, RfPublication, EDITOR, own, false);


        canChangeWorkflow(Draft, Draft, REVIEWER, own, false);
        canChangeWorkflow(Draft, Review, REVIEWER, own, false);
        canChangeWorkflow(Draft, Review, REVIEWER, notOwn, false);
        canChangeWorkflow(Draft, Approved, REVIEWER, own, false);
        canChangeWorkflow(Draft, RfPublication, REVIEWER, own, false);
        canChangeWorkflow(Draft, Published, REVIEWER, own, false);
        canChangeWorkflow(Review, Draft, REVIEWER, own, true);
        canChangeWorkflow(Review, Approved, REVIEWER, own, true);
        canChangeWorkflow(Review, RfPublication, REVIEWER, own, false);
        canChangeWorkflow(Review, Published, REVIEWER, own, false);
        canChangeWorkflow(Approved, Draft, REVIEWER, own, false);
        canChangeWorkflow(Approved, Review, REVIEWER, own, false);
        canChangeWorkflow(Approved, RfPublication, REVIEWER, own, false);
        canChangeWorkflow(Approved, Published, REVIEWER, own, false);
        canChangeWorkflow(RfPublication, Draft, REVIEWER, own, false);
        canChangeWorkflow(RfPublication, Review, REVIEWER, own, false);
        canChangeWorkflow(RfPublication, Approved, REVIEWER, own, false);
        canChangeWorkflow(RfPublication, Published, REVIEWER, own, false);
        canChangeWorkflow(Published, Draft, REVIEWER, own, false);
        canChangeWorkflow(Published, Review, REVIEWER, own, false);
        canChangeWorkflow(Published, Approved, REVIEWER, own, false);
        canChangeWorkflow(Published, RfPublication, REVIEWER, own, false);

        canChangeWorkflow(Draft, Draft, PUBLISHER, own, false);
        canChangeWorkflow(Draft, Review, PUBLISHER, own, true);
        canChangeWorkflow(Draft, Review, PUBLISHER, notOwn, true);
        canChangeWorkflow(Draft, Approved, PUBLISHER, own, false);
        canChangeWorkflow(Draft, RfPublication, PUBLISHER, own, false);
        canChangeWorkflow(Draft, Published, PUBLISHER, own, false);
        canChangeWorkflow(Review, Draft, PUBLISHER, own, false);
        canChangeWorkflow(Review, Approved, PUBLISHER, own, false);
        canChangeWorkflow(Review, RfPublication, PUBLISHER, own, false);
        canChangeWorkflow(Review, Published, PUBLISHER, own, false);
        canChangeWorkflow(Approved, Draft, PUBLISHER, own, true);
        canChangeWorkflow(Approved, Review, PUBLISHER, own, false);
        canChangeWorkflow(Approved, RfPublication, PUBLISHER, own, true);
        canChangeWorkflow(Approved, Published, PUBLISHER, own, false);
        canChangeWorkflow(RfPublication, Draft, PUBLISHER, own, false);
        canChangeWorkflow(RfPublication, Review, PUBLISHER, own, false);
        canChangeWorkflow(RfPublication, Approved, PUBLISHER, own, false);
        canChangeWorkflow(RfPublication, Published, PUBLISHER, own, true);
        canChangeWorkflow(Published, Draft, PUBLISHER, own, false);
        canChangeWorkflow(Published, Review, PUBLISHER, own, false);
        canChangeWorkflow(Published, Approved, PUBLISHER, own, false);
        canChangeWorkflow(Published, RfPublication, PUBLISHER, own, false);

    }


     private void canChangeWorkflow(WorkflowState from, WorkflowState to, Role role, boolean own, boolean canChange)  {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(from).setOwner(userName);
        var credentials = createCredentials(role, userName, own);
        assertThat(AdvisoryWorkflowUtil.canChangeWorkflow(advisory, to, credentials), is(canChange));
    }

    private Authentication createCredentials(Role role, String userName, boolean own) {

        var otherName = "Jack";
        String[] allRoles = {role.getRoleName()};
        if (role == EDITOR) {
            allRoles = new String[]{AUTHOR.getRoleName(), EDITOR.getRoleName()};
        } else if (role == PUBLISHER) {
            allRoles = new String[]{AUTHOR.getRoleName(), EDITOR.getRoleName(), PUBLISHER.getRoleName()};
        } else if (role == MANAGER) {
            allRoles = new String[]{AUTHOR.getRoleName(), EDITOR.getRoleName(), PUBLISHER.getRoleName(), MANAGER.getRoleName()};
        }

        return (own) ? createAuthentication(userName, allRoles) : createAuthentication(otherName, allRoles);
    }

    @Test
    public void getChangeTypeTest_changeDocTitle() throws IOException, CsafException {

        String releaseDate = (DateTimeFormatter.ISO_INSTANT.format(Instant.now().plus(1, ChronoUnit.HOURS)));
        String oldCsafJson = CsafDocumentJsonCreator.csafJsonTitleReleaseDate("Title1", releaseDate);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.csafJsonTitleReleaseDate("Title2", releaseDate);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory), is(PatchType.PATCH));
    }

    @Test
    public void getChangeTypeTest_addVulnerabilityCve() throws IOException, CsafException {

        String oldVul = """
                 [ { "cve": "cve1"
                   }
                  ]
                """;

        String newVul = """
                 [ { "cve": "cve1"
                   },
                   { "cve": "cve2"
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_removeVulnerabilityCve() throws IOException, CsafException {

        String oldVul = """
                 [ { "cve": "cve1"
                   },
                   { "cve": "cve2"
                   }
                  ]
                """;

        String newVul = """
                 [ { "cve": "cve1"
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));
        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_addVulnerabilityProductStatusFirstAffected1() throws IOException, CsafException {

        String oldVul = """
                 [
                  ]
                """;

        String newVul = """
                 [ { "product_status": {
                      "first_affected": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_addVulnerabilityProductStatusFirstAffected() throws IOException, CsafException {

        String oldVul = """
                 [ { "product_status": {
                      "known_affected": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;

        String newVul = """
                 [ { "product_status": {
                      "known_affected": ["CSAFPID-0001", "CSAFPID-0002"]
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_changeProductTree() throws IOException, CsafException {

        String oldTree = """
                 {
                   "full_product_names": [
                     {
                       "product_id": "%s",
                       "name": "Exxcellent CSAF",
                       "product_identification_helper": {
                          "cpe": "cpe"
                       }
                     }
                   ]
                 }
                """;

        String newTree = """
                  {
                   "full_product_names": [
                     {
                       "product_id": "%s",
                       "name": "Exxcellent CHANGED",
                       "product_identification_helper": {
                          "cpe": "cpe"
                       }
                     }
                   ]
                 }
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithProductTree(oldTree);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithProductTree(newTree);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory), is(PatchType.MAJOR));
    }

    /**
     * Create authentication
     * @param userName the Name of the user
     * @param role the role of the user
     * @return the created authentication
     */
    private Authentication createAuthentication(String userName, Role role) {

        var authority = new SimpleGrantedAuthority(role.getRoleName());
        var principal =  new User(userName, EMPTY_PASSWD, singletonList(authority));
        return new TestingAuthenticationToken(principal, null, role.getRoleName());
    }

    private Authentication createAuthentication(String userName, String ... roles) {

        var authority = new SimpleGrantedAuthority(roles[0]);
        var principal =  new User(userName, EMPTY_PASSWD, singletonList(authority));
        return new TestingAuthenticationToken(principal, null, roles);
    }

}
