package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.bsi.secvisogram.csaf_cms_backend.PostgreSQLExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The key order of a saved document survives the round trip request, database, response. The keys are chosen so that
 * neither the alphabetical order nor the order of a jsonb column (by length, then bytewise) equals the saved order.
 */
@SpringBootTest(properties = {
        "csaf.workflow.allowOwnDocumentsApproved=true",
})
@ExtendWith(PostgreSQLExtension.class)
@DirtiesContext
@SpringJUnitConfig
public class AdvisoryCsafKeyOrderTest {

    private static final JsonMapper MAPPER = new JsonMapper();

    private static final String FIRST = """
            {"vulnerabilities": [{"title": "v", "cve": "CVE-2024-0001"}],
             "document": {"title": "t", "category": "c", "lang": "en"}}""";
    private static final String SECOND = """
            {"document": {"lang": "de", "category": "c", "title": "t2"},
             "vulnerabilities": [{"cve": "CVE-2024-0002", "title": "v"}]}""";

    @Autowired
    private AdvisoryService advisoryService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void savedKeyOrderIsStoredAndReturned() throws Exception {
        IdAndRevision created = advisoryService.addAdvisory(csafToRequest(FIRST));

        assertFirstOrder(MAPPER.readTree(jdbc.queryForObject("SELECT csaf FROM advisories", String.class)));
        assertFirstOrder(advisoryService.getAdvisory(created.getId()).getCsaf());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void keyOrderOfAnUpdateIsStoredAndReturned() throws Exception {
        IdAndRevision created = advisoryService.addAdvisory(csafToRequest(FIRST));
        advisoryService.updateAdvisory(created.getId(), created.getRevision(), csafToRequest(SECOND));

        assertSecondOrder(MAPPER.readTree(jdbc.queryForObject("SELECT csaf FROM advisories", String.class)));
        assertSecondOrder(advisoryService.getAdvisory(created.getId()).getCsaf());
    }

    private static void assertFirstOrder(JsonNode csaf) {
        assertEquals(List.of("vulnerabilities", "document"), names(csaf, Set.of("vulnerabilities", "document")));
        assertEquals(List.of("title", "category", "lang"),
                names(csaf.get("document"), Set.of("title", "category", "lang")));
        assertEquals(List.of("title", "cve"), names(csaf.get("vulnerabilities").get(0), Set.of("title", "cve")));
    }

    private static void assertSecondOrder(JsonNode csaf) {
        assertEquals(List.of("document", "vulnerabilities"), names(csaf, Set.of("vulnerabilities", "document")));
        assertEquals(List.of("lang", "category", "title"),
                names(csaf.get("document"), Set.of("title", "category", "lang")));
        assertEquals(List.of("cve", "title"), names(csaf.get("vulnerabilities").get(0), Set.of("title", "cve")));
    }

    /** The names of the object's members that are in the given set, in document order (the service adds others). */
    private static List<String> names(JsonNode node, Set<String> only) {
        return node.properties().stream().map(Map.Entry::getKey).filter(only::contains).toList();
    }
}
