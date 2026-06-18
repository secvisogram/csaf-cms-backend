package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles.Role.*;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static de.bsi.secvisogram.csaf_cms_backend.json.VersioningType.Semantic;
import static de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState.*;
import static de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryWorkflowUtil.isSpellingMistake;
import static de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryWorkflowUtil.timestampIsBefore;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Draft, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Review, AUTHOR, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Review, AUTHOR, notOwn, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Approved, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, RfPublication, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Published, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Draft, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Approved, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, RfPublication, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Published, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Draft, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Review, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, RfPublication, AUTHOR, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, RfPublication, AUTHOR, notOwn, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Published, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Draft, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Review, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Approved, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Published, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Draft, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Review, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Approved, AUTHOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, RfPublication, AUTHOR, own, false);

        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Draft, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Review, EDITOR, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Review, EDITOR, notOwn, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Approved, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, RfPublication, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Published, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Draft, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Approved, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, RfPublication, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Published, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Draft, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Review, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, RfPublication, EDITOR, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, RfPublication, EDITOR, notOwn, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Published, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Draft, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Review, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Approved, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Published, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Draft, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Review, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Approved, EDITOR, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, RfPublication, EDITOR, own, false);

        // REVIEWER — own document: Review->Draft allowed, Review->Approved blocked (default flag=false)
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Draft, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Review, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Review, REVIEWER, notOwn, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Approved, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, RfPublication, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Published, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Draft, REVIEWER, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Draft, REVIEWER, notOwn, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Approved, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Approved, REVIEWER, notOwn, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, RfPublication, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Published, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Draft, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Review, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, RfPublication, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Published, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Draft, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Review, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Approved, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Published, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Draft, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Review, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Approved, REVIEWER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, RfPublication, REVIEWER, own, false);

        // PUBLISHER — inherits EDITOR+REVIEWER; own Review->Draft allowed, own Review->Approved blocked (default flag=false)
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Draft, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Review, PUBLISHER, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Review, PUBLISHER, notOwn, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Approved, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, RfPublication, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Draft, Published, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Draft, PUBLISHER, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Draft, PUBLISHER, notOwn, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Approved, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Approved, PUBLISHER, notOwn, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, RfPublication, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Review, Published, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Draft, PUBLISHER, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Review, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, RfPublication, PUBLISHER, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Approved, Published, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Draft, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Review, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Approved, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(RfPublication, Published, PUBLISHER, own, true);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Draft, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Review, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, Approved, PUBLISHER, own, false);
        canChangeWorkflowNotAllowOwnDocsApproved(Published, RfPublication, PUBLISHER, own, false);
    }

    /**
     * Tests workflow-change permissions when allowOwnDocumentsApproved = true.
     * In this mode, a Reviewer or Publisher IS allowed to approve their own advisory (Review -> Approved).
     */
    @Test
    public void canChangeWorkflows_allowOwnDocsApproved() {

        final boolean own = true;
        final boolean notOwn = false;

        // REVIEWER with own-approval flag enabled
        canChangeWorkflowAllowOwnDocsApproved(Review, Draft, REVIEWER, own, true);
        canChangeWorkflowAllowOwnDocsApproved(Review, Draft, REVIEWER, notOwn, true);
        canChangeWorkflowAllowOwnDocsApproved(Review, Approved, REVIEWER, own, true);   // allowed when flag=true
        canChangeWorkflowAllowOwnDocsApproved(Review, Approved, REVIEWER, notOwn, true);

        // PUBLISHER with own-approval flag enabled
        canChangeWorkflowAllowOwnDocsApproved(Review, Draft, PUBLISHER, own, true);
        canChangeWorkflowAllowOwnDocsApproved(Review, Draft, PUBLISHER, notOwn, true);
        canChangeWorkflowAllowOwnDocsApproved(Review, Approved, PUBLISHER, own, true);   // allowed when flag=true
        canChangeWorkflowAllowOwnDocsApproved(Review, Approved, PUBLISHER, notOwn, true);

        // All other transitions are unaffected by the flag
        canChangeWorkflowAllowOwnDocsApproved(Draft, Review, REVIEWER, own, false);
        canChangeWorkflowAllowOwnDocsApproved(Draft, Review, PUBLISHER, own, true);
        canChangeWorkflowAllowOwnDocsApproved(Approved, Draft, PUBLISHER, own, true);
        canChangeWorkflowAllowOwnDocsApproved(RfPublication, Published, PUBLISHER, own, true);
    }

    private void canChangeWorkflowNotAllowOwnDocsApproved(WorkflowState from, WorkflowState to, Role role, boolean own, boolean canChange) {

        canChangeWorkflow(from, to, role, own, canChange, false);
    }

    private void canChangeWorkflowAllowOwnDocsApproved(WorkflowState from, WorkflowState to, Role role, boolean own, boolean canChange) {

        canChangeWorkflow(from, to, role, own, canChange, true);
    }
    private void canChangeWorkflow(WorkflowState from, WorkflowState to, Role role, boolean own,
                                   boolean canChange, boolean allowOwnDocumentsApproved) {

        var userName = "John";
        var advisory = new AdvisoryInformationResponse().setWorkflowState(from).setOwner(userName);
        var credentials = createCredentials(role, userName, own);
        assertThat(AdvisoryWorkflowUtil.canChangeWorkflow(advisory, to, credentials, allowOwnDocumentsApproved),
                is(canChange));
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

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.PATCH));
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

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
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
        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_addVulnerabilityProductStatusFirstAffected() throws IOException, CsafException {

        String oldVul = """
                 [ { "product_status": {
                      "first_affected": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;

        String newVul = """
                 [ { "product_status": {
                      "first_affected": ["CSAFPID-0001", "CSAFPID-0002"]
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_addVulnerabilityProductStatusKnownAffected() throws IOException, CsafException {

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

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_addVulnerabilityProductStatusLastAffected() throws IOException, CsafException {

        String oldVul = """
                 [ { "product_status": {
                      "last_affected": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;

        String newVul = """
                 [ { "product_status": {
                      "last_affected": ["CSAFPID-0001", "CSAFPID-0002"]
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_removeVulnerabilityProductStatusFirstAffected() throws IOException, CsafException {

        String oldVul = """
                 [ { "product_status": {
                      "first_affected": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;

        String newVul = """
                 [ { "product_status": {
                      "first_affected": []
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_removeVulnerabilityProductStatusKnownAffected() throws IOException, CsafException {

        String oldVul = """
                 [ { "product_status": {
                      "known_affected": ["CSAFPID-0001", "CSAFPID-0002"]
                     }
                   }
                  ]
                """;

        String newVul = """
                 [ { "product_status": {
                      "known_affected": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_removeVulnerabilityProductStatusLastAffected() throws IOException, CsafException {

        String oldVul = """
                 [ { "product_status": {
                      "last_affected": ["CSAFPID-0001", "CSAFPID-0002"]
                     }
                   }
                  ]
                """;

        String newVul = """
                 [ { "product_status": {
                      "last_affected": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_removeVulnerabilityProductStatusFirstFixed() throws IOException, CsafException {

        String oldVul = """
                 [ { "product_status": {
                      "first_fixed": ["CSAFPID-0001", "CSAFPID-0002"]
                     }
                   }
                  ]
                """;

        String newVul =  """
                 [ { "product_status": {
                      "first_fixed": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_removeVulnerabilityProductStatusFixed() throws IOException, CsafException {

        String oldVul = """
                 [ { "product_status": {
                      "fixed": ["CSAFPID-0001", "CSAFPID-0002"]
                     }
                   }
                  ]
                """;

        String newVul = """
                 [ { "product_status": {
                      "fixed": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    @Test
    public void getChangeTypeTest_removeVulnerabilityProductStatusKnownNotAffected() throws IOException, CsafException {

        String oldVul = """
                 [ { "product_status": {
                      "known_not_affected": ["CSAFPID-0001", "CSAFPID-0002"]
                     }
                   }
                  ]
                """;

        String newVul = """
                 [ { "product_status": {
                      "known_not_affected": ["CSAFPID-0001"]
                     }
                   }
                  ]
                """;


        String oldCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(oldVul);
        AdvisoryWrapper oldAdvisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(oldCsafJson), "user1", Semantic.name());
        String newCsafJson = CsafDocumentJsonCreator.docWithVulnerabilities(newVul);
        AdvisoryWrapper newAdvisory = AdvisoryWrapper.updateFromExisting(oldAdvisory, csafToRequest(newCsafJson));

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
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

        assertThat(AdvisoryWorkflowUtil.getChangeType(oldAdvisory, newAdvisory, 4), is(PatchType.MAJOR));
    }

    private static Stream<Arguments> spellingMistakeArgs() {
        return Stream.of(
                Arguments.of("Test", "Tets", 4, TRUE),
                Arguments.of("Test", "Test1", 4, TRUE),
                Arguments.of("Test", "Tstings", 4, FALSE),
                Arguments.of("Test longer Words", "Tets lnget Words", 4, TRUE),
                Arguments.of("Test longer Words", "Tets lnget Wrds", 4, FALSE),
                Arguments.of("Test", "Tets", 2, TRUE),
                Arguments.of("Test", "Test1", 2, TRUE),
                Arguments.of("Test", "Test", 0, TRUE),
                Arguments.of("Test", "Tst", 0, FALSE)
        );

    }

    @ParameterizedTest()
    @MethodSource("spellingMistakeArgs")
    public void isSpellingMistakeTest(String oldString, String newString, int maxLevenshteinDistance, Boolean expectedValue) {

        assertThat(isSpellingMistake(oldString, newString, maxLevenshteinDistance), is(expectedValue));

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

    private static Stream<Arguments> timestampArgs() {
        return Stream.of(
                Arguments.of("2022-09-22T01:00:00.000Z", "2022-09-22T02:00:00.000Z", TRUE),
                Arguments.of("2022-09-22T02:00:00.000Z", "2022-09-22T01:00:00.000Z", FALSE),
                Arguments.of("2022-09-22T01:00:00.000Z", "2022-09-22T01:00:00.000Z", FALSE),
                Arguments.of("2022-09-22T01:00:00.000Z", "2022-09-23T01:00:00.000Z", TRUE),
                Arguments.of("2022-09-23T01:00:00.000Z", "2022-09-22T02:00:00.000Z", FALSE),
                Arguments.of("2022-09-22T01:02:03.004Z", "2022-09-22T02:03:04.005Z", TRUE),
                Arguments.of("2022-09-22T01:02:03.004Z", "2022-09-22T02:03:04.005678Z", TRUE),
                Arguments.of("2022-09-22T01:02:03.004567Z", "2022-09-22T02:03:04.005Z", TRUE)
        );

    }

    @ParameterizedTest()
    @MethodSource("timestampArgs")
    public void timestampIsBeforeTest(String timestamp1, String timestamp2, Boolean expectedResult) {
        assertThat(timestampIsBefore(timestamp1, timestamp2), is(expectedResult));
    }

}
