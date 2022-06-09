package de.bsi.secvisogram.csaf_cms_backend.model.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to load all templates or the content of a specific template
 */
@Service
public class DocumentTemplateService {

    @Value("${csaf.document.templates.file}")
    private String templatesFile;


    /**
     * Read all templates from the template file
     * @return all templates
     * @throws IOException error reading the files
     */
    public DocumentTemplateDescription[] getAllTemplates() throws IOException {

        Path templateFilePath = Path.of(templatesFile);
        String templatesJson = Files.readString(templateFilePath);

        return DocumentTemplateReader.json2TemplateDescriptions(templatesJson);
    }

    /**
     * Read template with the given Id
     * @param templateId the id of the template to read
     * @return the template description
     * @throws IOException error reading the template
     */
    public Optional<DocumentTemplateDescription> getTemplateForId(String templateId) throws IOException {

        return Arrays.stream(this.getAllTemplates())
                .filter(template -> template.getId().equals(templateId))
                .findFirst();
    }

}
