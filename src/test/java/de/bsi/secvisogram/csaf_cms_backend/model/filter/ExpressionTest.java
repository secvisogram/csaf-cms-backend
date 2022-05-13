package de.bsi.secvisogram.csaf_cms_backend.model.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.ArrayMatching.arrayContaining;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryJsonService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExpressionTest {

    @Test
    public void expressionToJson() throws JsonProcessingException {

        OperatorExpression opExpr = new OperatorExpression(new String[] {"document"}, TypeOfOperator.Equal, "123.45", TypeOfValue.Decimal);
        AndExpression andExpr = new AndExpression(opExpr);

        String expressionString = new AdvisoryJsonService().expression2Json(andExpr);

        assertThat(expressionString, equalToIgnoringWhiteSpace("{" +
                "  \"type\" : \"AND\"," +
                "  \"expressions\" : [ {" +
                "    \"type\" : \"Operator\"," +
                "    \"pathInJson\" : [ \"document\" ]," +
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
                "    \"pathInJson\" : [ \"document\", \"version\" ]," +
                "    \"operatorType\" : \"Equal\"," +
                "    \"value\" : \"123.45\"," +
                "    \"valueType\" : \"Decimal\"" +
                "  } ]\n" +
                "}";

        Expression expression = new AdvisoryJsonService().json2Expression(expressionString);

        assertThat(expression, instanceOf(AndExpression.class));
        assertThat(((AndExpression) expression).getExpressions().size(), equalTo(1));
        Expression expr2 = ((AndExpression) expression).getExpressions().get(0);
        assertThat(expr2, instanceOf(OperatorExpression.class));
        OperatorExpression operatorExpr = (OperatorExpression) expr2;
        assertThat(operatorExpr.getOperatorType(), equalTo(TypeOfOperator.Equal));
        assertThat(operatorExpr.getValue(), equalTo("123.45"));
        assertThat(operatorExpr.getValueType(), equalTo(TypeOfValue.Decimal));
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
                () ->  new AdvisoryJsonService().json2Expression(expressionString));
        assertThat(thrown.getMessage(), startsWith("Unrecognized field \"expressi\""));
    }
}
