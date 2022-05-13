package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import de.bsi.secvisogram.csaf_cms_backend.model.filter.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Create the selector part of a CouchDB find expression as Cloudant FindOptions selector
 *
 * <a href="https://docs.couchdb.org/en/stable/api/database/find.html">CouchDB find</a>
 * <a href="https://cloud.ibm.com/apidocs/cloudant?code=java#postfind">Cloudant postfind</a>
 *  */
public class CouchDBFilterCreator {

    /**
     * Builder Method for CouchDBFilterCreator
     * @param expr expression to convert
     * @return Cloudant expression
     */
    public static Map<String, Object> expr2CouchDBFilter(Expression expr) {
        return new CouchDBFilterCreator().expression2CouchDbFilter(expr);
    }

    private final Map<TypeOfOperator, String> operator2CouchDB = Map.of(
            TypeOfOperator.Equal, "$eq",
            TypeOfOperator.Greater, "$gt",
            TypeOfOperator.GreaterOrEqual, "$gte",
            TypeOfOperator.Less, "$lt",
            TypeOfOperator.LessOrEqual, "$lte",
            TypeOfOperator.NotEqual, "$ne",
            TypeOfOperator.ContainsIgnoreCase, "$regex"
    );


    /**
     * Builder Method for CouchDBFilterCreator
     * @param expr expression to convert
     * @param arrayFieldSelectors Selectors that mark an array field in the JSON structure
     * @return Cloudant expression
     */
    public static Map<String, Object> expr2CouchDBFilter(Expression expr, String[] ... arrayFieldSelectors) {
        return new CouchDBFilterCreator(Arrays.asList(arrayFieldSelectors)).expression2CouchDbFilter(expr);
    }

    /**
     * Selectors that mark an array field in the JSON structure
     */
    private final List<String[]> arrayFieldSelectors;

    public CouchDBFilterCreator() {

        this.arrayFieldSelectors = Collections.emptyList();

    }

    public CouchDBFilterCreator(List<String[]> arrayFieldSelectors) {

        this.arrayFieldSelectors = Collections.unmodifiableList(arrayFieldSelectors);
    }

    /**
     * Create Cloudant expression for given expression
     * @param expr the expression to convert
     * @return the converted expression object
     */
    public Map<String, Object> expression2CouchDbFilter(Expression expr) {

        return expression2Filter(expr);
    }

    /**
     * Create recursive Cloudant expression for given expression
     * @param expr the expression to convert
     * @return the converted expression object
     */
    private Map<String, Object> expression2Filter(Expression expr) {

        ExpressionHandler<Map<String, Object>> couchDbExprHandler = new ExpressionHandler<>() {
            @Override
            public Map<String, Object> and(AndExpression andExpr) {
                List<Object> andExpressions = andExpr.getExpressions()
                        .stream()
                        .map(expr -> expression2Filter(expr))
                        .collect(Collectors.toList());

                return createAndExpression(andExpressions);
            }

            @Override
            public Map<String, Object> operator(OperatorExpression opExpr) {
                return createOperatorExpression(opExpr);
            }
        };
        return expr.handleExpr(couchDbExprHandler);
    }

    /**
     * Create Cloudant expression for given operator expression, like
     * <pre>
     *     "document": {
     *             "acknowledgments": {
     *                "$elemMatch": {
     *                   "urls": {
     *                      "$elemMatch": {
     *                         "$eq": "exccellent.de"
     *                      }
     *                   }
     *                }
     *             }
     *          }
     * </pre>
     * @param opExpr the expression to convert
     * @return the converted expression object
     */
    private Map<String, Object> createOperatorExpression(OperatorExpression opExpr) {

        final String couchDBOperator = convertOperator(opExpr);

        final Object compareValue;
        if (opExpr.getValueType() == TypeOfValue.Decimal) {
            compareValue = Double.valueOf(opExpr.getValue());
        } else if (opExpr.getValueType() == TypeOfValue.Boolean) {
            compareValue = "true".equals(opExpr.getValue());
        } else if (opExpr.getOperatorType() == TypeOfOperator.ContainsIgnoreCase) {
            compareValue = "^.*(?i)" + opExpr.getValue() + "(?i).*";
        } else {
            compareValue = opExpr.getValue();
        }

        return createSelectorExpression(opExpr.getSelector(), couchDBOperator, compareValue);
    }

    /**
     * Create operator selector expression
     * @param selector the json selector
     * @param couchDBOperator the couchDB operator: $gt, $eq, ...
     * @param compareValue the value to compare
     * @return the created selector expression
     */
    private Map<String, Object> createSelectorExpression(String[] selector, String couchDBOperator, Object compareValue) {
        // create subfields selector for nested objects
        Map<String, Object> lastOperator = Map.of(couchDBOperator, compareValue);
        for (int i = selector.length - 1; i >= 0; i--) {
            // a query on an array field needs a $elemMatch to be inserted
            if (isArrayFieldSelector(selector, i)) {
                lastOperator = Map.of("$elemMatch", lastOperator);
            }
            lastOperator = Map.of(selector[i], lastOperator);
        }

        return lastOperator;
    }

    /**
     * Checks whether the selector is a path to an array field in the JSON structure
     * @param selectorToCheck path to check
     * @param toIndex compare selector from 0 to toIndex
     * @return true - selector is an array
     */
    private boolean isArrayFieldSelector(String[] selectorToCheck, int toIndex) {

        Optional<String[]> arrayFieldSelector = this.arrayFieldSelectors.stream()
                .filter(selector -> Arrays.compare(selectorToCheck, 0, toIndex + 1, selector, 0, selector.length) == 0)
                .findFirst();

        return arrayFieldSelector.isPresent();
    }

    /** Convert expression operator to couchDB operator
     * @param opExpr the expression to get the operator
     * @return the couchDB operator
     * */
    private String convertOperator(OperatorExpression opExpr) {

        String couchDBOperator = operator2CouchDB.get(opExpr.getOperatorType());
        if (couchDBOperator == null) {
            throw new RuntimeException("Invalid Operator " + opExpr.getOperatorType().name());
        }
        return couchDBOperator;
    }

    /**
     * Create Cloudant And expression for list of expression
     * @param expressions the expression to add to the 'and' expression
     * @return the cloudant and expression object
     */
    private Map<String, Object> createAndExpression(List<Object> expressions) {

        return Map.of("$and", expressions);
   }
}
