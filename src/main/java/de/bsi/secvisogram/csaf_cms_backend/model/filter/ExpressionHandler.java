package de.bsi.secvisogram.csaf_cms_backend.model.filter;

public interface ExpressionHandler<TResult> {

    TResult and(AndExpression expr);
    TResult or(OrExpression expr);
    TResult operator(OperatorExpression expr);
}
