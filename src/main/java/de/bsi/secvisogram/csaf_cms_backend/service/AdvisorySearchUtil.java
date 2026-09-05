package de.bsi.secvisogram.csaf_cms_backend.service;

import de.bsi.secvisogram.csaf_cms_backend.model.filter.Expression;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Utility for converting filter expressions to/from JSON.
 */
public class AdvisorySearchUtil {

    /**
     * Convert Search Expression to JSON String
     *
     * @param expression2Convert the expression to convert
     * @return the converted expression
     * @throws JacksonException a conversion problem has occurred
     */
    public static String expression2Json(Expression expression2Convert) throws JacksonException {

        final ObjectMapper jacksonMapper = new JsonMapper();
        ObjectWriter writer = jacksonMapper.writerWithDefaultPrettyPrinter();

        return writer.writeValueAsString(expression2Convert);
    }

    /**
     * Convert JSON String to Search expression
     *
     * @param jsonString the String to convert
     * @return the converted expression
     * @throws JacksonException error in json
     */
    public static Expression json2Expression(String jsonString) throws JacksonException {

        final ObjectMapper jacksonMapper = new JsonMapper();
        return jacksonMapper.readValue(jsonString, Expression.class);

    }
}
