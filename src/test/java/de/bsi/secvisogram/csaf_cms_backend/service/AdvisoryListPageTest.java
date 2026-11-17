package de.bsi.secvisogram.csaf_cms_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.bsi.secvisogram.csaf_cms_backend.PostgreSQLExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.AndExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.Expression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OrExpression;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryPageResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * The paged advisory list: paging, sort, column-only filters, visibility and version snapshots.
 */
@SpringBootTest(properties = {
        "csaf.workflow.allowOwnDocumentsApproved=true",
})
@ExtendWith(PostgreSQLExtension.class)
@DirtiesContext
@SpringJUnitConfig
public class AdvisoryListPageTest {

    @Autowired
    private AdvisoryService advisoryService;

    @Autowired
    private JdbcTemplate jdbc;

    private static String csaf(String title, String trackingId, String status, String releaseDate, String cve) {
        return """
                {"document": {"category": "csaf_base", "title": "%s",
                  "tracking": {"id": "%s", "version": "1.0.0" %s %s}}
                 %s}""".formatted(title, trackingId,
                status == null ? "" : ", \"status\": \"" + status + "\"",
                releaseDate == null ? "" : ", \"current_release_date\": \"" + releaseDate + "\"",
                cve == null ? "" : ", \"vulnerabilities\": [{\"cve\": \"" + cve + "\"}]");
    }

    /** Fixed reference time for updated_at and created_at, so that no test depends on a clock. */
    private static final String BASE_TIME = "2025-01-01T12:00:00Z";

