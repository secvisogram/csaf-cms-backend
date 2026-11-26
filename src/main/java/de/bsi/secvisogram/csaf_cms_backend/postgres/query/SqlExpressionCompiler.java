package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import de.bsi.secvisogram.csaf_cms_backend.model.filter.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Compiles a filter expression to SQL with bound parameters. The subclasses decide how a single comparison is
 * compiled:
 * <ul>
 *   <li>{@link LenientSqlExpressionCompiler} (list v1): {@code owner} and {@code workflowState} use their columns,
 *   every CSAF selector uses a JSON path on the document.</li>
 *   <li>{@link StrictSqlExpressionCompiler} (paged list of advisories, v2): only columns are accepted, everything
 *   else throws {@link UnsupportedFilterException}.</li>
 * </ul>
 */
abstract class SqlExpressionCompiler {

    /** The meta selectors of both compilers that are plain columns of the table. */
    static final Map<String, String> META_COLUMNS = Map.of(
            "owner", "owner",
            "workflowState", "workflow_state");

    private final List<Object> values = new ArrayList<>();

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

    /** One comparison of a selector with a value. */
    abstract String compileOperator(OperatorExpression expression);

    /** The CSAF selector without the optional leading {@code csaf}; empty if nothing is left. */
    static String[] csafPath(String[] selector) {
        int offset = selector.length > 0 && "csaf".equals(selector[0]) ? 1 : 0;
        return Arrays.copyOfRange(selector, offset, selector.length);
    }

    String columnComparison(String column, OperatorExpression expression) {
        return columnComparison(column, expression, null);
    }

    /** Compares the column with {@code rightSide}, or with the bound value of the expression if it is null. */
    String columnComparison(String column, OperatorExpression expression, String rightSide) {
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

    String bindValue(OperatorExpression expression) {
        return switch (expression.getValueType()) {
            case Decimal -> "CAST(" + bindText(expression.getValue()) + " AS numeric)";
            case Boolean -> "CAST(" + bindText(expression.getValue()) + " AS boolean)";
            case Text -> bindText(expression.getValue());
        };
    }

    private static String escapeLike(String value) {
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
