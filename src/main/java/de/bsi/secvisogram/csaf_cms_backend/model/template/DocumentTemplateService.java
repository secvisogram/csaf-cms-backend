package de.bsi.secvisogram.csaf_cms_backend.model.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.security.RolesAllowed;
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
    @RolesAllowed({CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR})
    public DocumentTemplateDescription[] getAllTemplates() throws IOException {

        Path templateFilePath = Path.of(templatesFile);
        String templatesJson = Files.readString(templateFilePath);

        return DocumentTemplateReader.json2TemplateDescriptions(templatesJson);
    }


    /**
     * Get template filename of the template with the given Id
     * @param templateId the id of the template to get the filename of
     * @return the template file name
     */
    @RolesAllowed({CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUDITOR})
    public Optional<String> getTemplateFileName(String templateId) throws IOException {

        return Arrays.stream(this.getAllTemplates())
                .filter(template -> template.getId().equals(templateId))
                .map(DocumentTemplateDescription::getFile)
                .findFirst();
    }

    /**
     * Reads the template with given ID
     * @param templateId the ID of the template to read
     * @return the template as JSON node
     * @throws IOException when there are errors reading the file
     */
    public Optional<JsonNode> getTemplate(String templateId) throws IOException {

        Optional<String> relativeFileName = getTemplateFileName(templateId);
        if (relativeFileName.isPresent()) {
            Path parentPath = Path.of(templatesFile).getParent();
            if (parentPath == null) {
                throw new IOException("Could not find directory containing templates!");
            }
            Path templatePath = parentPath.resolve(relativeFileName.get());
            final ObjectMapper jacksonMapper = new ObjectMapper();
            return Optional.of(jacksonMapper.readValue(Files.readAllBytes(templatePath), JsonNode.class));
        }
        return Optional.empty();
    }

}
