package de.bsi.secvisogram.csaf_cms_backend.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafAcknowledgmentsNames;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonRevisionHistorySummary;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonTitle;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafProductTreeBranchesCategory;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafProductTreeFullProductNamesProductId;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafVulnerabilitiesCve;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;

@SpringBootTest(properties = {
        "csaf.workflow.allowOwnDocumentsApproved=true",
})
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@ContextConfiguration
public class AdvisorySearchUtilTest {

    @Autowired
    private AdvisoryService advisoryService;


    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_documentTitle() throws IOException, CsafException {
        IdAndRevision idRev1 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("title1")));
        this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("title2")));
        List<AdvisoryInformationResponse> infos = this.advisoryService.getAdvisoryInformations(createExprDocumentTitle("title1"));
        List<String> expectedIDs = List.of(idRev1.getId());
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();
        Assertions.assertTrue(ids.size() == expectedIDs.size()
                && ids.containsAll(expectedIDs)
                && expectedIDs.containsAll(ids));
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_documentTrackingRevisionHistorysummary() throws IOException, CsafException {

        this.advisoryService.addAdvisory(csafToRequest(csafJsonRevisionHistorySummary("SummaryOne")));
        CreateAdvisoryRequest request = csafToRequest(csafJsonRevisionHistorySummary("SummaryTwo"));
        request.setSummary("SummaryInRequest");
        IdAndRevision idRev2 = this.advisoryService.addAdvisory(request);
        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(createExprRevisionHistorySummary("SummaryInRequest"));
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();
        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRev2.getId()));
    }


    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_csafAcknowledgmentsNames() throws IOException, CsafException {

        this.advisoryService.addAdvisory(csafToRequest(csafAcknowledgmentsNames("John")));
        IdAndRevision idRev2 = this.advisoryService.addAdvisory(csafToRequest(csafAcknowledgmentsNames("Jack")));
        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(createExprAcknowledgmentsNames("Jack"));
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();
        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRev2.getId()));
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_vulnerabilitiesCve() throws IOException, CsafException {

        this.advisoryService.addAdvisory(csafToRequest(csafVulnerabilitiesCve("CVE-2021-44228")));
        IdAndRevision idRev2 = this.advisoryService.addAdvisory(csafToRequest(csafVulnerabilitiesCve("CVE-2021-44999")));
        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(createExprVulnerabilitiesCve("CVE-2021-44999"));
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();
        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRev2.getId()));
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_productTreeFullProductNames() throws IOException, CsafException {

        this.advisoryService.addAdvisory(csafToRequest(csafProductTreeFullProductNamesProductId("ProductOne")));
        IdAndRevision idRev2 = this.advisoryService.addAdvisory(csafToRequest(csafProductTreeFullProductNamesProductId("ProductTwo")));
        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(createProductTreeProductId("ProductTwo"));
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();
        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRev2.getId()));
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_csafProductTreeBranchesCategory() throws IOException, CsafException {

        this.advisoryService.addAdvisory(csafToRequest(csafProductTreeBranchesCategory("CategoryOne")));
        IdAndRevision idRev2 = this.advisoryService.addAdvisory(csafToRequest(csafProductTreeBranchesCategory("CategoryTwo")));
        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(createProductTreeBranchesCategory("CategoryTwo"));
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();
        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRev2.getId()));
    }

    // -----------------------------------------------------------------------
    // Role-visibility tests
    // -----------------------------------------------------------------------

    /**
     * An EDITOR can see all advisories regardless of workflow state.
     * Two advisories created by an author are both visible when searching as an EDITOR.
     */
    @Test
    @WithMockUser(username = "editor1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR,
            CsafRoles.ROLE_EDITOR})
    public void getAdvisoryInformationsTest_roleEditor_seesAll() throws IOException, CsafException, DatabaseException {

        IdAndRevision idRev1 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("editorTitle1")));
        IdAndRevision idRev2 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("editorTitle2")));

        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(null);
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

        assertThat(ids.size(), equalTo(2));
        assertThat(ids, hasItems(idRev1.getId(), idRev2.getId()));
    }

    /**
     * An EDITOR can search advisories by title and gets the correct result.
     */
    @Test
    @WithMockUser(username = "editor1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR,
            CsafRoles.ROLE_EDITOR})
    public void getAdvisoryInformationsTest_roleEditor_filterByTitle() throws IOException, CsafException, DatabaseException {

        IdAndRevision idRev1 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("editorFilterTitle1")));
        this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("editorFilterTitle2")));

        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(createExprDocumentTitle("editorFilterTitle1"));
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRev1.getId()));
    }

    /**
     * An AUDITOR can see all advisories (Draft state) and also AdvisoryVersions.
     * Advisories are created by an author first; then we switch to auditor to verify visibility.
     */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_roleAuditor_seesAll() throws IOException, CsafException, DatabaseException {

        // Create advisories as an author
        IdAndRevision idRev1 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("auditorTitle1")));
        IdAndRevision idRev2 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("auditorTitle2")));

        // Switch to auditor role
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(
                        "auditor1", null,
                        CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR));

        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(null);
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

        assertThat(ids.size(), equalTo(2));
        assertThat(ids, hasItems(idRev1.getId(), idRev2.getId()));
    }

    /**
     * An AUDITOR can filter advisories by title.
     * Advisories are created by an author first; then we switch to auditor to verify filtering.
     */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_roleAuditor_filterByTitle() throws IOException, CsafException, DatabaseException {

        // Create advisories as an author
        IdAndRevision idRev1 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("auditorFilterTitle1")));
        this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("auditorFilterTitle2")));

        // Switch to auditor role
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(
                        "auditor1", null,
                        CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR));

        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(createExprDocumentTitle("auditorFilterTitle1"));
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRev1.getId()));
    }

    /**
     * A REVIEWER can see advisories that are in Review state (and not owned by them) as well as
     * published advisories. Draft advisories of other users are NOT visible.
     * Setup: author1 creates two advisories; one is moved to Review, the other stays in Draft.
     * When reviewer1 searches, only the advisory in Review state should be returned.
     */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_roleReviewer_seesOnlyReviewState() throws IOException, CsafException, DatabaseException {

        IdAndRevision idRevDraft = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("reviewerDraftTitle")));
        IdAndRevision idRevReview = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("reviewerReviewTitle")));
        // Move the second advisory to Review state
        this.advisoryService.changeAdvisoryWorkflowState(
                idRevReview.getId(), idRevReview.getRevision(), WorkflowState.Review, null, null);

        // Now switch to reviewer role and search
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(
                        "reviewer1", null,
                        CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_REVIEWER));

        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(null);
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

        // reviewer1 should only see the advisory in Review (not the Draft, which belongs to author1)
        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRevReview.getId()));
    }

    /**
     * A REVIEWER can filter by title among the advisories they are allowed to see.
     */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_roleReviewer_filterByTitle() throws IOException, CsafException, DatabaseException {

        IdAndRevision idRevReview1 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("reviewFilterTitle1")));
        IdAndRevision idRevReview2 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("reviewFilterTitle2")));
        this.advisoryService.changeAdvisoryWorkflowState(
                idRevReview1.getId(), idRevReview1.getRevision(), WorkflowState.Review, null, null);
        this.advisoryService.changeAdvisoryWorkflowState(
                idRevReview2.getId(), idRevReview2.getRevision(), WorkflowState.Review, null, null);

        // Switch to reviewer role
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(
                        "reviewer1", null,
                        CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_REVIEWER));

        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(createExprDocumentTitle("reviewFilterTitle1"));
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRevReview1.getId()));
    }

    /**
     * A PUBLISHER can see advisories in Draft, Approved, and RfPublication states as well as published.
     * Two advisories created by author1: one stays Draft, the other is moved to Review then Approved.
     * A publisher should see both (Draft is visible to publishers).
     */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR,
            CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER})
    public void getAdvisoryInformationsTest_rolePublisher_seesApprovedAndDraft() throws IOException, CsafException, DatabaseException {

        IdAndRevision idRevDraft = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("publisherDraftTitle")));
        IdAndRevision idRevApproved = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("publisherApprovedTitle")));
        String revision = this.advisoryService.changeAdvisoryWorkflowState(
                idRevApproved.getId(), idRevApproved.getRevision(), WorkflowState.Review, null, null);
        this.advisoryService.changeAdvisoryWorkflowState(
                idRevApproved.getId(), revision, WorkflowState.Approved, null, null);

        // Switch to publisher role
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(
                        "publisher1", null,
                        CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_PUBLISHER));

        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(null);
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

        assertThat(ids.size(), equalTo(2));
        assertThat(ids, hasItems(idRevDraft.getId(), idRevApproved.getId()));
    }

    /**
     * A PUBLISHER can filter by title among the advisories they are allowed to see.
     */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR,
            CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER})
    public void getAdvisoryInformationsTest_rolePublisher_filterByTitle() throws IOException, CsafException, DatabaseException {

        IdAndRevision idRevDraft1 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("publisherFilterTitle1")));
        this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("publisherFilterTitle2")));

        // Switch to publisher role
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(
                        "publisher1", null,
                        CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_PUBLISHER));

        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(createExprDocumentTitle("publisherFilterTitle1"));
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

        assertThat(ids.size(), equalTo(1));
        assertThat(ids, hasItems(idRevDraft1.getId()));
    }

    /**
     * A REGISTERED user (without AUTHOR/EDITOR/etc.) can only see published advisories.
     * A Draft advisory created by an author must NOT appear in the results for a registered-only user.
     */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR,
            CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void getAdvisoryInformationsTest_roleRegistered_seesOnlyPublished() throws IOException, CsafException, DatabaseException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock =
                     Mockito.mockStatic(ValidatorServiceClient.class)) {

            validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

            // Create one advisory and keep it as Draft; create another and publish it
            this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("registeredDraftTitle")));
            IdAndRevision idRevPublished = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("registeredPublishedTitle")));
            String revision = this.advisoryService.changeAdvisoryWorkflowState(
                    idRevPublished.getId(), idRevPublished.getRevision(), WorkflowState.Review, null, null);
            revision = this.advisoryService.changeAdvisoryWorkflowState(
                    idRevPublished.getId(), revision, WorkflowState.Approved, null, null);
            revision = this.advisoryService.changeAdvisoryWorkflowState(
                    idRevPublished.getId(), revision, WorkflowState.RfPublication, null, null);
            this.advisoryService.changeAdvisoryWorkflowState(
                    idRevPublished.getId(), revision, WorkflowState.Published, null, null);

            // Switch to a registered-only user (no AUTHOR/EDITOR/etc.)
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.TestingAuthenticationToken(
                            "registeredUser", null,
                            CsafRoles.ROLE_REGISTERED));

            List<AdvisoryInformationResponse> infos =
                    this.advisoryService.getAdvisoryInformations(null);
            List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

            // Only the published advisory should be visible; the Draft advisory must not appear
            assertThat(ids.size(), equalTo(1));
            assertThat(ids, hasItems(idRevPublished.getId()));
        }
    }

    /**
     * An AUTHOR can see their own advisories regardless of workflow state
     * and all published advisories. Advisories of other users in Draft are NOT visible.
     */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void getAdvisoryInformationsTest_roleAuthor_seesOwnAndPublished() throws IOException, CsafException {

        // author1 creates two advisories
        IdAndRevision ownAdvisory1 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("authorOwnTitle1")));
        IdAndRevision ownAdvisory2 = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("authorOwnTitle2")));

        // Simulate author2 creating an advisory by switching credentials temporarily
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(
                        "author2", null,
                        CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR));
        this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("authorOtherTitle")));

        // Switch back to author1 and search
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(
                        "author1", null,
                        CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR));

        List<AdvisoryInformationResponse> infos =
                this.advisoryService.getAdvisoryInformations(null);
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

        // author1 should only see their own advisories (other user's Draft is not visible)
        assertThat(ids.size(), equalTo(2));
        assertThat(ids, hasItems(ownAdvisory1.getId(), ownAdvisory2.getId()));
    }

    /**
     * A MANAGER role inherits no special visibility filter (similar to REGISTERED-only).
     * Draft advisories are not visible to a plain manager who is not also an editor/author.
     * This test verifies that a manager without extra roles sees only published advisories.
     */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR,
            CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void getAdvisoryInformationsTest_roleManager_seesOnlyPublished() throws IOException, CsafException, DatabaseException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock =
                     Mockito.mockStatic(ValidatorServiceClient.class)) {

            validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

            this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("managerDraftTitle")));
            IdAndRevision idRevPublished = this.advisoryService.addAdvisory(csafToRequest(csafJsonTitle("managerPublishedTitle")));
            String revision = this.advisoryService.changeAdvisoryWorkflowState(
                    idRevPublished.getId(), idRevPublished.getRevision(), WorkflowState.Review, null, null);
            revision = this.advisoryService.changeAdvisoryWorkflowState(
                    idRevPublished.getId(), revision, WorkflowState.Approved, null, null);
            revision = this.advisoryService.changeAdvisoryWorkflowState(
                    idRevPublished.getId(), revision, WorkflowState.RfPublication, null, null);
            this.advisoryService.changeAdvisoryWorkflowState(
                    idRevPublished.getId(), revision, WorkflowState.Published, null, null);

            // Switch to a manager-only user (MANAGER does not have special visibility in buildVisibilityExpression)
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.TestingAuthenticationToken(
                            "manager1", null,
                            CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_MANAGER));

            List<AdvisoryInformationResponse> infos =
                    this.advisoryService.getAdvisoryInformations(null);
            List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();

            assertThat(ids.size(), equalTo(1));
            assertThat(ids, hasItems(idRevPublished.getId()));
        }
    }



    private String createExprDocumentTitle(String value) {

        return createExpr(value, "csaf", "document", "title");

    }

    private String createExprRevisionHistorySummary(String value) {

        return createExpr(value, "csaf", "document", "tracking", "revision_history", "summary");
    }

    private String createExprAcknowledgmentsNames(String value) {

        return createExpr(value, "csaf", "document", "acknowledgments", "names");
    }

    private String createExprVulnerabilitiesCve(String value) {

        return createExpr(value, "csaf", "vulnerabilities", "cve");
    }

    private String createProductTreeProductId(String value) {

        return createExpr(value, "csaf", "product_tree", "full_product_names", "product_id");
    }

    private String createProductTreeBranchesCategory(String value) {

        return createExpr(value, "csaf", "product_tree", "branches", "category");
    }

    private String createExpr(String value, String ... selector) {

        String selectorString = Arrays.stream(selector)
                                .map(select -> "\"" + select + "\"")
                                .collect(Collectors.joining(", "));

        return """
                { "type" : "Operator",
                  "selector" : [ %s ],
                  "operatorType" : "Equal",
                  "value" : "%s",
                  "valueType" : "Text"
                }
                """.formatted(selectorString, value);
    }
}
