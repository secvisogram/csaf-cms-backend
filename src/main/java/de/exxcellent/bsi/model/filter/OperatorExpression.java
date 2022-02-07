package de.exxcellent.bsi.model.filter;

/**
 * Expression which compares a Property with a Value by a defined Operator
 */
public class OperatorExpression {

    private final String[] pathInJson;
    private final TypeOfOperator operatorType;
    private final String value;
    private final TypeOfValue valueType;

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
