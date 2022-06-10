package de.bsi.secvisogram.csaf_cms_backend.model.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Description of a  prefilled CSAF documents as templates for a new CSAF documents
 */
public class DocumentTemplateDescription {

    private String id;
    private String description;

    private String file;

    public DocumentTemplateDescription() {
    }

    public DocumentTemplateDescription(String id, String description, String file) {
        this.id = id;
        this.description = description;
        this.file = file;
    }

    /**
     * The unique id of the template
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * The description of the template displayed in the client
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * The path to the file with the content of the CSAF document
     * @return the file
     */
    public String getFile() {
        return file;
    }

    /**
     * Read the file and return its content as JsonNode
     * @return the content of the file
     * @throws IOException error reading the file
     */
    public JsonNode getFileAsJsonNode() throws IOException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        return jacksonMapper.readValue(Files.readAllBytes(Path.of(this.file)), JsonNode.class);
    }
}
