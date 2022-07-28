package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.TYPE_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.equal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.json.ObjectType;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.AndExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.Expression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class AdvisorySearchUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisorySearchUtil.class);
    private static final OperatorExpression typeExpr = equal(ObjectType.Advisory.name(), TYPE_FIELD.getDbName());
    private static final String[] selectDocAcknowledgments = {"csaf", "document", "acknowledgments"};
    private static final String[] selectDocAcknowledgmentsNames = {"csaf", "document", "acknowledgments", "names"};
    private static final String[] selectDocAcknowledgmentsUrls = {"csaf", "document", "acknowledgments", "urls"};
    private static final String[] selectDocNotes = {"csaf", "document", "notes"};
    private static final String[] selectDocReferences = {"csaf", "document", "references"};
    private static final String[] selectDocTrackingRev = {"csaf", "document", "tracking", "revision_history"};
    private static final String[] selectDocTrackingAliases = {"csaf", "document", "tracking", "aliases"};
    private static final String[] selectVulnerabilities = {"csaf", "vulnerabilities"};
    private static final String[] selectProductTreeFullProductNames = {"csaf", "product_tree", "full_product_names"};
    private static final String[] selectProductTreeBranches = {"csaf", "product_tree", "branches"};


    public static Map<String, Object> buildAdvisoryExpression(String expression, ObjectType objectType) throws CsafException {

        try {
            final Map<String, Object> selector;
            if (expression != null && !expression.isBlank()) {
                Expression searchExpression = json2Expression(expression);


                AndExpression wholeExpr = new AndExpression(typeExpr, searchExpression);
                selector = expr2CouchDBFilter(wholeExpr,
                        selectDocAcknowledgments,
                        selectDocAcknowledgmentsNames,
                        selectDocAcknowledgmentsUrls,
                        selectDocNotes,
                        selectDocReferences,
                        selectDocTrackingRev,
                        selectDocTrackingAliases,
                        selectVulnerabilities,
                        selectProductTreeFullProductNames,
                        selectProductTreeBranches);
            } else {
                selector = expr2CouchDBFilter(equal(objectType.name(), TYPE_FIELD.getDbName()));
            }

            return selector;
        } catch (JsonProcessingException ex) {
            LOG.debug("Invalid expression", ex);
            throw new CsafException("Invalid filter expression", CsafExceptionKey.InvalidFilterExpression,
                    HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * Convert Search Expression to JSON String
     *
     * @param expression2Convert the expression to convert
     * @return the converted expression
     * @throws JsonProcessingException a conversion problem has occurred
     */
    public static String expression2Json(Expression expression2Convert) throws JsonProcessingException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        ObjectWriter writer = jacksonMapper.writer(new DefaultPrettyPrinter());

        return writer.writeValueAsString(expression2Convert);
    }

    /**
     * Convert JSON String to Search expression
     *
     * @param jsonString the String to convert
     * @return the converted expression
     * @throws JsonProcessingException error in json
     */
    public static Expression json2Expression(String jsonString) throws JsonProcessingException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        return jacksonMapper.readValue(jsonString, Expression.class);

    }
}
