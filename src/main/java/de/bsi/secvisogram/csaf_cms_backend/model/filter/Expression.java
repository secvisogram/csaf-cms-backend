package de.bsi.secvisogram.csaf_cms_backend.model.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AndExpression.class, name = "AND"),
        @JsonSubTypes.Type(value = OperatorExpression.class, name = "Operator")
})
public interface Expression {

    <TResult> TResult handleExpr(ExpressionHandler<TResult> handler);
}
