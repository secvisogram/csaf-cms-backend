package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.*;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonTitle;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AdvisoryWorkflowUtilTest {

    private static final String EMPTY_PASSWD = "";

    @Test
    public void canDeleteAdvisoryTest_registeredDraftOwn() throws IOException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_authorDraftOwn() throws IOException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(true));
    }

    @Test
    public void canDeleteAdvisoryTest_authorDraftNotOwn() throws IOException {

        var userName = "John";
        var otherName = "Jack";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(otherName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_authorNotDraftOwn() throws IOException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        wrapper.setWorkflowState(WorkflowState.Approved);
        Authentication credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_editorDraftNotOwn() throws IOException {

        var userName = "John";
        var otherName = "Jack";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        Authentication credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(true));
    }

    @Test
    public void canDeleteAdvisoryTest_editorNotDraftOwn() throws IOException {

        var userName = "John";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        wrapper.setWorkflowState(WorkflowState.Approved);
        Authentication credentials = createAuthentication(userName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(false));
    }

    @Test
    public void canDeleteAdvisoryTest_managerNotDraftNotOwn() throws IOException {

        var userName = "John";
        var otherName = "Jack";
        var wrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("The Title"), userName);
        wrapper.setWorkflowState(WorkflowState.Approved);
        Authentication credentials = createAuthentication(otherName, MANAGER);
        assertThat(AdvisoryWorkflowUtil.canDeleteAdvisory(wrapper, credentials), is(true));
    }

    @Test
    public void canViewComment_registeredDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Draft).setOwner(userName);
        var credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(false));
    }

    @Test
    public void canViewComment_authorDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Draft).setOwner(userName);
        var credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(true));
    }

    @Test
    public void canViewComment_authorDraftNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Draft).setOwner(userName);
        var credentials = createAuthentication(otherName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(false));
    }

    @Test
    public void canViewComment_editorDraftNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Draft).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(true));
    }

    @Test
    public void canViewComment_editorReviewNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Review).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canViewComment(advisory, credentials), is(false));
    }

    @Test
    public void canViewComment_reviewerReviewNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Review).setOwner(userName);
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
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Draft).setOwner(userName);
        var credentials = createAuthentication(userName, REGISTERED);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canAddComment_authorDraftOwn() {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Draft).setOwner(userName);
        var credentials = createAuthentication(userName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canAddComment_authorDraftNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Draft).setOwner(userName);
        var credentials = createAuthentication(otherName, AUTHOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canAddComment_editorDraftNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Draft).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(true));
    }

    @Test
    public void canAddComment_editorReviewNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Review).setOwner(userName);
        var credentials = createAuthentication(otherName, EDITOR);
        assertThat(AdvisoryWorkflowUtil.canAddAndReplyCommentToAdvisory(advisory, credentials), is(false));
    }

    @Test
    public void canAddComment_reviewerReviewNotOwn()  {

        var userName = "John";
        var otherName = "Jack";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(WorkflowState.Review).setOwner(userName);
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

    /**
     * Create authentication
     * @param userName the Name of the user
     * @param role the role of the user
     * @return the created authentication
     */
    private Authentication createAuthentication(String userName, CsafRoles.Role role) {

        var authority = new SimpleGrantedAuthority(role.getRoleName());
        var principal =  new User(userName, EMPTY_PASSWD, singletonList(authority));
        return new TestingAuthenticationToken(principal, null, role.getRoleName());
    }
}
