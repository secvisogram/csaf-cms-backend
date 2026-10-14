package de.bsi.secvisogram.csaf_cms_backend.service;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.*;
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
        LOG.info("Start getAdvisoryInformations");
        SqlExpressionCompiler compiler = new SqlExpressionCompiler();
        String compiledExpr = compiler.compile(filter);
        LOG.info("Compiled expression: {}", compiledExpr);
        String where = visibilityClause(credentials, compiler) + " AND " + compiledExpr;
        String sql = """
                SELECT id, workflow_state, owner, version,
                       tracking_id,
                       document_title,
                       current_release_date
                  FROM advisories
                 WHERE""" + " " + where + "\n" + """
                 ORDER BY id
                 """;

        LOG.info("Start getAdvisoryInformations - createNativeQuery");
        var query = entityManager.createNativeQuery(sql);
        compiler.bind(query);
        LOG.info("Start getAdvisoryInformations - getResultList");
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        LOG.info("Start getAdvisoryInformations - stream");
        return rows.stream().map(row -> new AdvisoryListProjection(
                row[0].toString(), (String) row[1], (String) row[2], ((Number) row[3]).longValue(),
                (String) row[4], (String) row[5], row[6] == null ? null : row[6].toString())).toList();
    }

    /** Executes the advisory version list query without selecting the large {@code csaf} column. */
    public List<AdvisoryVersionListProjection> findAdvisoryVersions(Expression filter) {
        SqlExpressionCompiler compiler = new SqlExpressionCompiler();
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

    private static final class SqlExpressionCompiler {
        private final List<Object> values = new ArrayList<>();

        private String compile(Expression expression) {
            if (expression == null) {
                return "TRUE";
            }
            if (expression instanceof AndExpression and) {
                return join(and.getExpressions(), " AND ", "TRUE");
            }
            if (expression instanceof OrExpression or) {
                return join(or.getExpressions(), " OR ", "FALSE");
            }
            if (expression instanceof OperatorExpression operator) {
                return compileOperator(operator);
            }
            return "TRUE";
        }

        private String join(List<Expression> expressions, String separator, String emptyValue) {
            if (expressions.isEmpty()) {
                return emptyValue;
            }
            return "(" + expressions.stream().map(this::compile).reduce((a, b) -> a + separator + b).orElse(emptyValue) + ")";
        }

        private String compileOperator(OperatorExpression expression) {
            String[] selector = expression.getSelector();
            if (selector.length == 1 && "owner".equals(selector[0])) {
                return columnComparison("owner", expression);
            }
            if (selector.length == 1 && "workflowState".equals(selector[0])) {
                return columnComparison("workflow_state", expression);
            }
            int offset = selector.length > 0 && "csaf".equals(selector[0]) ? 1 : 0;
            if (offset == selector.length) {
                return "TRUE";
            }
            String path = jsonPath(selector, offset);
            String comparisonPath = path + " ? (" + predicate(expression) + ")";
            return "jsonb_path_exists(csaf, CAST(" + bindText(comparisonPath)
                    + " AS jsonpath), jsonb_build_object('value', to_jsonb(" + bindValue(expression) + ")))";
        }

        private String columnComparison(String column, OperatorExpression expression) {
            String value = bindValue(expression);
            return switch (expression.getOperatorType()) {
                case Equal -> column + " = " + value;
                case NotEqual -> column + " <> " + value;
                case Greater -> column + " > " + value;
                case GreaterOrEqual -> column + " >= " + value;
                case Less -> column + " < " + value;
                case LessOrEqual -> column + " <= " + value;
                case ContainsIgnoreCase -> "LOWER(" + column + ") LIKE '%' || LOWER(" + value + ") || '%'";
            };
        }

        private String predicate(OperatorExpression expression) {
            return switch (expression.getOperatorType()) {
                case Equal -> "@ == $value";
                case NotEqual -> "@ != $value";
                case Greater -> "@ > $value";
                case GreaterOrEqual -> "@ >= $value";
                case Less -> "@ < $value";
                case LessOrEqual -> "@ <= $value";
                case ContainsIgnoreCase -> "@ like_regex $value flag \"i\"";
            };
        }

        private String jsonPath(String[] selector, int offset) {
            StringBuilder path = new StringBuilder("$");
            for (int index = offset; index < selector.length; index++) {
                path.append(".\"").append(selector[index].replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
            }
            return path.toString();
        }

        private String bindValue(OperatorExpression expression) {
            return switch (expression.getValueType()) {
                case Decimal -> bindText(expression.getValue()) + "::numeric";
                case Boolean -> bindText(expression.getValue()) + "::boolean";
                case Text -> bindText(expression.getOperatorType() == TypeOfOperator.ContainsIgnoreCase
                        ? ".*" + expression.getValue().replace("\\", "\\\\").replace(".", "\\.") + ".*"
                        : expression.getValue());
            };
        }

        private String bindText(Object value) {
            values.add(value);
            return ":p" + values.size();
        }

        private void bind(jakarta.persistence.Query query) {
            for (int index = 0; index < values.size(); index++) {
                query.setParameter("p" + (index + 1), values.get(index));
            }
        }
    }
}