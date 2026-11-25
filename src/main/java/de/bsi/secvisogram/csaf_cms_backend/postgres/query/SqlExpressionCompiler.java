package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import de.bsi.secvisogram.csaf_cms_backend.model.filter.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Compiles a filter expression to SQL.
 * <ul>
 *   <li>{@link #lenient()} (list v1): {@code owner} and {@code workflowState} use their columns, every CSAF
 *   selector uses a JSON path on the document.</li>
 *   <li>{@link #strict(Map)} (paged list of advisories, v2): only columns are accepted, everything else throws
 *   {@link UnsupportedFilterException}. CSAF selectors use the given list columns, {@code lastUpdated} and
 *   {@code createdAt} address {@code updated_at} and {@code created_at}, and the named numbers of
 *   {@link #NUMBER_COLUMNS} take Decimal values.</li>
 * </ul>
 */
final class SqlExpressionCompiler {

    /**
     * Named numbers of the paged list. They are aggregates (the highest CVSS base score, critical as v4, else v3,
     * else v2, the number of revisions), not values at a CSAF path, so they have their own selectors.
     */
    private static final Map<String, String> NUMBER_COLUMNS = Map.of(
            "cvssV2Score", "cvss_v2_score",
            "cvssV3Score", "cvss_v3_score",
            "cvssV4Score", "cvss_v4_score",
            "critical", "critical",
            "revisionHistoryLength", "revision_history_length");

    /** The meta selectors of both modes that are plain columns of the table. */
    private static final Map<String, String> META_COLUMNS = Map.of(
            "owner", "owner",
            "workflowState", "workflow_state");

    /** The meta selectors of the strict mode that are timestamp columns. */
    private static final Map<String, String> META_TIMESTAMP_COLUMNS = Map.of(
            "lastUpdated", "updated_at",
            "createdAt", "created_at");

    private final List<Object> values = new ArrayList<>();
    private final boolean strict;
    private final Map<String, ListColumn> columns;

    private SqlExpressionCompiler(boolean strict, Map<String, ListColumn> columns) {
        this.strict = strict;
        this.columns = columns;
    }

    /** Filters CSAF selectors by a JSON path on the document. */
    static SqlExpressionCompiler lenient() {
        return new SqlExpressionCompiler(false, Map.of());
    }

    /** Filters on columns only; CSAF selectors must be keys of {@code columns}. */
    static SqlExpressionCompiler strict(Map<String, ListColumn> columns) {
        return new SqlExpressionCompiler(true, columns);
    }

    String compile(Expression expression) {
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
        return "(" + expressions.stream().map(this::compile).reduce((a, b) -> a + separator + b).orElse(emptyValue)
                + ")";
    }

    private String compileOperator(OperatorExpression expression) {
        return strict ? compileOperatorStrict(expression) : compileOperatorLenient(expression);
    }

    /** Columns only: anything else throws with the reason. */
    private String compileOperatorStrict(OperatorExpression expression) {
        String[] selector = expression.getSelector();
        String name = String.join(".", selector);
        String numberColumn = NUMBER_COLUMNS.get(name);
        if (numberColumn != null) {
            return numberComparison(name, numberColumn, expression);
        }
        if (expression.getValueType() != TypeOfValue.Text) {
            throw new UnsupportedFilterException("Only text values are supported, not "
                    + expression.getValueType());
        }
        String metaColumn = META_COLUMNS.get(name);
        if (metaColumn != null) {
            return columnComparison(metaColumn, expression);
        }
        String metaTimestamp = META_TIMESTAMP_COLUMNS.get(name);
        if (metaTimestamp != null) {
            return timestampComparison(name, metaTimestamp, expression);
        }
        String[] path = csafPath(selector);
        if (path.length == 0) {
            throw new UnsupportedFilterException("Missing selector");
        }
        String key = String.join(".", path);
        ListColumn column = columns.get(key);
        if (column == null) {
            throw new UnsupportedFilterException("Filter on " + name
                    + " is not supported: the field is not stored as a column");
        }
        return switch (column.kind()) {
            case TEXT -> columnComparison(column.column(), expression);
            case TIMESTAMP -> timestampComparison(name, column.column(), expression);
            case TEXT_ARRAY -> arrayComparison(column.column(), expression);
        };
    }

    /** owner and workflowState use their columns, every CSAF selector uses a JSON path on the document. */
    private String compileOperatorLenient(OperatorExpression expression) {
        String[] selector = expression.getSelector();
        String metaColumn = META_COLUMNS.get(String.join(".", selector));
        if (metaColumn != null) {
            return columnComparison(metaColumn, expression);
        }
        String[] path = csafPath(selector);
        if (path.length == 0) {
            return "TRUE";
        }
        String comparisonPath = jsonPath(path) + " ? (" + predicate(expression) + ")";
        String jsonPath = "jsonb_path_exists(csaf, CAST(" + bindText(comparisonPath) + " AS jsonpath)";
        if (expression.getOperatorType() == TypeOfOperator.ContainsIgnoreCase) {
            return jsonPath + ")";
        }
        return jsonPath + ", jsonb_build_object('value', to_jsonb(" + bindValue(expression) + ")))";
    }

    /** The CSAF selector without the optional leading {@code csaf}; empty if nothing is left. */
    private static String[] csafPath(String[] selector) {
        int offset = selector.length > 0 && "csaf".equals(selector[0]) ? 1 : 0;
        return Arrays.copyOfRange(selector, offset, selector.length);
    }

    /**
     * A named number (strict only): the value must be a Decimal and is compared as a number. A NULL column (e.g. no
     * scores) never matches.
     */
    private String numberComparison(String name, String column, OperatorExpression expression) {
        if (expression.getValueType() != TypeOfValue.Decimal) {
            throw new UnsupportedFilterException(name + " needs a Decimal value, not "
                    + expression.getValueType());
        }
        if (expression.getOperatorType() == TypeOfOperator.ContainsIgnoreCase) {
            throw new UnsupportedFilterException("ContainsIgnoreCase is not supported on the number " + name);
        }
        String value = expression.getValue();
        BigDecimal number;
        try {
            number = new BigDecimal(value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            throw new UnsupportedFilterException(name + " needs a number, not '" + value + "'");
        }
        return columnComparison(column, expression, "CAST(" + bindText(number.toPlainString()) + " AS numeric)");
    }

    private String columnComparison(String column, OperatorExpression expression) {
        return columnComparison(column, expression, null);
    }

    private String columnComparison(String column, OperatorExpression expression, String rightSide) {
        return switch (expression.getOperatorType()) {
            case Equal -> column + " = " + valueOr(rightSide, expression);
            case NotEqual -> column + " <> " + valueOr(rightSide, expression);
            case Greater -> column + " > " + valueOr(rightSide, expression);
            case GreaterOrEqual -> column + " >= " + valueOr(rightSide, expression);
            case Less -> column + " < " + valueOr(rightSide, expression);
            case LessOrEqual -> column + " <= " + valueOr(rightSide, expression);
            case ContainsIgnoreCase -> "LOWER(" + column + ") LIKE '%' || LOWER("
                    + bindText(escapeLike(expression.getValue())) + ") || '%' ESCAPE E'\\\\'";
        };
    }

    private String valueOr(String rightSide, OperatorExpression expression) {
        return rightSide != null ? rightSide : bindValue(expression);
    }

    /** Compares a timestamp column with an ISO-8601 date-time with offset; anything else throws. */
    private String timestampComparison(String name, String column, OperatorExpression expression) {
        if (expression.getOperatorType() == TypeOfOperator.ContainsIgnoreCase) {
            throw new UnsupportedFilterException("ContainsIgnoreCase is not supported on the timestamp " + name);
        }
        String value = expression.getValue();
        Instant instant;
        try {
            instant = OffsetDateTime.parse(value == null ? "" : value).toInstant();
        } catch (DateTimeParseException ex) {
            throw new UnsupportedFilterException(name + " needs an ISO-8601 date-time with offset, e.g. "
                    + "2024-01-31T00:00:00Z, not '" + value + "'");
        }
        return columnComparison(column, expression, bindText(instant));
    }

    /** A document matches if any element matches, like a JSON path over an array does. */
    private String arrayComparison(String column, OperatorExpression expression) {
        if (expression.getOperatorType() == TypeOfOperator.Equal) {
            return column + " @> ARRAY[CAST(" + bindText(expression.getValue()) + " AS text)]";
        }
        return "EXISTS (SELECT 1 FROM unnest(" + column + ") AS e WHERE "
                + columnComparison("e", expression) + ")";
    }

    private String predicate(OperatorExpression expression) {
        return switch (expression.getOperatorType()) {
            case Equal -> "@ == $value";
            case NotEqual -> "@ != $value";
            case Greater -> "@ > $value";
            case GreaterOrEqual -> "@ >= $value";
            case Less -> "@ < $value";
            case LessOrEqual -> "@ <= $value";
            case ContainsIgnoreCase -> "@ like_regex " + jsonPathStringLiteral(
                    ".*" + escapeRegex(expression.getValue()) + ".*") + " flag \"i\"";
        };
    }

    private String jsonPath(String[] csafPath) {
        StringBuilder path = new StringBuilder("$");
        for (String part : csafPath) {
            path.append(".\"").append(part.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        return path.toString();
    }

    private String bindValue(OperatorExpression expression) {
        return switch (expression.getValueType()) {
            case Decimal -> "CAST(" + bindText(expression.getValue()) + " AS numeric)";
            case Boolean -> "CAST(" + bindText(expression.getValue()) + " AS boolean)";
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

    String bindText(Object value) {
        values.add(value);
        return ":p" + values.size();
    }

    void bind(jakarta.persistence.Query query) {
        for (int index = 0; index < values.size(); index++) {
            query.setParameter("p" + (index + 1), values.get(index));
        }
    }
}
