package de.bsi.secvisogram.csaf_cms_backend;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.OpenAPIGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to generate swagger documentation.
 * No really application test. This class uses the spring boot Testframework to generate documentation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OpenApiGenerateDocuTest {

    @LocalServerPort
    private int port;

    /**
     * Start Rest Server and generate documentation from OpenApi-Json
     */
    @Test
    public void generateSwaggerDocu()  {


        final String url = "http://localhost:" + port + "/v3/api-docs/";

        OpenAPIGenerator.main(Arrays.array("generate", "-i", url, "-g", "html", "-o", "documents/generated"));
        OpenAPIGenerator.main(Arrays.array("generate", "-i", url, "-g", "asciidoc", "-o", "documents/generated"));
   }

}
