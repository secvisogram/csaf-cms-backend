package de.bsi.secvisogram.csaf_cms_backend.mustache;

import static de.bsi.secvisogram.csaf_cms_backend.mustache.JavascriptExporter.determineMediaTypeOfLogo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(properties = "csaf.document.templates.companyLogoPath=./src/test/resources/eXXcellent_solutions.png")
@ExtendWith(SpringExtension.class)
class JavascriptExporterTest {

    @Autowired
    private JavascriptExporter javascriptExporter;


    @Test
    void createHtml() throws IOException {

        String html = this.javascriptExporter.createHtml(JavascriptExporterTestFixtures.json);
        assertThat(html, equalToIgnoringWhiteSpace(JavascriptExporterTestFixtures.resultHtmlWithLogo));

    }

    @Test
    void determineMediaTypeOfLogoTest() {

        assertThat(determineMediaTypeOfLogo(Path.of("test.png")), is(equalTo(MediaType.IMAGE_PNG)));
        assertThat(determineMediaTypeOfLogo(Path.of("test.jpg")), is(equalTo(MediaType.IMAGE_JPEG)));
        assertThat(determineMediaTypeOfLogo(Path.of("test.jpeg")), is(equalTo(MediaType.IMAGE_JPEG)));
        assertThrows(IllegalArgumentException.class, () -> determineMediaTypeOfLogo(Path.of("test.txt")));
    }
}