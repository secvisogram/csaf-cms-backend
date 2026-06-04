package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationPageResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for visible-layer pagination of the advisory list.
 * Backed by a real CouchDB started by {@link CouchDBExtension} (Docker-in-Docker), mirroring
 * the existing {@link AdvisoryServiceTest} wiring. These verify the end-to-end behaviour the
 * controller and codec unit tests cannot: that more than 25 rows are returned across pages, that the
 * visibility filter does not shrink page size below {@code limit} while more rows exist, that
 * {@code hasMore}/{@code bookmark} are correct at the boundaries, and that an auditor's
 * {@code AdvisoryVersion} rows survive a page boundary.
 *
 * <p><b>Run:</b> {@code ./mvnw test -DargLine="-Dapi.version=1.40"} from {@code csaf-cms-backend/}
 * (the {@code api.version} pin is required so Testcontainers' docker-java client negotiates a
 * version the Docker-in-Docker daemon accepts; see the test-run note in the build log).
 */
@SpringBootTest(properties = {
        "csaf.document.templates.companyLogoPath=./src/test/resources/eXXcellent_solutions.png",
        "csaf.summary.publication=testPublishMessage",
        "csaf.trackingid.company=",
        "csaf.trackingid.digits=7",
})
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@ContextConfiguration
@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "False positives on multiline format strings")
class AdvisoryServicePaginationTest {

    @Autowired
    private AdvisoryService advisoryService;

    private static final String csafJson = """
            {
                "document": {
                    "category": "CSAF_BASE"
                }
            }""";

    private static final String testEngineName = "Test Engine";
    private static final String testEngineVersion = "Test Version";

    @TestConfiguration
    public static class TestConfig {
        @Bean
        BuildProperties buildProperties() {
            Properties props = new Properties();
            props.setProperty("version", testEngineVersion);
            props.setProperty("name", testEngineName);
            return new BuildProperties(props);
        }
    }

