package de.bsi.secvisogram.csaf_cms_backend.model.template;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converts the content of the file that contains the description of all document templates
 * to a list of document descriptions
 */
public class DocumentTemplateReader {

    /**
     * Converts the content of the jsonString to a List of descriptions
     * @param jsonString the Json String
     * @return all descriptions
     * @throws JacksonException error processing the JSON String
     */
    public static DocumentTemplateDescription[] json2TemplateDescriptions(String jsonString) throws JacksonException {

        final ObjectMapper jacksonMapper = new JsonMapper();
        return jacksonMapper.readValue(jsonString, DocumentTemplateDescription[].class);
    }
}
