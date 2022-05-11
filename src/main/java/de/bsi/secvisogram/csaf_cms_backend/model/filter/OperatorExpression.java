package de.bsi.secvisogram.csaf_cms_backend.model.filter;

/**
 * Expression which describes an Expression that compares a Property with a Value by a defined Operator
 */
public class OperatorExpression implements Expression {

    private String[] pathInJson;
    private TypeOfOperator operatorType;
    private String value;
    private TypeOfValue valueType;

    public OperatorExpression() {

    }

    public OperatorExpression(String[] pathInJson, TypeOfOperator operatorType, String value, TypeOfValue valueType) {
        super();
        this.pathInJson = pathInJson.clone();
        this.operatorType = operatorType;
        this.value = value;
        this.valueType = valueType;
    }

    public String[] getPathInJson() {
        return pathInJson.clone();
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
}
