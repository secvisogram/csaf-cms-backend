package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.*;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonTitle;
import static de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState.*;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
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
    public void canDeleteAdvisoryTest_registeredDraftOwn() throws IOException, CsafException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_authorDraftOwn() throws IOException, CsafException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(true));
    }

    @Test
    public void canDeleteAdvisoryTest_authorDraftNotOwn() throws IOException, CsafException {

        var userName = "John";
        var otherName = "Jack";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(otherName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_authorNotDraftOwn() throws IOException, CsafException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        wrapper.setWorkflowState(WorkflowState.Approved);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_editorDraftNotOwn() throws IOException, CsafException {

        var userName = "John";
        var otherName = "Jack";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(true));
    }

    @Test
    public void canDeleteAdvisoryTest_editorNotDraftOwn() throws IOException, CsafException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        wrapper.setWorkflowState(WorkflowState.Approved);
        Authentication credentials = createAuthentication(userName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_managerNotDraftNotOwn() throws IOException, CsafException {

        var userName = "John";
        var otherName = "Jack";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        wrapper.setWorkflowState(WorkflowState.Approved);
        Authentication credentials = createAuthentication(otherName, MANAGER);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(true));
    }

    @Test
    public void canChangeAdvisoryTest_registeredDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        Authentication credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canChangeAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canChangeAdvisoryTest_authorDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canChangeAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canChangeAdvisoryTest_authorDraftNotOwn() {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        Authentication credentials = createAuthentication(otherName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canChangeAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canChangeAdvisoryTest_authorNotDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Approved).setOwner(userName);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canChangeAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canChangeAdvisoryTest_editorDraftNotOwn() {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        Authentication credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canChangeAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canChangeAdvisoryTest_editorNotDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Approved).setOwner(userName);
        var credentials = createAuthentication(userName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canChangeAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canChangeAdvisoryTest_managerNotDraftNotOwn() {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Approved).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR.getRoleName(), MANAGER.getRoleName());
        assertThat(AdvisoryWorkflowUtil.canChangeAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canViewAdvisoryTest_registeredDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        Authentication credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canViewAdvisoryTest_registeredPublishedTimeInPast() {

        String nowMinus1h = DateTimeFormatter.ISO_INSTANT.format(Instant.now().minus(1, ChronoUnit.HOURS));
        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Published).setOwner(userName)
                .setCurrentReleaseDate(nowMinus1h);
        Authentication credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canViewAdvisoryTest_registeredPublishedTimeInFuture() {

        String nowPlus1h = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plus(1, ChronoUnit.HOURS));
        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Published).setOwner(userName)
                .setCurrentReleaseDate(nowPlus1h);
        Authentication credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canViewAdvisoryTest_authorDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canViewAdvisoryTest_authorDraftNotOwn() {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        Authentication credentials = createAuthentication(otherName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canViewAdvisoryTest_authorNotDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Approved).setOwner(userName);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canViewAdvisoryTest_editorDraftNotOwn() {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        Authentication credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canViewAdvisoryTest_editorApprovedOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Approved).setOwner(userName);
        var credentials = createAuthentication(userName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canViewAdvisoryTest_managerApprovedNotOwn() {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Approved).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR.getRoleName(), PUBLISHER.getRoleName(), MANAGER.getRoleName());
        assertThat(AdvisoryWorkflowUtil.canViewAdvisory(advisory, credentials), is(true));
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
    public void canCreateNewVersion_draftAuthorOwn()  {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Draft).setOwner(userName);
        createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canCreateNewVersion(advisory), is(false));
    }

    @Test
    public void canCreateNewVersion_PublishedAuthorOwn()  {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(Published).setOwner(userName);
        createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canCreateNewVersion(advisory), is(true));
    }


    @Test
    public void canChangeWorkflows() {

        final boolean own = true;
        final boolean notOown = false;

        canChangeWorkflow(Draft, Draft, AUTHOR, own, false);
        canChangeWorkflow(Draft, Review, AUTHOR, own, true);
        canChangeWorkflow(Draft, Review, AUTHOR, notOown, false);
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
        canChangeWorkflow(Approved, Published, AUTHOR, own, false);
        canChangeWorkflow(RfPublication, Draft, AUTHOR, own, false);
        canChangeWorkflow(RfPublication, Review, AUTHOR, own, false);
        canChangeWorkflow(RfPublication, Approved, AUTHOR, own, false);
        canChangeWorkflow(RfPublication, Published, AUTHOR, own, false);
        canChangeWorkflow(Published, Draft, AUTHOR, own, false);
        canChangeWorkflow(Published, Review, AUTHOR, own, false);
        canChangeWorkflow(Published, Approved, AUTHOR, own, false);
        canChangeWorkflow(Published, RfPublication, AUTHOR, own, false);

        canChangeWorkflow(Draft, Draft, REVIEWER, own, false);
        canChangeWorkflow(Draft, Review, REVIEWER, own, false);
        canChangeWorkflow(Draft, Review, REVIEWER, notOown, false);
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
        canChangeWorkflow(Draft, Review, PUBLISHER, notOown, true);
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
        var otherName = "Jack";
        String[] allRoles = {role.getRoleName()};
        if (role == EDITOR) {
            allRoles = new String[]{AUTHOR.getRoleName(), EDITOR.getRoleName()};
        } else if (role == PUBLISHER) {
            allRoles = new String[]{AUTHOR.getRoleName(), EDITOR.getRoleName(), PUBLISHER.getRoleName()};
        } else if (role == MANAGER) {
            allRoles = new String[]{AUTHOR.getRoleName(), EDITOR.getRoleName(), PUBLISHER.getRoleName(), MANAGER.getRoleName()};
        }

        var advisory = new AdvisoryInformationResponse().setWorkflowState(from).setOwner(userName);
        var credentials = (own) ? createAuthentication(userName, allRoles) : createAuthentication(otherName, allRoles);
        assertThat(AdvisoryWorkflowUtil.canChangeWorkflow(advisory, to, credentials), is(canChange));
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
