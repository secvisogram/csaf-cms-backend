package de.bsi.secvisogram.csaf_cms_backend.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
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

    @Autowired
    private AdvisoryService advisoryService;

    @Test
    @WithMockUser(username = "editor1", authorities = {CsafRoles.ROLE_AUTHOR})
    public void addAdvisoryTest() throws IOException, DatabaseException {

        final String csafJson = csafDocumentJson("Category1", "Title1");
        IdAndRevision idRev = advisoryService.addAdvisory(csafJson);
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());

        String dateNowMinutes = Instant.now().toString().substring(0, 16);
        assertThat(advisory.getCsaf().at("/document/tracking/version").asText(), equalTo("0.0.1"));
        assertThat(advisory.getCsaf().at("/document/tracking/status").asText(), equalTo("draft"));
        assertThat(advisory.getCsaf().at("/document/tracking/current_release_date").asText(), startsWith(dateNowMinutes));
    }

    @Test
    @WithMockUser(username = "reviewer1", authorities = { CsafRoles.ROLE_REVIEWER})
    public void addAdvisoryTest_InvalidRole() throws IOException, DatabaseException {

        final String csafJson = csafDocumentJson("Category1", "Title1");
        Assertions.assertThrows(AccessDeniedException.class, () -> advisoryService.addAdvisory(csafJson));
     }


    private String csafDocumentJson(String documentCategory, String documentTitle) {

        return """
                { "document": {
                      "category": "%s",
                      "title":"%s"
                   }
                }""".formatted(documentCategory, documentTitle);
    }

}