    /** Authenticate the current thread as the given user with the given roles. */
    private static void authenticateAs(String user, String... roles) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user, null, authorities));
    }

    /** Seed {@code count} draft advisories owned by the current user; returns their ids. */
    private List<String> seedAdvisories(int count) throws IOException, CsafException {
        List<String> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String json = csafJson.replace("CSAF_BASE", "CSAF_BASE_" + i);
            ids.add(advisoryService.addAdvisory(csafToRequest(json)).getId());
        }
        return ids;
    }

    /**
     * Page the whole list with the given limit, following the bookmark until {@code hasMore} is
     * false, and return every advisoryId emitted in page order. Also asserts the per-page
     * fullness invariant along the way.
     */
    private List<String> collectAllPages(String expression, int limit) throws IOException, CsafException {
        List<String> collected = new ArrayList<>();
        String bookmark = null;
        int guard = 0;
        while (true) {
            AdvisoryInformationPageResponse page =
                    advisoryService.getAdvisoryInformationsPage(expression, limit, bookmark);
            assertEquals(limit, page.getLimit(), "the page must echo back the effective limit");
            assertTrue(page.getAdvisories().size() <= limit, "a page must never exceed the limit");

            for (AdvisoryInformationResponse a : page.getAdvisories()) {
                collected.add(a.getAdvisoryId());
            }

            if (page.isHasMore()) {
                assertNotNull(page.getBookmark(), "a non-final page must carry a non-null bookmark");
                // While more rows exist a page must be full (up to limit) of VISIBLE rows.
                assertEquals(limit, page.getAdvisories().size(),
                        "a non-final page must contain exactly 'limit' visible rows");
                bookmark = page.getBookmark();
            } else {
                assertNull(page.getBookmark(), "the final page must have a null bookmark");
                break;
            }

            if (++guard > 10_000) {
                throw new IllegalStateException("pagination did not terminate – likely a cursor bug");
            }
        }
        return collected;
    }

    private static void assertNoDuplicates(List<String> ids) {
        Set<String> unique = new HashSet<>(ids);
        assertEquals(ids.size(), unique.size(), "no advisoryId may appear on more than one page");
    }

    // ------------------------------------------------------------------------------------------
    // Cap removed: more than 25 rows are returned across pages, all of them, in order.
    // ------------------------------------------------------------------------------------------

    @Test
    void pagesThroughMoreThan25AdvisoriesWithLimit50() throws IOException, CsafException {
        // EDITOR can view every advisory, so the visible set == the seed set.
        authenticateAs("editor", CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR);

        final int seedCount = 60;
        List<String> seededIds = seedAdvisories(seedCount);
        assertTrue(seedCount > 25, "the seed must exceed the legacy 25-row cap to be meaningful");

        List<String> paged = collectAllPages(null, 50);

        // All seeded rows are returned, no duplicates.
        assertEquals(seedCount, paged.size(), "paging must return every seeded advisory");
        assertNoDuplicates(paged);
        assertTrue(paged.containsAll(seededIds) && seededIds.containsAll(paged),
                "the paged id set must equal the seeded id set");

        // Ordering matches the legacy endpoint's ordering rule (same order, no gaps). The
        // legacy endpoint is intentionally LEFT capped at the Mango default of 25 rows (that is the
        // bug pagination works around) so it returns only the first 25 rows in
        // order; the paged result must begin with exactly that prefix and then continue with the
        // remaining rows that the legacy path silently dropped.
        List<String> legacyOrder = advisoryService.getAdvisoryInformations(null).stream()
                .map(AdvisoryInformationResponse::getAdvisoryId).toList();
        assertEquals(25, legacyOrder.size(),
                "legacy mode is still capped at the 25-row Mango default (unchanged by this feature)");
        assertEquals(legacyOrder, paged.subList(0, legacyOrder.size()),
                "the paged result must start with the legacy rows in the same order (no reordering)");
    }

    @Test
    void lastPageHasNoMoreAndExactSizeWhenEvenlyDivisible() throws IOException, CsafException {
        authenticateAs("editor", CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR);
        seedAdvisories(20);

        // limit 10, 20 rows: page 1 hasMore=true, page 2 hasMore=false with null bookmark.
        AdvisoryInformationPageResponse page1 = advisoryService.getAdvisoryInformationsPage(null, 10, null);
        assertEquals(10, page1.getAdvisories().size());
        assertTrue(page1.isHasMore());
        assertNotNull(page1.getBookmark());

        AdvisoryInformationPageResponse page2 =
                advisoryService.getAdvisoryInformationsPage(null, 10, page1.getBookmark());
        assertEquals(10, page2.getAdvisories().size());
        assertFalse(page2.isHasMore(), "after the last full page there are no more rows");
        assertNull(page2.getBookmark());
    }

    @Test
    void singlePageWhenFewerRowsThanLimit() throws IOException, CsafException {
        authenticateAs("editor", CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR);
        seedAdvisories(3);

        AdvisoryInformationPageResponse page = advisoryService.getAdvisoryInformationsPage(null, 50, null);
        assertEquals(3, page.getAdvisories().size());
        assertFalse(page.isHasMore(), "fewer rows than the limit means the first page is the last");
        assertNull(page.getBookmark());
    }

    // ------------------------------------------------------------------------------------------
    // Visibility filter discards rows: a page still yields up to 'limit' VISIBLE rows.
    // ------------------------------------------------------------------------------------------

    @Test
    void visibilityFilterStillFillsPagesToLimit() throws IOException, CsafException {
        // An AUTHOR sees only their own (draft) advisories. We interleave advisories owned by two
        // users so that roughly half the raw rows are invisible to author1. Paging as author1 must
        // still return full pages of author1's visible rows, never short pages while more exist.
        final int perUser = 40; // 80 raw advisories, 40 visible to author1

        authenticateAs("author1", CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR);
        List<String> author1Ids = seedAdvisories(perUser);

        authenticateAs("author2", CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR);
        seedAdvisories(perUser);

        // Page as author1: only author1's drafts are visible.
        authenticateAs("author1", CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR);
        List<String> visiblePaged = collectAllPages(null, 15);

        assertEquals(perUser, visiblePaged.size(),
                "paging must return exactly the rows visible to author1");
        assertNoDuplicates(visiblePaged);
        assertTrue(visiblePaged.containsAll(author1Ids) && author1Ids.containsAll(visiblePaged),
                "the visible paged set must equal author1's own advisories");

        // None of author2's rows leaked into author1's pages: every paged id is author1's own.
        assertTrue(author1Ids.containsAll(visiblePaged),
                "no advisory owned by another user may appear in author1's pages");

        // Ordering matches the legacy endpoint's ordering rule for the same (filtered) user. Legacy
        // is capped at 25 RAW rows pre-filter, so for author1 it yields only the visible rows among
        // the first 25 raw rows, in order. Those must be a prefix of the full paged result.
        List<String> legacyVisible = advisoryService.getAdvisoryInformations(null).stream()
                .map(AdvisoryInformationResponse::getAdvisoryId).toList();
        assertFalse(legacyVisible.isEmpty(), "legacy must surface at least some visible rows");
        assertTrue(legacyVisible.size() < perUser,
                "legacy's 25 raw-row cap must drop some of author1's visible rows (else the test is moot)");
        assertEquals(legacyVisible, visiblePaged.subList(0, legacyVisible.size()),
                "visible paged order must start with the legacy visible rows in the same order");
    }

    // ------------------------------------------------------------------------------------------
    // Auditor: AdvisoryVersion rows follow the advisory rows, none lost at a page boundary.
    // ------------------------------------------------------------------------------------------

    @Test
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Bug in SpotBugs: https://github.com/spotbugs/spotbugs/issues/1338")
    void auditorReceivesAllAdvisoryVersionRowsAcrossPageBoundary()
            throws IOException, CsafException, DatabaseException {
        try (MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class)) {
            validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);

            // Seed and publish several advisories, then create a new version of each. Creating a new
            // version writes an AdvisoryVersion backup document, so an auditor's list contains both
            // the current advisories and their AdvisoryVersion rows (advisories first, then versions).
            authenticateAs("publisher", CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR,
                    CsafRoles.ROLE_EDITOR, CsafRoles.ROLE_REVIEWER, CsafRoles.ROLE_PUBLISHER);

            final int advisoryCount = 6;
            for (int i = 0; i < advisoryCount; i++) {
                var idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
                String rev = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), idRev.getRevision(),
                        WorkflowState.Review, null, null);
                rev = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), rev,
                        WorkflowState.Approved, null, null);
                rev = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), rev,
                        WorkflowState.RfPublication, null, null);
                rev = advisoryService.changeAdvisoryWorkflowState(idRev.getId(), rev,
                        WorkflowState.Published, null, null);
                // New version -> creates an AdvisoryVersion backup document.
                advisoryService.createNewCsafDocumentVersion(idRev.getId(), rev);
            }

            // Now page as an auditor, who sees the advisory rows and then the AdvisoryVersion rows.
            authenticateAs("auditor", CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR);

            // Legacy auditor view = advisories first, then versions; this is the reference set/order.
            List<AdvisoryInformationResponse> legacy = advisoryService.getAdvisoryInformations(null);
            List<String> legacyIds = legacy.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();
            // There must be at least one AdvisoryVersion row to make this test meaningful.
            assertTrue(legacy.size() > advisoryCount,
                    "the auditor must see advisory rows plus AdvisoryVersion rows");

            // Use a small limit so the page boundary falls inside the advisory stream and/or straddles
            // the advisory -> version stream transition; collect across pages.
            for (int limit : new int[] {1, 2, 3, advisoryCount}) {
                List<String> paged = collectAllPages(null, limit);
                assertNoDuplicates(paged);
                assertEquals(legacyIds.size(), paged.size(),
                        "auditor paging with limit=" + limit + " must lose no row at any page boundary");
                assertEquals(legacyIds, paged,
                        "auditor paged order with limit=" + limit
                        + " must match the legacy advisories-then-versions order");
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Bad cursor – fingerprint mismatch when the expression changes mid-pagination -> 400.
    // ------------------------------------------------------------------------------------------

    @Test
    void cursorFromDifferentExpressionIsRejected() throws IOException, CsafException {
        authenticateAs("editor", CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR, CsafRoles.ROLE_EDITOR);
        seedAdvisories(30);

        AdvisoryInformationPageResponse page = advisoryService.getAdvisoryInformationsPage(null, 10, null);
        assertTrue(page.isHasMore());
        String bookmark = page.getBookmark();
        assertNotNull(bookmark);

        // Reuse the bookmark under a different (non-null) expression: fingerprint mismatch -> 400.
        String otherExpression = buildTitleEqualsExpression("does-not-matter");
        CsafException ex = org.junit.jupiter.api.Assertions.assertThrows(CsafException.class,
                () -> advisoryService.getAdvisoryInformationsPage(otherExpression, 10, bookmark));
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getRecommendedHttpState());
    }

    private static String buildTitleEqualsExpression(String value) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode expr = mapper.createObjectNode();
        expr.put("type", "Operator");
        var selector = mapper.createArrayNode();
        selector.add("csaf");
        selector.add("document");
        selector.add("title");
        expr.set("selector", selector);
        expr.put("operatorType", "Equal");
        expr.put("value", value);
        expr.put("valueType", "Text");
        JsonNode node = expr;
        return node.toString();
    }
}