    /** Inserts an advisory as it is; updated_at is {@link #BASE_TIME} minus the given minutes. */
    private UUID insert(String owner, String workflowState, String csaf, int minutesBefore) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO advisories (id, owner, workflow_state, csaf, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, now(), CAST(? AS timestamptz) - make_interval(mins => ?))",
                id, owner, workflowState, csaf, BASE_TIME, minutesBefore);
        return id;
    }

    /** Inserts a snapshot; created_at is {@link #BASE_TIME} minus the given minutes. */
    private void insertVersion(UUID advisoryId, String csaf, int minutesBefore) {
        jdbc.update("INSERT INTO advisory_versions (advisory_id, owner, workflow_state, csaf, versioning_type, "
                + "created_at) VALUES (?, 'author1', 'Published', ?, 'Semantic', "
                + "CAST(? AS timestamptz) - make_interval(mins => ?))", advisoryId, csaf, BASE_TIME, minutesBefore);
    }

    /** Five drafts of author1: titles A..E, updated in the order E (newest) .. A (oldest). */
    private void seedFive() {
        insert("author1", "Draft", csaf("C", "ID-C", "draft", "2024-03-01T00:00:00Z", "CVE-2024-0003"), 3);
        insert("author1", "Draft", csaf("A", "ID-A", "final", "2024-01-01T00:00:00Z", "CVE-2024-0001"), 5);
        insert("author1", "Draft", csaf("E", "ID-E", null, null, null), 1);
        insert("author1", "Draft", csaf("B", "ID-B", "interim", "2024-02-01T00:00:00+02:00", "CVE-2024-0002"), 4);
        insert("author1", "Draft", csaf("D", "ID-D", "draft", "2024-04-01T00:00:00Z", "CVE-2024-0001"), 2);
    }

    private static List<String> titles(AdvisoryPageResponse page) {
        return page.items().stream().map(AdvisoryInformationResponse::getTitle).toList();
    }

    private AdvisoryPageResponse page(Expression filter, int page, int size, String... sort) throws Exception {
        return advisoryService.getAdvisoryPage(filter == null ? null : AdvisorySearchUtil.expression2Json(filter),
                page, size, List.of(sort));
    }

    private void assertRejected(CsafExceptionKey key, Executable call) {
        CsafException ex = assertThrows(CsafException.class, call::run);
        assertEquals(key, ex.getExceptionKey());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getRecommendedHttpState());
    }

    private interface Executable {
        void run() throws Exception;
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void pagesAndTotal() throws Exception {
        seedFive();
        AdvisoryPageResponse first = page(null, 0, 2, "title,asc");
        assertEquals(List.of("A", "B"), titles(first));
        assertEquals(5, first.totalElements());
        assertEquals(0, first.page());
        assertEquals(2, first.size());
        assertEquals(List.of("C", "D"), titles(page(null, 1, 2, "title,asc")));
        assertEquals(List.of("E"), titles(page(null, 2, 2, "title,asc")));
        AdvisoryPageResponse beyond = page(null, 3, 2, "title,asc");
        assertEquals(List.of(), titles(beyond));
        assertEquals(5, beyond.totalElements());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void emptyResultHasTotalZero() throws Exception {
        AdvisoryPageResponse page = page(null, 0, 10);
        assertEquals(List.of(), titles(page));
        assertEquals(0, page.totalElements());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void defaultSortIsLastUpdatedNewestFirst() throws Exception {
        seedFive();
        AdvisoryPageResponse page = advisoryService.getAdvisoryPage(null, 0, 10, null);
        assertEquals(List.of("E", "D", "C", "B", "A"), titles(page));
        AdvisoryInformationResponse newest = page.items().get(0);
        assertEquals("0", newest.getRevision());
        assertEquals("ID-E", newest.getDocumentTrackingId());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void sortsByColumnsWithEmptyValuesLast() throws Exception {
        seedFive();
        assertEquals(List.of("E", "D", "C", "B", "A"), titles(page(null, 0, 10, "title,desc")));
        assertEquals(List.of("A", "B", "C", "D", "E"), titles(page(null, 0, 10, "currentReleaseDate")));
        assertEquals(List.of("D", "C", "B", "A", "E"), titles(page(null, 0, 10, "currentReleaseDate,desc")));
        // status: draft (C, D), final (A), interim (B), none (E); ties by the second criterion
        assertEquals(List.of("C", "D", "A", "B", "E"), titles(page(null, 0, 10, "trackingStatus", "title")));
        assertEquals(List.of("B", "A", "D", "C", "E"),
                titles(page(null, 0, 10, "trackingStatus,DESC", "title,desc")));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void equalSortValuesGiveStablePages() throws Exception {
        for (int i = 0; i < 6; i++) {
            insert("author1", "Draft", csaf("same", "ID-" + i, "draft", null, null), 0);
        }
        List<String> ids = new java.util.ArrayList<>();
        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            page(null, pageNumber, 2, "title").items().forEach(item -> ids.add(item.getAdvisoryId()));
        }
        assertEquals(6, ids.stream().distinct().count());
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void filtersOnColumns() throws Exception {
        seedFive();
        AdvisoryPageResponse cve = page(OperatorExpression.equal("CVE-2024-0001", "csaf", "vulnerabilities", "cve"),
                0, 10, "title");
        assertEquals(List.of("A", "D"), titles(cve));
        assertEquals(2, cve.totalElements());
        // B is 2024-01-31T22:00Z, so only C and D are after 2024-02-01T00:00Z
        assertEquals(List.of("C", "D"), titles(page(OperatorExpression.greater("2024-01-31T23:00:00Z",
                "csaf", "document", "tracking", "current_release_date"), 0, 10, "title")));
        assertEquals(List.of("C", "D"), titles(page(OperatorExpression.equal("draft",
                "csaf", "document", "tracking", "status"), 0, 10, "title")));
        // E and D were updated 1 and 2 minutes before BASE_TIME
        assertEquals(List.of("E", "D"), titles(page(OperatorExpression.greater("2025-01-01T13:57:30+02:00",
                "lastUpdated"), 0, 10)));
        assertEquals(List.of("B"), titles(page(OperatorExpression.containsIgnoreCase("id-b",
                "csaf", "document", "tracking", "id"), 0, 10)));
    }

    @Test
    @WithMockUser(username = "publisher1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_PUBLISHER})
    public void filtersOnOwnerAndWorkflowState() throws Exception {
        insert("author1", "Draft", csaf("draft 1", "ID-1", "draft", null, null), 0);
        insert("author2", "Draft", csaf("draft 2", "ID-2", "draft", null, null), 0);
        insert("author1", "Approved", csaf("approved 1", "ID-3", "draft", null, null), 0);
        assertEquals(List.of("approved 1", "draft 1"),
                titles(page(OperatorExpression.equal("author1", "owner"), 0, 10, "title")));
        assertEquals(List.of("draft 1", "draft 2"),
                titles(page(OperatorExpression.equal("Draft", "workflowState"), 0, 10, "title")));
        assertEquals(List.of("approved 1"), titles(page(new AndExpression(
                OperatorExpression.equal("author1", "owner"),
                OperatorExpression.notEqual("Draft", "workflowState")), 0, 10)));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void oneUnsupportedBranchRejectsTheWholeFilter() {
        OperatorExpression valid = OperatorExpression.equal("draft", "csaf", "document", "tracking", "status");
        OperatorExpression invalid = OperatorExpression.equal("en", "csaf", "document", "lang");
        assertRejected(CsafExceptionKey.InvalidFilterExpression,
                () -> page(new OrExpression(valid, invalid), 0, 10));
        assertRejected(CsafExceptionKey.InvalidFilterExpression,
                () -> page(new AndExpression(valid, new OrExpression(valid, invalid)), 0, 10));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void rejectsWhatNoColumnAnswers() {
        assertRejected(CsafExceptionKey.InvalidFilterExpression,
                () -> page(OperatorExpression.equal("en", "csaf", "document", "lang"), 0, 10));
        assertRejected(CsafExceptionKey.InvalidFilterExpression,
                () -> page(OperatorExpression.equal(2.0, "csaf", "document", "tracking", "version"), 0, 10));
        assertRejected(CsafExceptionKey.InvalidFilterExpression,
                () -> page(OperatorExpression.greater("yesterday", "lastUpdated"), 0, 10));
        assertRejected(CsafExceptionKey.InvalidFilterExpression, () -> page(OperatorExpression.containsIgnoreCase(
                "2024", "csaf", "document", "tracking", "current_release_date"), 0, 10));
        assertRejected(CsafExceptionKey.InvalidListParameter, () -> page(null, 0, 10, "cves"));
        assertRejected(CsafExceptionKey.InvalidListParameter, () -> page(null, 0, 10, "title,up"));
        assertRejected(CsafExceptionKey.InvalidListParameter, () -> page(null, -1, 10));
        assertRejected(CsafExceptionKey.InvalidListParameter, () -> page(null, 0, 0));
        assertRejected(CsafExceptionKey.InvalidListParameter,
                () -> page(null, 0, AdvisoryService.MAX_PAGE_SIZE + 1));
    }

    @Test
    @WithMockUser(username = "author2", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void authorSeesOwnAndReleasedPublishedOnly() throws Exception {
        insert("author1", "Draft", csaf("other draft", "ID-1", "draft", null, null), 0);
        insert("author2", "Draft", csaf("own draft", "ID-2", "draft", null, null), 0);
        insert("author1", "Published", csaf("released", "ID-3", "final", "2024-01-01T00:00:00Z", null), 0);
        insert("author1", "Published", csaf("future", "ID-4", "final", "2999-01-01T00:00:00Z", null), 0);
        AdvisoryPageResponse page = page(null, 0, 10, "title");
        assertEquals(List.of("own draft", "released"), titles(page));
        assertEquals(2, page.totalElements());
    }

    @Test
    @WithMockUser(username = "auditor1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR})
    public void auditorGetsVersionsInTheSameSortedList() throws Exception {
        UUID advisory = insert("author1", "Draft", csaf("B current", "ID-B", "draft", null, null), 0);
        insertVersion(advisory, csaf("A snapshot", "ID-B", "final", "2024-01-01T00:00:00Z", null), 20);
        insertVersion(advisory, csaf("C snapshot", "ID-B", "final", "2024-02-01T00:00:00Z", null), 10);

        AdvisoryPageResponse first = page(null, 0, 2, "title");
        assertEquals(List.of("A snapshot", "B current"), titles(first));
        assertEquals(3, first.totalElements());
        AdvisoryInformationResponse snapshot = first.items().get(0);
        assertEquals(advisory.toString(), snapshot.getAdvisoryId());
        assertNull(snapshot.getRevision());
        assertFalse(snapshot.isChangeable());
        assertEquals(List.of("C snapshot"), titles(page(null, 1, 2, "title")));

        AdvisoryPageResponse filtered = page(OperatorExpression.equal("final",
                "csaf", "document", "tracking", "status"), 0, 10, "title");
        assertEquals(List.of("A snapshot", "C snapshot"), titles(filtered));

        // snapshots are filtered and sorted by their created_at, the advisory by updated_at
        assertEquals(List.of("B current", "C snapshot", "A snapshot"), titles(page(null, 0, 10, "lastUpdated,desc")));
        assertEquals("2025-01-01T11:50:00Z", page(null, 0, 10, "lastUpdated,desc").items().get(1).getLastUpdated());
        assertEquals(List.of("B current", "C snapshot"), titles(page(OperatorExpression.greater(
                "2025-01-01T11:45:00Z", "lastUpdated"), 0, 10, "lastUpdated,desc")));
    }

    @Test
    @WithMockUser(username = "author1", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void authorDoesNotGetVersions() throws Exception {
        UUID advisory = insert("author1", "Draft", csaf("current", "ID-1", "draft", null, null), 0);
        insertVersion(advisory, csaf("snapshot", "ID-1", "final", null, null), 0);
        assertEquals(List.of("current"), titles(page(null, 0, 10)));
    }
}
