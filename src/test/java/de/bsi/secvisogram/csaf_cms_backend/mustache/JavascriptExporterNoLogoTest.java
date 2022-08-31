package de.bsi.secvisogram.csaf_cms_backend.mustache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(properties = "csaf.document.templates.companyLogoPath=")
@ExtendWith(SpringExtension.class)
class JavascriptExporterNoLogoTest {

    @Autowired
    private JavascriptExporter javascriptExporter;


    @Test
    void createHtml() throws IOException {

        String html = this.javascriptExporter.createHtml(JavascriptExporterTestFixtures.json);
        assertThat(html, equalToIgnoringWhiteSpace(JavascriptExporterTestFixtures.resultHtmlNoLogo));

    }

}