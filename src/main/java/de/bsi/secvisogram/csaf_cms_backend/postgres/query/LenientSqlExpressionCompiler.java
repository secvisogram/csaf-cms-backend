package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.TypeOfOperator;

/**
 * The compiler of the list v1: {@code owner} and {@code workflowState} use their columns, every CSAF selector uses a
 * JSON path on the document, so any CSAF path can be filtered.
 */
final class LenientSqlExpressionCompiler extends SqlExpressionCompiler {

    @Override
    String compileOperator(OperatorExpression expression) {
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
        String jsonPath = "jsonb_path_exists(csaf_jsonb, CAST(" + bindText(comparisonPath) + " AS jsonpath)";
        if (expression.getOperatorType() == TypeOfOperator.ContainsIgnoreCase) {
            return jsonPath + ")";
        }
        return jsonPath + ", jsonb_build_object('value', to_jsonb(" + bindValue(expression) + ")))";
    }

    private static String predicate(OperatorExpression expression) {
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

    private static String jsonPath(String[] csafPath) {
        StringBuilder path = new StringBuilder("$");
        for (String part : csafPath) {
            path.append(".\"").append(part.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        return path.toString();
    }

    private static String jsonPathStringLiteral(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String escapeRegex(String value) {
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
}
