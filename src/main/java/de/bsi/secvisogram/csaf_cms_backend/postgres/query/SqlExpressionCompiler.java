package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import de.bsi.secvisogram.csaf_cms_backend.model.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Compiles a filter expression to SQL for a table with the given columns. Not strict (list v1): selectors
 * without a column use a JSON path on the document. Strict (paged list of advisories): only columns are
 * accepted, everything else throws {@link UnsupportedFilterException}, the selectors {@code lastUpdated}
 * and {@code createdAt} address {@code updated_at} and {@code created_at}, and the named numbers of
 * {@link #NUMBER_COLUMNS} take Decimal values.
 */
final class SqlExpressionCompiler {

    private static final Logger LOG = LoggerFactory.getLogger(SqlExpressionCompiler.class);

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

    private final List<Object> values = new ArrayList<>();
    private final boolean strict;
    private final Map<String, ListColumn> columns;

    SqlExpressionCompiler(boolean strict, Map<String, ListColumn> columns) {
        this.strict = strict;
        this.columns = columns;
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
        String[] selector = expression.getSelector();
        String numberColumn = selector.length == 1 ? NUMBER_COLUMNS.get(selector[0]) : null;
        if (strict && numberColumn != null) {
            return numberComparison(selector[0], numberColumn, expression);
        }
        if (strict && expression.getValueType() != TypeOfValue.Text) {
            throw new UnsupportedFilterException("Only text values are supported, not "
                    + expression.getValueType());
        }
        if (selector.length == 1 && "owner".equals(selector[0])) {
            return columnComparison("owner", expression);
        }
        if (selector.length == 1 && "workflowState".equals(selector[0])) {
            return columnComparison("workflow_state", expression);
        }
        String metaTimestamp = selector.length != 1 ? null : switch (selector[0]) {
            case "lastUpdated" -> "updated_at";
            case "createdAt" -> "created_at";
            default -> null;
        };
        if (strict && metaTimestamp != null) {
            String sql = timestampComparison(metaTimestamp, expression);
            if (sql == null) {
                throw new UnsupportedFilterException(selector[0]
                        + " needs an ISO-8601 date-time with offset and a comparison operator");
            }
            return sql;
        }
        int offset = selector.length > 0 && "csaf".equals(selector[0]) ? 1 : 0;
        if (offset == selector.length) {
            if (strict) {
                throw new UnsupportedFilterException("Missing selector");
            }
            return "TRUE";
        }
        String column = columnForSelector(selector, offset, expression);
        if (column != null) {
            return column;
        }
        if (strict) {
            throw new UnsupportedFilterException("Filter on " + String.join(".", selector)
                    + " is not supported: no column, or a timestamp that is not an ISO-8601 date-time with "
                    + "offset, or ContainsIgnoreCase on a timestamp");
        }
        String path = jsonPath(selector, offset);
        String comparisonPath = path + " ? (" + predicate(expression) + ")";
        String jsonPath = "jsonb_path_exists(CAST(csaf AS jsonb), CAST(" + bindText(comparisonPath) + " AS jsonpath)";
        if (expression.getOperatorType() == TypeOfOperator.ContainsIgnoreCase) {
            return jsonPath + ")";
        }
        return jsonPath + ", jsonb_build_object('value', to_jsonb(" + bindValue(expression) + ")))";
    }

    /**
     * A named number (strict only): the value must be a Decimal and is compared as a number. A NULL column (e.g. no
     * scores) never matches.
     */
    private String numberComparison(String name, String column, OperatorExpression expression) {
        if (expression.getValueType() != TypeOfValue.Decimal
                || expression.getOperatorType() == TypeOfOperator.ContainsIgnoreCase) {
            throw new UnsupportedFilterException(name + " needs a Decimal value and a comparison operator");
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

    /**
     * SQL for a selector that is stored in a column, or {@code null} if the filter has to use the JSON path.
     * The column answers text values only: a number or boolean compared with a string never matches in a
     * JSON path, so those keep using it.
     */
    private String columnForSelector(String[] selector, int offset, OperatorExpression expression) {
        String key = String.join(".", Arrays.copyOfRange(selector, offset, selector.length));
        ListColumn listColumn = columns.get(key);
        if (listColumn == null || expression.getValueType() != TypeOfValue.Text) {
            return null;
        }
        String sql = switch (listColumn.kind()) {
            case TEXT -> columnComparison(listColumn.column(), expression);
            case TIMESTAMP -> timestampComparison(listColumn.column(), expression);
            case TEXT_ARRAY -> arrayComparison(listColumn.column(), expression);
        };
        if (sql == null) {
            LOG.debug("Filter on {} uses the JSON path, not the column {}", key, listColumn.column());
        }
        return sql;
    }

    private String timestampComparison(String column, OperatorExpression expression) {
        if (expression.getOperatorType() == TypeOfOperator.ContainsIgnoreCase) {
            return null;
        }
        Instant instant;
        try {
            instant = OffsetDateTime.parse(expression.getValue()).toInstant();
        } catch (DateTimeParseException ex) {
            return null;
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

    private String jsonPath(String[] selector, int offset) {
        StringBuilder path = new StringBuilder("$");
        for (int index = offset; index < selector.length; index++) {
            path.append(".\"").append(selector[index].replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
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
