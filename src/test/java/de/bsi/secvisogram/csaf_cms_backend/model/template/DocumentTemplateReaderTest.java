package de.bsi.secvisogram.csaf_cms_backend.model.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings(value = "CLI_CONSTANT_LIST_INDEX", justification = "for test it is ok")
public class DocumentTemplateReaderTest {

    @Test
    public void json2TemplateDescriptions() throws JsonProcessingException {

        var templatesString = """
                [{ "id": "T1",
                   "description":"Test Template 1",
                   "file":"C:/Project/file1.json"
                 },
                 { "id": "T2",
                   "description":"Test Template 2",
                   "file":"C:/Project/file2.json"
                 }
                ]""";

        DocumentTemplateDescription[] templates = DocumentTemplateReader.json2TemplateDescriptions(templatesString);
        assertThat(templates.length, equalTo(2));
        assertThat(templates[0].getId(), equalTo("T1"));
        assertThat(templates[0].getDescription(), equalTo("Test Template 1"));
        assertThat(templates[0].getFile(), equalTo("C:/Project/file1.json"));
        assertThat(templates[1].getId(), equalTo("T2"));
        assertThat(templates[1].getDescription(), equalTo("Test Template 2"));
        assertThat(templates[1].getFile(), equalTo("C:/Project/file2.json"));
    }

}
