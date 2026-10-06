package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
 * Verifies that with {@code csaf.references.regeneration=always}, no self-reference exists before an
 * advisory is actually published, and that once one exists it is kept in sync with the document's TLP
 * label and real publication year on every subsequent update/publish. See issue #230.
 */
@SpringBootTest(properties = {
        "csaf.references.baseURL=https://example.com",
        "csaf.references.regeneration=always",
        "csaf.trackingid.company=Testcase",
        "csaf.trackingid.digits=5",
        "csaf.trackingid.assignment.phase=draft",
        "csaf.workflow.allowOwnDocumentsApproved=true",
})
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@SpringJUnitConfig
public class AdvisorySelfReferenceAlwaysModeTest {

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
    public void updateAdvisory_regeneratesSelfReferenceOnTlpChangeAfterPublish() throws IOException, DatabaseException, CsafException {

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
            assertThat(published.getCsaf().at("/document/references/0/url").asString(), containsString("/white/"));

            // start a new draft version of the already-published advisory and change its TLP
            revision = advisoryService.createNewCsafDocumentVersion(idRev.getId(), revision);
            AdvisoryResponse draftV2 = advisoryService.getAdvisory(idRev.getId());
            ObjectNode changedCsaf = (ObjectNode) draftV2.getCsaf();
            ((ObjectNode) changedCsaf.at("/document/distribution/tlp")).put("label", "AMBER");
            CreateAdvisoryRequest request = new CreateAdvisoryRequest().setSummary("Changed TLP").setCsaf(changedCsaf);
            advisoryService.updateAdvisory(idRev.getId(), revision, request);

            AdvisoryResponse updated = advisoryService.getAdvisory(idRev.getId());
            assertThat(updated.getCsaf().at("/document/references/0/url").asString(), containsString("/amber/"));
            assertThat(updated.getCsaf().at("/document/references/0/url").asString(), not(containsString("/white/")));
            // regeneration replaces the existing entry in place, it does not duplicate it
            assertThat(updated.getCsaf().at("/document/references/1").isMissingNode(), is(true));
        }
    }

}
