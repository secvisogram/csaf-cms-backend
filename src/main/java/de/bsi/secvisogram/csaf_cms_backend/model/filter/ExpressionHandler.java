package de.bsi.secvisogram.csaf_cms_backend.model.filter;

public interface ExpressionHandler<TResult> {

    public TResult and(AndExpression expr);
    public TResult operator(OperatorExpression expr);
}
