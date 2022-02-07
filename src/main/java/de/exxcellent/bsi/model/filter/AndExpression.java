package de.exxcellent.bsi.model.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Expression which concatenates all added expressions with the And Operator
 */
public class AndExpression {

    private final List<FilterExpression> expressions;

    public AndExpression() {

        this.expressions = new ArrayList<>();
    }

    public AndExpression(FilterExpression... operatorExpressions) {
        this.expressions = new ArrayList<>();
        Collections.addAll(this.expressions, operatorExpressions);
    }

    public AndExpression(List<FilterExpression> operatorExpressions) {
        this.expressions = new ArrayList<>();
        this.expressions.addAll(operatorExpressions);
    }

    /**
     * Get all expression the of this and expresion
     */
    public List<FilterExpression> getExpressions() {

        return Collections.unmodifiableList(this.expressions);
    }

}
