package de.bsi.secvisogram.csaf_cms_backend.model.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(properties = "csaf.document.templates.file=./src/test/resources/de/bsi/secvisogram/csaf_cms_backend/couchdb/model/template/allTemplates.json")
@ExtendWith(SpringExtension.class)
@SuppressFBWarnings(value = "CLI_CONSTANT_LIST_INDEX", justification = "for test it is ok")
class DocumentTemplateServiceTest {

    @Autowired
    private DocumentTemplateService templateService;

    @Test
    void getAllTemplatesTest() throws IOException {

        DocumentTemplateDescription[] allTemplates = this.templateService.getAllTemplates();
        assertThat(allTemplates.length, equalTo(2));
        assertThat(allTemplates[0].getId(), equalTo("T1"));
        assertThat(allTemplates[0].getDescription(), equalTo("Test Template 1"));
        assertThat(allTemplates[0].getFile(), equalTo("./src/test/resources/de/bsi/secvisogram/csaf_cms_backend/couchdb/model/template/template1.json"));
        assertThat(allTemplates[1].getId(), equalTo("T2"));
        assertThat(allTemplates[1].getDescription(), equalTo("Test Template 2"));
        assertThat(allTemplates[1].getFile(), equalTo("./src/test/resources/de/bsi/secvisogram/csaf_cms_backend/couchdb/model/template/template2.json"));
    }

    @Test
    void getTemplatesForIdTest() throws IOException {

        var template1 = this.templateService.getTemplateForId("T1");
        assertThat(template1.get().getDescription(), equalTo("Test Template 1"));

        JsonNode node1 = template1.get().getFileAsJsonNode();
        assertThat(node1.at("/document/title").asText(), equalTo("Test Template 1"));

        var template2 = this.templateService.getTemplateForId("T2");
        assertThat(template2.get().getDescription(), equalTo("Test Template 2"));
        Assertions.assertThrows(NoSuchFileException.class, () -> template2.get().getFileAsJsonNode());

        var template3 = this.templateService.getTemplateForId("T3");
        assertThat(template3.isPresent(), is(false));
    }
}