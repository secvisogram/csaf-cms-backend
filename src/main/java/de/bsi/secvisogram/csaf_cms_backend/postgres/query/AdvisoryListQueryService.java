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
        SqlExpressionCompiler compiler = new SqlExpressionCompiler(ListColumn.ADVISORIES);
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
        SqlExpressionCompiler compiler = new SqlExpressionCompiler(ListColumn.VERSIONS);
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
