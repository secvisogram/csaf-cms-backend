package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.TYPE_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.equal;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.json.ObjectType;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.AndExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.Expression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import jakarta.annotation.Nullable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

public class AdvisorySearchUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisorySearchUtil.class);
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


    /**
     * Build a CouchDB Mango selector for advisories of the given type that match the optional
     * search expression AND an optional visibility constraint expression.
     *
     * @param expression      optional search expression (JSON-encoded); {@code null} or blank means
     *                        no content filter
     * @param objectType      the advisory type to filter by (Advisory or AdvisoryVersion)
     * @param visibilityExpr  optional additional visibility filter; {@code null} means no extra
     *                        constraint (caller can see everything)
     * @return a ready-to-use Mango selector map
     * @throws CsafException if the expression string cannot be parsed
     */
    public static Map<String, Object> buildAdvisoryExpression(String expression, ObjectType objectType,
                                                              @Nullable Expression visibilityExpr)
            throws CsafException {

        try {
            final Map<String, Object> resulSelector;
            OperatorExpression typeExpr = equal(objectType.name(), TYPE_FIELD.getDbName());

            if (expression != null && !expression.isBlank()) {
                Expression searchExpression = json2Expression(expression);

                if (visibilityExpr != null) {
                    AndExpression wholeExpr = new AndExpression(typeExpr, searchExpression, visibilityExpr);
                    resulSelector = expr2CouchDBFilter(wholeExpr,
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
                    AndExpression wholeExpr = new AndExpression(typeExpr, searchExpression);
                    resulSelector = expr2CouchDBFilter(wholeExpr,
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
                }
            } else {
                if (visibilityExpr != null) {
                    resulSelector = expr2CouchDBFilter(new AndExpression(typeExpr, visibilityExpr));
                } else {
                    resulSelector = expr2CouchDBFilter(typeExpr);
                }
            }
            return resulSelector;
        } catch (JacksonException ex) {
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
