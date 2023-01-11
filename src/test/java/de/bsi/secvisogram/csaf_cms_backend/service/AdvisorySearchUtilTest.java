package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
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
