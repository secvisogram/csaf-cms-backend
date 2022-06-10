package de.bsi.secvisogram.csaf_cms_backend.model.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converts the content of the file that contains the description of all document templates
 * to a list of document descriptions
 */
public class DocumentTemplateReader {

    /**
     * Converts the content of the jsonString to a List of descriptions
     * @param jsonString the Json String
     * @return all descriptions
     * @throws JsonProcessingException error processing the JSON String
     */
    public static DocumentTemplateDescription[] json2TemplateDescriptions(String jsonString) throws JsonProcessingException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        return jacksonMapper.readValue(jsonString, DocumentTemplateDescription[].class);
    }
}
