package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.Expression;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Executes the advisory list query without selecting the large {@code csaf} column. */
@Service
public class AdvisoryListQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryListQueryService.class);
    @PersistenceContext
    private EntityManager entityManager;

    public List<AdvisoryListProjection> findAdvisories(Expression filter, Authentication credentials) {
        LOG.debug("Start findAdvisories");
        SqlExpressionCompiler compiler = new SqlExpressionCompiler(false, ListColumn.ADVISORIES);
        String compiledExpr = compiler.compile(filter);
        String where = visibilityClause(credentials, compiler) + " AND " + compiledExpr;
        String sql = """
                SELECT id, workflow_state, owner, version,
                       tracking_id,
                       document_title,
                       current_release_date,
                       updated_at
                  FROM advisories
                 WHERE""" + " " + where + "\n" + """
                 ORDER BY id
                 """;

        LOG.debug("Start findAdvisories - createNativeQuery");
        var query = entityManager.createNativeQuery(sql);
        compiler.bind(query);
        LOG.debug("Start findAdvisories - getResultList");
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        LOG.debug("Start findAdvisories - stream");
        return rows.stream().map(row -> new AdvisoryListProjection(
                row[0].toString(), (String) row[1], (String) row[2], ((Number) row[3]).longValue(),
                (String) row[4], (String) row[5], row[6] == null ? null : row[6].toString(),
                toIsoInstant(row[7]))).toList();
    }

    private static String toIsoInstant(Object timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp instanceof java.sql.Timestamp sqlTimestamp) {
            return sqlTimestamp.toInstant().toString();
        }
        if (timestamp instanceof java.time.OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant().toString();
        }
        return timestamp.toString();
    }

    /** Executes the advisory version list query without selecting the large {@code csaf} column. */
    public List<AdvisoryVersionListProjection> findAdvisoryVersions(Expression filter) {
        SqlExpressionCompiler compiler = new SqlExpressionCompiler(false, ListColumn.VERSIONS);
        String sql = """
                SELECT advisory_id, workflow_state, owner,
                       tracking_id,
                       document_title,
                       current_release_date
                  FROM advisory_versions
                 WHERE""" + " " + compiler.compile(filter) + "\n" + """
                 ORDER BY id
                 """;

        var query = entityManager.createNativeQuery(sql);
        compiler.bind(query);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new AdvisoryVersionListProjection(
                row[0].toString(), (String) row[1], (String) row[2],
                (String) row[3], (String) row[4], row[5] == null ? null : row[5].toString())).toList();
    }

    /**
     * One page of the advisories the user may see, and, if {@code includeVersions} is set, of the snapshots in
     * {@code advisory_versions}, both in one sorted list. Only filters on columns are accepted (no JSON path).
     *
     * @throws UnsupportedFilterException if the filter uses a field or value that is not backed by a column
     */
    public AdvisoryListPage findAdvisoryPage(Expression filter, List<AdvisoryListSort> sort, int page, int size,
                                             Authentication credentials, boolean includeVersions) {
        SqlExpressionCompiler compiler = new SqlExpressionCompiler(true, ListColumn.ADVISORIES);
        String advisoryWhere = visibilityClause(credentials, compiler) + " AND "
                + compiler.compile(filter, "updated_at");
        StringBuilder listed = new StringBuilder("""
                SELECT id, id AS advisory_id, FALSE AS is_version, workflow_state, owner, version,
                       tracking_id, document_title, current_release_date, updated_at,
                       tracking_version, tracking_status, category, tlp
                  FROM advisories
                 WHERE\s""").append(advisoryWhere);
        if (includeVersions) {
            listed.append("""

                    UNION ALL
                    SELECT id, advisory_id, TRUE, workflow_state, owner, NULL,
                           tracking_id, document_title, current_release_date, created_at,
                           tracking_version, tracking_status, category, tlp
                      FROM advisory_versions
                     WHERE\s""").append(compiler.compile(filter, "created_at"));
        }
        String with = "WITH listed AS (\n" + listed + "\n)\n";

        // the total comes with the page rows; only a page beyond the end needs a separate count
        String sql = with + """
                SELECT advisory_id, is_version, workflow_state, owner, version,
                       tracking_id, document_title, current_release_date, updated_at,
                       count(*) OVER () AS total
                  FROM listed
                 ORDER BY\s""" + orderBy(sort) + "\n LIMIT " + size + " OFFSET " + ((long) page * size);
        var query = entityManager.createNativeQuery(sql);
        compiler.bind(query);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        long total;
        if (!rows.isEmpty()) {
            total = ((Number) rows.get(0)[9]).longValue();
        } else if (page == 0) {
            total = 0;
        } else {
            var countQuery = entityManager.createNativeQuery(with + "SELECT count(*) FROM listed");
            compiler.bind(countQuery);
            total = ((Number) countQuery.getSingleResult()).longValue();
        }

        List<AdvisoryListPage.Entry> entries = rows.stream().map(row -> new AdvisoryListPage.Entry(
                (Boolean) row[1],
                new AdvisoryListProjection(row[0].toString(), (String) row[2], (String) row[3],
                        row[4] == null ? null : ((Number) row[4]).longValue(),
                        (String) row[5], (String) row[6], toIsoInstant(row[7]), toIsoInstant(row[8])))).toList();
        return new AdvisoryListPage(entries, total);
    }

    /** The sort criteria, then the id so that the order and therefore the pages are stable. */
    private static String orderBy(List<AdvisoryListSort> sort) {
        List<String> parts = new ArrayList<>();
        for (AdvisoryListSort criterion : sort) {
            parts.add(criterion.field().getColumn() + (criterion.descending() ? " DESC" : " ASC") + " NULLS LAST");
        }
        parts.add("id ASC");
        return String.join(", ", parts);
    }

    private String visibilityClause(Authentication credentials, SqlExpressionCompiler compiler) {
        if (hasRole(credentials, CsafRoles.ROLE_AUDITOR) || hasRole(credentials, CsafRoles.ROLE_EDITOR)) {
            return "TRUE";
        }
        List<String> clauses = new ArrayList<>();
        clauses.add("(workflow_state = 'Published' AND "
                + "current_release_date < " + compiler.bindText(Instant.now()) + ")");
        if (hasRole(credentials, CsafRoles.ROLE_AUTHOR)) {
            clauses.add("owner = " + compiler.bindText(credentials.getName()));
        }
        if (hasRole(credentials, CsafRoles.ROLE_PUBLISHER)) {
            clauses.add("workflow_state IN ('Draft', 'Approved', 'RfPublication')");
        }
        if (hasRole(credentials, CsafRoles.ROLE_REVIEWER)) {
            clauses.add("(workflow_state = 'Review' AND owner <> " + compiler.bindText(credentials.getName()) + ")");
        }
        return "(" + String.join(" OR ", clauses) + ")";
    }

    private boolean hasRole(Authentication credentials, String role) {
        return credentials.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(role::equals);
    }
}
