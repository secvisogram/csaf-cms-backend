package de.bsi.secvisogram.csaf_cms_backend.mustache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Csaf2MapReaderTest {

    @Test
    void readCsafDocument() throws IOException {

        final String jsonFileName = "/examples/bsi-2022-0001.json";
        try (InputStream csafJsonStream = Csaf2MapReaderTest.class.getResourceAsStream(jsonFileName)) {


             Object result = new Csaf2MapReader()
                        .readCsafDocument(new InputStreamReader(csafJsonStream, StandardCharsets.UTF_8));

            Map<String, Object> resultMap = (Map<String, Object>) result;
            Map<String, Object> documentMap = (Map<String, Object>) resultMap.get("document");


            assertThat(documentMap.get("title"), equalTo("CVRF-CSAF-Converter: XML External Entities Vulnerability"));
        }
    }
}