package de.bsi.secvisogram.csaf_cms_backend.model.filter;

/**
 * Interface of all possible expressions
 */
public class FilterExpression {

    private final TypeOfExpression expressionType;

    private final AndExpression andExpr;
    private final OrExpression orExpr;
    private final OperatorExpression operatorExpr;

    public FilterExpression(AndExpression andExpr) {
        this.andExpr = andExpr;
        this.orExpr = null;
        this.operatorExpr = null;
        this.expressionType = TypeOfExpression.And;
    }

    public FilterExpression(OrExpression orExpr) {

        this.andExpr = null;
        this.orExpr = orExpr;
        this.operatorExpr = null;
        this.expressionType = TypeOfExpression.Or;
    }

    public FilterExpression(OperatorExpression operatorExpr) {

        this.andExpr = null;
        this.orExpr = null;
        this.operatorExpr = operatorExpr;
        this.expressionType = TypeOfExpression.Or;
    }


    public TypeOfExpression getExpressionType() {
        return expressionType;
    }

    public AndExpression getAndExpr() {
        return andExpr;
    }

    public OrExpression getOrExpr() {
        return orExpr;
    }

    public OperatorExpression getOperatorExpr() {
        return operatorExpr;
    }
}
