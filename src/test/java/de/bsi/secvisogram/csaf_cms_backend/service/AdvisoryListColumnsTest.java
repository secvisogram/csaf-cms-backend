package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static org.junit.jupiter.api.Assertions.*;

import de.bsi.secvisogram.csaf_cms_backend.PostgreSQLExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * The csaf jsonb column set from csaf_text by a trigger, the columns generated from it, the paged list (v2) filters
 * that use them, and the list v1 filters, which use the JSON path on the document instead.
 */
@SpringBootTest(properties = {
        "csaf.workflow.allowOwnDocumentsApproved=true",
})
@ExtendWith(PostgreSQLExtension.class)
@DirtiesContext
@SpringJUnitConfig
public class AdvisoryListColumnsTest {

    private static final String ALPHA = """
            {"document": {"category": "csaf_base", "title": "Alpha",
              "distribution": {"tlp": {"label": "AMBER"}},
              "tracking": {"id": "ID-A", "version": "1.0.0", "status": "final",
                "current_release_date": "2024-05-01T10:00:00+02:00",
                "aliases": ["ALIAS-1", "alias-two"]}},
             "vulnerabilities": [
               {"cve": "CVE-2024-0001", "ids": [{"system_name": "s", "text": "VID-1"}, {"system_name": "s", "text": "VID-2"}]},
               {"cve": "CVE-2024-0002"}]}""";
    private static final String BETA = """
            {"document": {"category": "csaf_security_advisory", "title": "Beta",
              "distribution": {"tlp": {"label": "WHITE"}},
              "tracking": {"id": "ID-B", "version": "2", "status": "draft",
                "current_release_date": "2024-06-01T00:00:00Z"}},
             "vulnerabilities": [{"cve": "CVE-2024-0002"}]}""";
    private static final String GAMMA = """
            {"document": {"category": "csaf_base", "title": "Gamma"}}""";

    @Autowired
    private AdvisoryService advisoryService;

    @Autowired
    private JdbcTemplate jdbc;

    /** Inserts the documents as they are: the service would replace tracking id, version, status and dates. */
    private void insert(String csaf) {
        jdbc.update("INSERT INTO advisories (id, owner, csaf_text, created_at, updated_at) "
                + "VALUES (gen_random_uuid(), 'author1', ?, now(), now())", csaf);
    }

    private void seed() {
        insert(ALPHA);
        insert(BETA);
        insert(GAMMA);
    }

    /** The number of advisories the list v1 finds; it filters CSAF selectors by the JSON path. */
    private int count(OperatorExpression filter) throws Exception {
        return advisoryService.getAdvisoryInformations(AdvisorySearchUtil.expression2Json(filter)).size();
    }

    /** The number of advisories the paged list (v2) finds; it filters CSAF selectors by the columns. */
    private int countV2(OperatorExpression filter) throws Exception {
        return Math.toIntExact(advisoryService.getAdvisoryPage(AdvisorySearchUtil.expression2Json(filter), 0, 500,
                List.of()).totalElements());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void triggerFillsTheColumns() throws Exception {
        insert(ALPHA);
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT tracking_id, document_title, tracking_version, tracking_status, category, tlp,
                       current_release_date AT TIME ZONE 'UTC' AS released, cves, vul_ids, aliases
                  FROM advisories""");
        assertEquals("ID-A", row.get("tracking_id"));
        assertEquals("Alpha", row.get("document_title"));
        assertEquals("1.0.0", row.get("tracking_version"));
        assertEquals("final", row.get("tracking_status"));
        assertEquals("csaf_base", row.get("category"));
        assertEquals("AMBER", row.get("tlp"));
        assertEquals("2024-05-01 08:00:00.0", row.get("released").toString());
        assertEquals(List.of("CVE-2024-0001", "CVE-2024-0002"), array(row.get("cves")));
        assertEquals(List.of("VID-1", "VID-2"), array(row.get("vul_ids")));
        assertEquals(List.of("ALIAS-1", "alias-two"), array(row.get("aliases")));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void absentOptionalValuesGiveNullOrEmpty() throws Exception {
        insert(GAMMA);
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT tracking_id, tracking_status, tlp, current_release_date, cves, vul_ids, aliases,
                       initial_release_date, publisher_name, revision_history_length,
                       cvss_v2_score, cvss_v3_score, cvss_v4_score, critical
                  FROM advisories""");
        assertEquals(null, row.get("tracking_id"));
        assertEquals(null, row.get("tracking_status"));
        assertEquals(null, row.get("tlp"));
        assertEquals(null, row.get("current_release_date"));
        assertEquals(List.of(), array(row.get("cves")));
        assertEquals(List.of(), array(row.get("vul_ids")));
        assertEquals(List.of(), array(row.get("aliases")));
        for (String column : List.of("initial_release_date", "publisher_name", "revision_history_length",
                "cvss_v2_score", "cvss_v3_score", "cvss_v4_score", "critical")) {
            assertEquals(null, row.get(column), column);
        }
    }

