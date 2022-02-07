package de.exxcellent.bsi.model.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Expression which concatenates all added expressions with the Or Operator
 */
public class OrExpression {

    private final List<FilterExpression> expressions;

    public OrExpression() {

        this.expressions = new ArrayList<>();
    }

    public OrExpression(FilterExpression... operatorExpressions) {
        super();
        this.expressions = new ArrayList<>();
        Collections.addAll(this.expressions, operatorExpressions);
    }

    /**
     * Get all expression the of this and expresion
     */
    public List<FilterExpression> getExpressions() {

        return Collections.unmodifiableList(this.expressions);
    }

}
