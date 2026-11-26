package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.TypeOfOperator;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.TypeOfValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * The compiler of the paged list of advisories (v2): only columns are accepted, everything else throws
 * {@link UnsupportedFilterException}. CSAF selectors use the given selector columns, {@code lastUpdated} and
 * {@code createdAt} address {@code updated_at} and {@code created_at}, and the named numbers of
 * {@link #NUMBER_COLUMNS} take Decimal values.
 */
final class StrictSqlExpressionCompiler extends SqlExpressionCompiler {

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

    /** The meta selectors that are timestamp columns. */
    private static final Map<String, String> META_TIMESTAMP_COLUMNS = Map.of(
            "lastUpdated", "updated_at",
            "createdAt", "created_at");

    private final Map<String, CsafSelectorColumn> columns;

    /** @param columns the CSAF selectors that may be filtered, with their columns */
    StrictSqlExpressionCompiler(Map<String, CsafSelectorColumn> columns) {
        this.columns = columns;
    }

    /** Columns only: anything else throws with the reason. */
    @Override
    String compileOperator(OperatorExpression expression) {
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
        CsafSelectorColumn column = columns.get(key);
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

    /** A named number: the value must be a Decimal and is compared as a number. A NULL column never matches. */
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
}
