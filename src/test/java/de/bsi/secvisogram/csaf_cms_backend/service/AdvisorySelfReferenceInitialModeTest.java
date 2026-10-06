package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;

import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.node.ObjectNode;

/**
 * Verifies that with the default {@code csaf.references.regeneration=initial}, the self-reference is
 * generated once - at the advisory's first actual publish, never before - and left untouched by any
 * later TLP change, preserving the historic behavior. See issue #230.
 */
@SpringBootTest(properties = {
        "csaf.references.baseURL=https://example.com",
        "csaf.trackingid.company=Testcase",
        "csaf.trackingid.digits=5",
        "csaf.trackingid.assignment.phase=draft",
        "csaf.workflow.allowOwnDocumentsApproved=true",
})
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@SpringJUnitConfig
public class AdvisorySelfReferenceInitialModeTest {

    @Autowired
    private AdvisoryService advisoryService;

    private static final String csafJsonWhite = """
            {
                "document": {
                    "category": "CSAF_BASE",
                    "distribution": {
                        "tlp": {
                            "label": "WHITE"
                        }
                    }
                }
            }""";

    @Test
    @WithMockUser(username = "editor1", authorities = {CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER})
    public void selfReference_generatedOnceAtFirstPublishAndUnaffectedByLaterTlpChange() throws IOException, DatabaseException, CsafException {

        try (final MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class)) {
            validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

            IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJsonWhite));
            AdvisoryResponse created = advisoryService.getAdvisory(idRev.getId());
            assertThat(created.getCsaf().at("/document/references/0").isMissingNode(), is(true));

            String revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), idRev.getRevision(), WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.AutoPublish, null, DocumentTrackingStatus.Interim);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);

            AdvisoryResponse published = advisoryService.getAdvisory(idRev.getId());
            String originalSelfReferenceUrl = published.getCsaf().at("/document/references/0/url").asString();
            assertThat(originalSelfReferenceUrl, containsString("/white/"));

            // start a new draft version of the already-published advisory and change its TLP
            revision = advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
            AdvisoryResponse draftV2 = advisoryService.getAdvisory(idRev.getId());
            ObjectNode changedCsaf = (ObjectNode) draftV2.getCsaf();
            ((ObjectNode) changedCsaf.at("/document/distribution/tlp")).put("label", "AMBER");
            CreateAdvisoryRequest request = new CreateAdvisoryRequest().setSummary("Changed TLP").setCsaf(changedCsaf);
            revision = advisoryService.updateAdvisory(idRev.getId(), revision, request);

            AdvisoryResponse updated = advisoryService.getAdvisory(idRev.getId());
            // initial mode: the self-reference was generated once, at the first publish, and is never
            // touched again - not even by a later TLP change on a subsequent version.
            assertThat(updated.getCsaf().at("/document/references/0/url").asString(), equalTo(originalSelfReferenceUrl));

            // now actually publish the second version (with the changed TLP) as well
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Review, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Approved, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.RfPublication, null, null);
            revision = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.AutoPublish, null, DocumentTrackingStatus.Interim);
            advisoryService.changeAdvisoryWorkflowState(idRev.getId(), revision, WorkflowState.Published, null, null);

            AdvisoryResponse republished = advisoryService.getAdvisory(idRev.getId());
            // initial mode: even a second full publish must not touch the self-reference generated at
            // the first publish.
            assertThat(republished.getCsaf().at("/document/references/0/url").asString(), equalTo(originalSelfReferenceUrl));
        }
    }
}
