package de.bsi.secvisogram.csaf_cms_backend.model.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Expression which concatenates all added expressions with the And Operator
 */
public class AndExpression implements Expression {

    private final List<Expression> expressions;

    public AndExpression() {

        this.expressions = new ArrayList<>();

    }

    public AndExpression(Expression... expressions) {
        this.expressions = new ArrayList<>();
        Collections.addAll(this.expressions, expressions);
    }

    public AndExpression(List<Expression> expressions) {
        this.expressions = new ArrayList<>();
        this.expressions.addAll(expressions);
    }

    /**
     * Get all expression the of this and expresion
     */
    public List<Expression> getExpressions() {

        return Collections.unmodifiableList(this.expressions);
    }

    public <TResult> TResult handleExpr(ExpressionHandler<TResult> handler) {

        return handler.and(this);
    }

}