    /** CSAF 2.0: the scores are in vulnerabilities[].scores[]; each column is the maximum over all vulnerabilities. */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void triggerFillsTheIsdubaColumnsFromCsaf20() throws Exception {
        insert("""
                {"document": {"category": "csaf_base", "title": "Scores 2.0",
                  "publisher": {"category": "vendor", "name": "Example Inc.", "namespace": "https://example.com"},
                  "tracking": {"initial_release_date": "2024-01-01T10:00:00+02:00",
                    "revision_history": [{"number": "1"}, {"number": "2"}, {"number": "3"}]}},
                 "vulnerabilities": [
                   {"scores": [{"cvss_v3": {"baseScore": 7.5}}, {"cvss_v2": {"baseScore": 5.0}}]},
                   {"scores": [{"cvss_v3": {"baseScore": 9.8}, "cvss_v2": {"baseScore": 10}}]},
                   {"cve": "CVE-2024-0001"}]}""");
        Map<String, Object> row = isdubaColumns();
        assertEquals("2024-01-01 08:00:00.0", row.get("initial").toString());
        assertEquals("Example Inc.", row.get("publisher_name"));
        assertEquals(3, row.get("revision_history_length"));
        assertEquals("10", String.valueOf(row.get("cvss_v2_score")));
        assertEquals("9.8", String.valueOf(row.get("cvss_v3_score")));
        assertNull(row.get("cvss_v4_score"));
        assertEquals("9.8", String.valueOf(row.get("critical")));
    }

    /** CSAF 2.1: the scores are in vulnerabilities[].metrics[].content; critical prefers cvss_v4 over cvss_v3. */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void triggerFillsTheScoresFromCsaf21() throws Exception {
        insert("""
                {"document": {"category": "csaf_base", "title": "Scores 2.1"},
                 "vulnerabilities": [
                   {"metrics": [{"products": ["P1"], "content": {"cvss_v3": {"baseScore": 9.1},
                                                                  "cvss_v4": {"baseScore": 8.7}}}]},
                   {"metrics": [{"content": {"cvss_v4": {"baseScore": 6.9}}},
                                {"content": {"cvss_v2": {"baseScore": 4.3}}}]}]}""");
        Map<String, Object> row = isdubaColumns();
        assertEquals("4.3", String.valueOf(row.get("cvss_v2_score")));
        assertEquals("9.1", String.valueOf(row.get("cvss_v3_score")));
        assertEquals("8.7", String.valueOf(row.get("cvss_v4_score")));
        assertEquals("8.7", String.valueOf(row.get("critical")));
    }

    /** Malformed values give NULL instead of rejecting the write; without v4 and v3, critical is the v2 score. */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void malformedIsdubaValuesGiveNull() throws Exception {
        insert("""
                {"document": {"category": "csaf_base", "title": "Malformed",
                  "tracking": {"initial_release_date": "2024-13-01T00:00:00Z", "revision_history": {"number": "1"}}},
                 "vulnerabilities": [
                   {"scores": [{"cvss_v3": {"baseScore": "9.8"}}, {"cvss_v4": null}, "not an object"]},
                   {"scores": [{"cvss_v2": {"baseScore": 6.4}}]}]}""");
        Map<String, Object> row = isdubaColumns();
        assertNull(row.get("initial"));
        assertNull(row.get("revision_history_length"));
        assertNull(row.get("cvss_v3_score"));
        assertNull(row.get("cvss_v4_score"));
        assertEquals("6.4", String.valueOf(row.get("cvss_v2_score")));
        assertEquals("6.4", String.valueOf(row.get("critical")));
    }

    /** The publisher name and the initial release date are CSAF paths, so the v2 filter uses their columns. */
    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void publisherAndInitialReleaseDateColumns() throws Exception {
        insert("""
                {"document": {"category": "csaf_base", "title": "One", "publisher": {"name": "Example Inc."},
                  "tracking": {"initial_release_date": "2024-01-01T10:00:00+02:00"}}}""");
        insert("""
                {"document": {"category": "csaf_base", "title": "Two", "publisher": {"name": "Other Ltd."},
                  "tracking": {"initial_release_date": "2024-03-01T00:00:00Z"}}}""");
        String[] publisher = {"csaf", "document", "publisher", "name"};
        String[] initial = {"csaf", "document", "tracking", "initial_release_date"};
        assertEquals(1, countV2(OperatorExpression.equal("Example Inc.", publisher)));
        assertEquals(2, countV2(OperatorExpression.containsIgnoreCase("e", publisher)));
        // the same instant as One, written with another offset
        assertEquals(1, countV2(OperatorExpression.equal("2024-01-01T08:00:00Z", initial)));
        assertEquals(1, countV2(OperatorExpression.greater("2024-02-01T00:00:00Z", initial)));
    }

