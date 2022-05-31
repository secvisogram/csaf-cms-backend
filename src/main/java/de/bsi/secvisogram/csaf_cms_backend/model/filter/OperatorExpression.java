package de.bsi.secvisogram.csaf_cms_backend.model.filter;

/**
 * Expression which describes an Expression that compares a Property with a Value by a defined Operator
 */
public class OperatorExpression implements Expression {

    public static OperatorExpression containsIgnoreCase(String value, String ... path) {

        return new OperatorExpression(path, TypeOfOperator.ContainsIgnoreCase, value, TypeOfValue.Text);
    }

    public static OperatorExpression equal(String value, String ... path) {

        return new OperatorExpression(path, TypeOfOperator.Equal, value, TypeOfValue.Text);
    }

    public static OperatorExpression equal(double value, String ... path) {

        return new OperatorExpression(path, TypeOfOperator.Equal, Double.toString(value), TypeOfValue.Decimal);
    }

    public static OperatorExpression equal(boolean value, String ... path) {

        return new OperatorExpression(path, TypeOfOperator.Equal, value ? "true" : "false", TypeOfValue.Boolean);
    }

    public static OperatorExpression notEqual(String value, String ... path) {

        return new OperatorExpression(path, TypeOfOperator.NotEqual, value, TypeOfValue.Text);
    }

    public static OperatorExpression greaterOrEqual(String value, String ... path) {

        return new OperatorExpression(path, TypeOfOperator.GreaterOrEqual, value, TypeOfValue.Text);
    }

    public static OperatorExpression greater(String value, String ... path) {

        return new OperatorExpression(path, TypeOfOperator.Greater, value, TypeOfValue.Text);
    }

    public static OperatorExpression lessOrEqual(String value, String ... path) {

        return new OperatorExpression(path, TypeOfOperator.LessOrEqual, value, TypeOfValue.Text);
    }

    public static OperatorExpression less(String value, String ... path) {

        return new OperatorExpression(path, TypeOfOperator.Less, value, TypeOfValue.Text);
    }

    private String[] selector;
    private TypeOfOperator operatorType;
    private String value;
    private TypeOfValue valueType;

    public OperatorExpression() {

    }

    public OperatorExpression(String[] selector, TypeOfOperator operatorType, String value, TypeOfValue valueType) {
        super();
        this.selector = selector.clone();
        this.operatorType = operatorType;
        this.value = value;
        this.valueType = valueType;
    }

    public String[] getSelector() {
        return selector.clone();
    }

    public TypeOfOperator getOperatorType() {
        return this.operatorType;
    }

    public String getValue() {
        return this.value;
    }

    public TypeOfValue getValueType() {
        return valueType;
    }

    public <TResult> TResult handleExpr(ExpressionHandler<TResult> handler) {

        return handler.operator(this);
    }

}
