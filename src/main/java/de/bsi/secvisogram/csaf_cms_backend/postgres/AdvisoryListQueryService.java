package de.bsi.secvisogram.csaf_cms_backend.postgres;

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
import java.util.Set;

/** Executes the advisory list query without selecting the large {@code csaf} column. */
@Service
public class AdvisoryListQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryListQueryService.class);
    @PersistenceContext
    private EntityManager entityManager;

    public List<AdvisoryListProjection> findAdvisories(Expression filter, Authentication credentials) {
        LOG.debug("Start getAdvisoryInformations");
        SqlExpressionCompiler compiler = new SqlExpressionCompiler();
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

        LOG.debug("Start getAdvisoryInformations - createNativeQuery");
        var query = entityManager.createNativeQuery(sql);
        compiler.bind(query);
        LOG.debug("Start getAdvisoryInformations - getResultList");
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        LOG.debug("Start getAdvisoryInformations - stream");
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

            // For Equal, use the @> containment operator which can leverage GIN indexes.
            if (expression.getOperatorType() == TypeOfOperator.Equal) {
                return compileContainment(selector, offset, expression);
            }

            String path = jsonPath(selector, offset);
            String comparisonPath = path + " ? (" + predicate(expression) + ")";
            return "jsonb_path_exists(csaf, CAST(" + bindText(comparisonPath) + " AS jsonpath))";
        }

        /**
         * CSAF array paths derived from the Secvisogram UI schema metadata for both
         * CSAF 2.0 and CSAF 2.1. Each entry is a dotted path (without the leading
         * {@code $}) whose last segment is an array in the CSAF JSON structure.
         * The union of both versions ensures correct containment queries regardless
         * of the document's CSAF version.
         *
         * @see <a href="https://github.com/secvisogram/secvisogram/blob/main/app/lib/uiSchemas/csaf_2_0/metaData.js">
         *     metaData.js — Secvisogram CSAF 2.0 UI schema</a>
         * @see <a href="https://github.com/secvisogram/secvisogram/blob/main/app/lib/uiSchemas/csaf_2_1/metaData.js">
         *     metaData.js — Secvisogram CSAF 2.1 UI schema</a>
         */
        private static final Set<String> CSAF_ARRAY_PATHS = Set.of(
                // --- document ---
                "document.acknowledgments",
                "document.acknowledgments.names",
                "document.acknowledgments.urls",
                "document.involvement.actions",                              // 2.1
                "document.involvement.actions.acting_entity_refs",           // 2.1
                "document.involvement.actions.dl_vuln_ids",                  // 2.1
                "document.involvement.actions.group_ids",                    // 2.1
                "document.involvement.actions.product_ids",                  // 2.1
                "document.involvement.actions.receiving_entity_refs",        // 2.1
                "document.involvement.actions.referenced_action_ids",        // 2.1
                "document.involvement.entities",                             // 2.1
                "document.involvement.entity_groups",                        // 2.1
                "document.involvement.entity_groups.entity_ids",             // 2.1
                "document.notes",
                "document.notes.group_ids",                                  // 2.1
                "document.notes.product_ids",                                // 2.1
                "document.references",
                "document.tracking.aliases",
                "document.tracking.revision_history",
                "document.x_extensions",                                     // 2.1
                // --- product_tree ---
                "product_tree.branches",
                "product_tree.branches.product.product_identification_helper.hashes",
                "product_tree.branches.product.product_identification_helper.hashes.file_hashes",
                "product_tree.branches.product.product_identification_helper.model_numbers",
                "product_tree.branches.product.product_identification_helper.purls",             // 2.1
                "product_tree.branches.product.product_identification_helper.sbom_urls",
                "product_tree.branches.product.product_identification_helper.serial_numbers",
                "product_tree.branches.product.product_identification_helper.skus",
                "product_tree.branches.product.product_identification_helper.x_generic_uris",
                "product_tree.branches.product.x_extensions",                // 2.1
                "product_tree.full_product_names",
                "product_tree.full_product_names.product_identification_helper.hashes",
                "product_tree.full_product_names.product_identification_helper.hashes.file_hashes",
                "product_tree.full_product_names.product_identification_helper.model_numbers",
                "product_tree.full_product_names.product_identification_helper.purls",           // 2.1
                "product_tree.full_product_names.product_identification_helper.sbom_urls",
                "product_tree.full_product_names.product_identification_helper.serial_numbers",
                "product_tree.full_product_names.product_identification_helper.skus",
                "product_tree.full_product_names.product_identification_helper.x_generic_uris",
                "product_tree.full_product_names.x_extensions",              // 2.1
                "product_tree.product_groups",
                "product_tree.product_groups.product_ids",
                "product_tree.product_paths",                                // 2.1
                "product_tree.product_paths.full_product_name.product_identification_helper.hashes",          // 2.1
                "product_tree.product_paths.full_product_name.product_identification_helper.hashes.file_hashes", // 2.1
                "product_tree.product_paths.full_product_name.product_identification_helper.model_numbers",   // 2.1
                "product_tree.product_paths.full_product_name.product_identification_helper.purls",           // 2.1
                "product_tree.product_paths.full_product_name.product_identification_helper.sbom_urls",       // 2.1
                "product_tree.product_paths.full_product_name.product_identification_helper.serial_numbers",  // 2.1
                "product_tree.product_paths.full_product_name.product_identification_helper.skus",            // 2.1
                "product_tree.product_paths.full_product_name.product_identification_helper.x_generic_uris", // 2.1
                "product_tree.product_paths.full_product_name.x_extensions",                                  // 2.1
                "product_tree.product_paths.subpaths",                       // 2.1
                "product_tree.relationships",
                "product_tree.relationships.full_product_name.product_identification_helper.hashes",
                "product_tree.relationships.full_product_name.product_identification_helper.hashes.file_hashes",
                "product_tree.relationships.full_product_name.product_identification_helper.model_numbers",
                "product_tree.relationships.full_product_name.product_identification_helper.purls",           // 2.1
                "product_tree.relationships.full_product_name.product_identification_helper.sbom_urls",
                "product_tree.relationships.full_product_name.product_identification_helper.serial_numbers",
                "product_tree.relationships.full_product_name.product_identification_helper.skus",
                "product_tree.relationships.full_product_name.product_identification_helper.x_generic_uris",
                // --- vulnerabilities ---
                "vulnerabilities",
                "vulnerabilities.acknowledgments",
                "vulnerabilities.acknowledgments.names",
                "vulnerabilities.acknowledgments.urls",
                "vulnerabilities.cwes",                                      // 2.1
                "vulnerabilities.first_known_exploitation_dates",            // 2.1
                "vulnerabilities.first_known_exploitation_dates.group_ids",  // 2.1
                "vulnerabilities.first_known_exploitation_dates.product_ids", // 2.1
                "vulnerabilities.flags",
                "vulnerabilities.flags.group_ids",
                "vulnerabilities.flags.product_ids",
                "vulnerabilities.ids",
                "vulnerabilities.ids.group_ids",                             // 2.1
                "vulnerabilities.ids.product_ids",                           // 2.1
                "vulnerabilities.involvements",                              // 2.0
                "vulnerabilities.metrics",                                   // 2.1
                "vulnerabilities.metrics.content.x_extensions",              // 2.1
                "vulnerabilities.metrics.products",                          // 2.1
                "vulnerabilities.notes",
                "vulnerabilities.notes.group_ids",                           // 2.1
                "vulnerabilities.notes.product_ids",                         // 2.1
                "vulnerabilities.product_status.first_affected",
                "vulnerabilities.product_status.first_fixed",
                "vulnerabilities.product_status.fixed",
                "vulnerabilities.product_status.known_affected",
                "vulnerabilities.product_status.known_not_affected",
                "vulnerabilities.product_status.last_affected",
                "vulnerabilities.product_status.recommended",
                "vulnerabilities.product_status.under_investigation",
                "vulnerabilities.product_status.unknown",                    // 2.1
                "vulnerabilities.references",
                "vulnerabilities.remediations",
                "vulnerabilities.remediations.entitlements",
                "vulnerabilities.remediations.group_ids",
                "vulnerabilities.remediations.product_ids",
                "vulnerabilities.scores",                                    // 2.0
                "vulnerabilities.scores.products",                           // 2.0
                "vulnerabilities.threats",
                "vulnerabilities.threats.group_ids",
                "vulnerabilities.threats.product_ids",
                "vulnerabilities.x_extensions",                              // 2.1
                // --- top-level ---
                "x_extensions"                                               // 2.1
        );

        /**
         * Returns whether the CSAF path up to and including segment index {@code end}
         * (relative to the selector starting at {@code offset}) denotes an array.
         * The path is built as {@code selector[offset] + "." + … + selector[end]}.
         */
        private static boolean isArrayPath(String[] selector, int offset, int end) {
            StringBuilder path = new StringBuilder();
            for (int i = offset; i <= end; i++) {
                if (i > offset) path.append('.');
                path.append(selector[i]);
            }
            return CSAF_ARRAY_PATHS.contains(path.toString());
        }

        /**
         * Compiles an Equal expression using the {@code @>} containment operator,
         * which can leverage GIN indexes on the {@code csaf} column or sub-paths.
         *
         * <p>Uses the CSAF schema metadata ({@link #CSAF_ARRAY_PATHS}) to determine
         * which intermediate levels are arrays and wraps only those levels in
         * {@code jsonb_build_array(…)}, producing a single containment expression
         * instead of an exponential number of OR-branches.</p>
         *
         * <p>For a selector like {@code ["csaf", "document", "tracking", "revision_history", "summary"]}
         * with value {@code "some text"}, the method knows that {@code revision_history}
         * is an array and generates:</p>
         * <pre>
         *   csaf @> jsonb_build_object('document',
         *            jsonb_build_object('tracking',
         *              jsonb_build_object('revision_history',
         *                jsonb_build_array(
         *                  jsonb_build_object('summary', to_jsonb(:p1::text))))))
         * </pre>
         *
         * @see <a href="https://github.com/secvisogram/secvisogram/blob/main/app/lib/uiSchemas/csaf_2_1/metaData.js">
         *     Secvisogram CSAF 2.1 metaData.js</a>
         */
        private String compileContainment(String[] selector, int offset, OperatorExpression expression) {
            String leafKey = selector[selector.length - 1];

            String valueParam = bindText(expression.getValue());
            String typedValue = switch (expression.getValueType()) {
                case Text -> "to_jsonb(CAST(" + valueParam + " AS text))";
                case Decimal -> "to_jsonb(CAST(" + valueParam + " AS numeric))";
                case Boolean -> "to_jsonb(CAST(" + valueParam + " AS boolean))";
            };

            // Start from the leaf: {"leafKey": typedValue}
            String inner = "jsonb_build_object(" + bindText(leafKey) + ", " + typedValue + ")";

            int segmentCount = selector.length - offset;
            if (segmentCount == 1) {
                // Top-level key (e.g. csaf.title): csaf @> {"title": "value"}.
                return "csaf @> " + inner;
            }

            // Build inside-out from the second-to-last segment up to the first.
            // At each level, wrap the inner value in jsonb_build_array if the
            // path up to that segment is a known array, otherwise wrap in an object.
            for (int i = selector.length - 2; i >= offset; i--) {
                String key = bindText(selector[i]);
                if (isArrayPath(selector, offset, i)) {
                    // This segment is an array: wrap the inner object in an array first,
                    // then nest it under the key.
                    inner = "jsonb_build_object(" + key + ", jsonb_build_array(" + inner + "))";
                } else {
                    inner = "jsonb_build_object(" + key + ", " + inner + ")";
                }
            }

            return "csaf @> " + inner;
        }

        private String columnComparison(String column, OperatorExpression expression) {
            return switch (expression.getOperatorType()) {
                case Equal -> column + " = " + bindValue(expression);
                case NotEqual -> column + " <> " + bindValue(expression);
                case Greater -> column + " > " + bindValue(expression);
                case GreaterOrEqual -> column + " >= " + bindValue(expression);
                case Less -> column + " < " + bindValue(expression);
                case LessOrEqual -> column + " <= " + bindValue(expression);
                case ContainsIgnoreCase -> "LOWER(" + column + ") LIKE '%' || LOWER("
                        + bindText(escapeLike(expression.getValue())) + ") || '%' ESCAPE E'\\\\'";
            };
        }

        private String predicate(OperatorExpression expression) {
            String literal = jsonPathLiteral(expression);
            return switch (expression.getOperatorType()) {
                case Equal -> "@ == " + literal;
                case NotEqual -> "@ != " + literal;
                case Greater -> "@ > " + literal;
                case GreaterOrEqual -> "@ >= " + literal;
                case Less -> "@ < " + literal;
                case LessOrEqual -> "@ <= " + literal;
                case ContainsIgnoreCase -> "@ like_regex " + jsonPathStringLiteral(
                        ".*" + escapeRegex(expression.getValue()) + ".*") + " flag \"i\"";
            };
        }

        /**
         * Returns a jsonpath literal for the expression's value, properly typed.
         * Strings are double-quoted and escaped, numerics and booleans are bare.
         */
        private String jsonPathLiteral(OperatorExpression expression) {
            return switch (expression.getValueType()) {
                case Text -> jsonPathStringLiteral(expression.getValue());
                case Decimal, Boolean -> expression.getValue();
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
                case Text -> bindText(expression.getValue());
            };
        }

        private String jsonPathStringLiteral(String value) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }

        private String escapeRegex(String value) {
            StringBuilder escaped = new StringBuilder();
            for (int index = 0; index < value.length(); index++) {
                char character = value.charAt(index);
                if ("\\\\.^$|?*+()[]{}".indexOf(character) >= 0) {
                    escaped.append('\\');
                }
                escaped.append(character);
            }
            return escaped.toString();
        }

        private String escapeLike(String value) {
            return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
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