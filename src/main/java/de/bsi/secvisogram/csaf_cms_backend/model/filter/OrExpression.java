package de.bsi.secvisogram.csaf_cms_backend.model.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Expression which concatenates all added expressions with the Or Operator
 */
public class OrExpression implements Expression {

    private final List<Expression> expressions;

    public OrExpression() {

        this.expressions = new ArrayList<>();

    }

    public OrExpression(Expression... expressions) {
        this.expressions = new ArrayList<>();
        Collections.addAll(this.expressions, expressions);
    }

    /**
     * Get all expressions of this or expression
     */
    public List<Expression> getExpressions() {

        return Collections.unmodifiableList(this.expressions);
    }

    public <TResult> TResult handleExpr(ExpressionHandler<TResult> handler) {

        return handler.or(this);
    }

}
