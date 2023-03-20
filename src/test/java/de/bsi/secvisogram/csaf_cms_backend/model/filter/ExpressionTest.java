package de.bsi.secvisogram.csaf_cms_backend.model.filter;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.ArrayMatching.arrayContaining;

import org.hamcrest.CoreMatchers;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisorySearchUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExpressionTest {

    @Test
    public void expressionToJson() throws JsonProcessingException {

        OperatorExpression opExpr = new OperatorExpression(new String[] {"document"}, TypeOfOperator.Equal, "123.45", TypeOfValue.Decimal);
        AndExpression andExpr = new AndExpression(opExpr);

        String expressionString = AdvisorySearchUtil.expression2Json(andExpr);
        assertThat(expressionString, equalToCompressingWhiteSpace("{" +
                "  \"type\" : \"AND\"," +
                "  \"expressions\" : [ {" +
                "    \"type\" : \"Operator\"," +
                "    \"selector\" : [ \"document\" ]," +
                "    \"operatorType\" : \"Equal\"," +
                "    \"value\" : \"123.45\"," +
                "    \"valueType\" : \"Decimal\"" +
                "  } ]\n" +
                "}"));
    }

    @Test
    public void json2Expression() throws JsonProcessingException {

        String expressionString = "{" +
                "  \"type\" : \"AND\"," +
                "  \"expressions\" : [ {" +
                "    \"type\" : \"Operator\"," +
                "    \"selector\" : [ \"document\", \"version\" ]," +
                "    \"operatorType\" : \"Equal\"," +
                "    \"value\" : \"123.45\"," +
                "    \"valueType\" : \"Decimal\"" +
                "  } ]\n" +
                "}";

        Expression expression = AdvisorySearchUtil.json2Expression(expressionString);

        assertThat(expression, CoreMatchers.instanceOf(AndExpression.class));
        assertThat(((AndExpression) expression).getExpressions().size(), CoreMatchers.equalTo(1));
        Expression expr2 = ((AndExpression) expression).getExpressions().get(0);
        assertThat(expr2, CoreMatchers.instanceOf(OperatorExpression.class));
        OperatorExpression operatorExpr = (OperatorExpression) expr2;
        assertThat(operatorExpr.getOperatorType(), CoreMatchers.equalTo(TypeOfOperator.Equal));
        assertThat(operatorExpr.getValue(), CoreMatchers.equalTo("123.45"));
        assertThat(operatorExpr.getValueType(), CoreMatchers.equalTo(TypeOfValue.Decimal));
        assertThat(operatorExpr.getSelector(),  	arrayContaining("document", "version"));
    }

    @Test
    public void json2Expression_wrongAndExpression() throws JsonProcessingException {

        String expressionString = "{" +
                "  \"type\" : \"AND\"," +
                "  \"expressi\" : [ {" +
                "    \"type\" : \"Operator\"," +
                "    \"pathInJson\" : [ \"document\", \"version\" ]," +
                "    \"operatorType\" : \"Equal\"," +
                "    \"value\" : \"123.45\"," +
                "    \"valueType\" : \"Decimal\"" +
                "  } ]\n" +
                "}";

        Throwable thrown = Assertions.assertThrows(JsonProcessingException.class,
                () ->  AdvisorySearchUtil.json2Expression(expressionString));
        assertThat(thrown.getMessage(), CoreMatchers.startsWith("Unrecognized field \"expressi\""));
    }
}
