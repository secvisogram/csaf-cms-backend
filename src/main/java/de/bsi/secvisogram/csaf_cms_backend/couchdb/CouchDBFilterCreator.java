package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryJsonService;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CouchDBFilterCreator {

    public static Map<String, Object> expr2CouchDBFilter(Expression expr) {
        return new CouchDBFilterCreator().expression2CouchDbFilter(expr);
    }

    public Map<String, Object> expression2CouchDbFilter(Expression expr) {

        return expression2Filter(expr);
    }


    private Map<String, Object> expression2Filter(Expression expr) {

        Map<String, Object> result;
        if (expr instanceof AndExpression) {
            AndExpression andExpr = (AndExpression) expr;
            List<Object> andExpressions = andExpr.getExpressions()
                    .stream()
                    .map(expression -> expression2Filter(expression))
                    .collect(Collectors.toList());

            result = createAndExpression(andExpressions);
        } else if (expr instanceof OperatorExpression) {

            OperatorExpression opExpr = (OperatorExpression) expr;
            result = createOperatorExpression(opExpr);
        } else {
            throw new RuntimeException("Invalid Expression Type " + expr.getClass().getName());
        }
        return result;
    }

    private Map<String, Object> createOperatorExpression(OperatorExpression opExpr) {

        String couchDBOperator;
        if (opExpr.getOperatorType() == TypeOfOperator.Equal) {
            couchDBOperator = "$eq";
        } else if (opExpr.getOperatorType() == TypeOfOperator.Greater) {
            couchDBOperator = "$gt";
        } else if (opExpr.getOperatorType() == TypeOfOperator.GreaterOrEqual) {
            couchDBOperator = "$gte";
        } else if (opExpr.getOperatorType() == TypeOfOperator.Less) {
            couchDBOperator = "$lt";
        } else if (opExpr.getOperatorType() == TypeOfOperator.LessOrEqual) {
            couchDBOperator = "$lte";
        } else if (opExpr.getOperatorType() == TypeOfOperator.NotEqual) {
            couchDBOperator = "$ne";
        } else {
            throw new RuntimeException("Invalid Operator " + opExpr.getOperatorType().name());
        }

        Object value;
        if (opExpr.getValueType() == TypeOfValue.Decimal) {
            value = Double.parseDouble(opExpr.getValue());
        } else if (opExpr.getValueType() == TypeOfValue.Boolean) {
            value = opExpr.getValue().equals("true");
        } else {
            value = opExpr.getValue();
        }

        Map<String, Object> operatorMap = Map.of(couchDBOperator, value);
        Map<String, Object> exprMap = Map.of(opExpr.getPathInJson()[0], operatorMap);

        return exprMap;
    }

    private Map<String, Object> createAndExpression(List<Object> expressions) {

        return Map.of("$and", expressions);
   }
}