    private Map<String, Object> isdubaColumns() {
        return jdbc.queryForMap("""
                SELECT initial_release_date AT TIME ZONE 'UTC' AS initial, publisher_name, revision_history_length,
                       cvss_v2_score, cvss_v3_score, cvss_v4_score, critical
                  FROM advisories""");
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void unparsableReleaseDateGivesNull() throws Exception {
        insert("""
                {"document": {"category": "c", "title": "t", "tracking": {"current_release_date": "yesterday"}}}""");
        insert("""
                {"document": {"category": "c", "title": "t", "tracking": {"current_release_date": "2024-13-01T00:00:00Z"}}}""");
        insert("""
                {"document": {"category": "c", "title": "t", "tracking": {"current_release_date": "2024-05-01T10:00:00"}}}""");
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(current_release_date) FROM advisories", Integer.class));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void updateRefreshesTheColumns() throws Exception {
        IdAndRevision created = advisoryService.addAdvisory(csafToRequest(ALPHA));
        advisoryService.updateAdvisory(created.getId(), created.getRevision(), csafToRequest(BETA));
        Map<String, Object> row = jdbc.queryForMap("SELECT document_title, tracking_status, cves, aliases FROM advisories");
        assertEquals("Beta", row.get("document_title"));
        // the service writes tracking id and dates itself: the column follows what was stored
        assertEquals(Boolean.TRUE, jdbc.queryForObject(
                "SELECT tracking_id = (csaf #>> '{document,tracking,id}') FROM advisories", Boolean.class));
        assertEquals("draft", row.get("tracking_status"));
        assertEquals(List.of("CVE-2024-0002"), array(row.get("cves")));
        assertEquals(List.of(), array(row.get("aliases")));
    }

    @Test
    public void invalidJsonIsRejectedByTheDatabase() {
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO advisories (id, owner, csaf_text, created_at, updated_at)
                VALUES (gen_random_uuid(), 'x', 'not json', now(), now())"""));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void arrayColumns() throws Exception {
        seed();
        String[] cve = {"csaf", "vulnerabilities", "cve"};
        assertEquals(2, countV2(OperatorExpression.equal("CVE-2024-0002", cve)));
        assertEquals(1, countV2(OperatorExpression.equal("CVE-2024-0001", cve)));
        assertEquals(0, countV2(OperatorExpression.equal("CVE-9999-0001", cve)));
        // some element differs, as with a JSON path over an array; a document without any cve never matches
        assertEquals(2, countV2(OperatorExpression.notEqual("CVE-2024-0001", cve)));
        assertEquals(2, countV2(OperatorExpression.containsIgnoreCase("cve-2024-000", cve)));
        assertEquals(1, countV2(OperatorExpression.containsIgnoreCase("0001", cve)));

        assertEquals(1, countV2(OperatorExpression.equal("VID-2", "csaf", "vulnerabilities", "ids", "text")));
        assertEquals(1, countV2(OperatorExpression.equal("alias-two", "csaf", "document", "tracking", "aliases")));
        assertEquals(1, countV2(OperatorExpression.containsIgnoreCase("ALIAS", "document", "tracking", "aliases")));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void textColumns() throws Exception {
        seed();
        assertEquals(1, countV2(OperatorExpression.equal("2", "csaf", "document", "tracking", "version")));
        assertEquals(1, countV2(OperatorExpression.equal("final", "csaf", "document", "tracking", "status")));
        assertEquals(2, countV2(OperatorExpression.equal("csaf_base", "csaf", "document", "category")));
        assertEquals(1, countV2(OperatorExpression.equal("ID-B", "csaf", "document", "tracking", "id")));
        assertEquals(1, countV2(OperatorExpression.containsIgnoreCase("amm", "document", "title")));
        assertEquals(1, countV2(OperatorExpression.equal("AMBER", "csaf", "document", "distribution", "tlp", "label")));
        assertEquals(1, countV2(OperatorExpression.equal("WHITE", "document", "distribution", "tlp", "label")));
        // without the leading csaf
        assertEquals(1, countV2(OperatorExpression.equal("draft", "document", "tracking", "status")));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void releaseDateIsComparedAsInstant() throws Exception {
        seed();
        String[] date = {"csaf", "document", "tracking", "current_release_date"};
        // the same instant as Alpha, written with another offset
        assertEquals(1, countV2(OperatorExpression.equal("2024-05-01T08:00:00Z", date)));
        assertEquals(1, countV2(OperatorExpression.greater("2024-05-15T00:00:00Z", date)));
        assertEquals(1, countV2(OperatorExpression.less("2024-05-15T00:00:00Z", date)));
        assertEquals(2, countV2(OperatorExpression.lessOrEqual("2024-06-01T00:00:00Z", date)));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void theV1FilterUsesTheJsonPath() throws Exception {
        seed();
        // the date is compared as text, so the same instant as Alpha with another offset does not match (v2 does)
        assertEquals(0, count(OperatorExpression.equal("2024-05-01T08:00:00Z", "csaf", "document", "tracking",
                "current_release_date")));
        assertEquals(1, count(OperatorExpression.equal("2024-05-01T10:00:00+02:00", "csaf", "document", "tracking",
                "current_release_date")));
        assertEquals(2, count(OperatorExpression.equal("CVE-2024-0002", "csaf", "vulnerabilities", "cve")));
        // not a timestamp, and a substring of one: both only make sense on the text in the document
        assertEquals(0, count(OperatorExpression.equal("yesterday", "csaf", "document", "tracking", "current_release_date")));
        assertEquals(1, count(OperatorExpression.containsIgnoreCase("2024-05", "document", "tracking", "current_release_date")));
        // a number is never equal to a string
        assertEquals(0, count(OperatorExpression.equal(1.0, "csaf", "document", "title")));
        // a selector without a column in v2
        assertEquals(1, count(OperatorExpression.equal("s", "csaf", "vulnerabilities", "ids", "system_name")));
    }

    /** csaf follows csaf_text on every write of csaf_text, and the generated columns follow csaf. */
    @Test
    public void csafFollowsCsafText() {
        insert(ALPHA);
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT csaf = csaf_text::jsonb FROM advisories",
                Boolean.class));

        jdbc.update("UPDATE advisories SET csaf_text = ?", BETA);
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT csaf = csaf_text::jsonb FROM advisories",
                Boolean.class));
        assertEquals("Beta", title());

        // an update of another column keeps both
        jdbc.update("UPDATE advisories SET owner = 'author2'");
        assertEquals("Beta", title());
    }

    /** csaf_text keeps the key order as written; the jsonb column orders the keys by length. */
    @Test
    public void csafTextKeepsTheKeyOrder() {
        String text = """
                {"vulnerabilities": [{"cve": "CVE-2024-0001"}], "document": {"title": "Order", "category": "csaf_base"}}""";
        insert(text);
        assertEquals(text, jdbc.queryForObject("SELECT csaf_text FROM advisories", String.class));
        assertTrue(jdbc.queryForObject("SELECT csaf::text FROM advisories", String.class)
                .startsWith("{\"document\": {\"title\""));
    }

    @Test
    public void generatedColumnsCannotBeWritten() {
        insert(ALPHA);
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE advisories SET document_title = 'x'"));
    }

    private String title() {
        return jdbc.queryForObject("SELECT document_title FROM advisories", String.class);
    }

    private static List<?> array(Object sqlArray) throws Exception {
        return List.of((Object[]) ((java.sql.Array) sqlArray).getArray());
    }

    @Test
    @WithMockUser(username = "auditor1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR})
    public void versionsHaveOnlyTheV1ColumnsAndFilterTheRestByJsonPath() throws Exception {
        jdbc.update("INSERT INTO advisories (id, owner, csaf_text) VALUES ('00000000-0000-0000-0000-000000000001', "
                + "'author1', ?)", GAMMA);
        jdbc.update("INSERT INTO advisory_versions (advisory_id, owner, workflow_state, csaf_text, versioning_type) "
                + "VALUES ('00000000-0000-0000-0000-000000000001', 'author1', 'Published', ?, 'Semantic')", ALPHA);

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT tracking_id, document_title, current_release_date AT TIME ZONE 'UTC' AS released
                  FROM advisory_versions""");
        assertEquals("ID-A", row.get("tracking_id"));
        assertEquals("Alpha", row.get("document_title"));
        assertEquals("2024-05-01 08:00:00.0", row.get("released").toString());
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_name = 'advisory_versions' AND column_name IN ('cves', 'vul_ids', 'aliases',
                       'tracking_version', 'tracking_status', 'category', 'tlp')""", Integer.class));

        // the advisory (Gamma) has no CVE; the snapshot (Alpha) is found by the JSON path
        assertEquals(1, count(OperatorExpression.equal("CVE-2024-0001", "csaf", "vulnerabilities", "cve")));
        assertEquals(1, count(OperatorExpression.equal("final", "csaf", "document", "tracking", "status")));
        assertEquals(1, count(OperatorExpression.containsIgnoreCase("alp", "csaf", "document", "title")));
        assertEquals(2, count(OperatorExpression.equal("csaf_base", "csaf", "document", "category")));
    }
}
