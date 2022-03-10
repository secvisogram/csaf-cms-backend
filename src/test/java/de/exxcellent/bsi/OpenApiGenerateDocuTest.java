package de.exxcellent.bsi;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.OpenAPIGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to generate swagger documentation.
 * No really application test. This class uses the spring boot Testframework to generate documentation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OpenApiGenerateDocuTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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
