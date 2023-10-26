package de.bsi.secvisogram.csaf_cms_backend.mustache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.apache.commons.codec.binary.Base64;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * Create Html String from a mustache template file and Json Input File
 */
@Service
public class JavascriptExporter {

    private static final Logger LOG = LoggerFactory.getLogger(JavascriptExporter.class);

    @org.springframework.beans.factory.annotation.Value("${csaf.document.templates.companyLogoPath}")
    private String companyLogoPath;

    /**
     * Create an HTML export from the provided advisory document (as JSON)
     *
     * @param advisoryJson the advisory that should be exported (in JSON format)
     * @return the HTML document as a String
     * @throws IOException on any error regarding disk write/read
     */
    public String createHtml(@Nonnull final String advisoryJson) throws IOException {

        // DocumentEntity.mjs is imported from Script.mjs
        // It has to bbe available in the Context WorkingDirectory
        final String documentEntityScript = "DocumentEntity.mjs";
        final Path tempDir = Files.createTempDirectory("mustache");
        final Path tempDocFile = tempDir.resolve(documentEntityScript);

        try (final InputStream in = JavascriptExporter.class.getResourceAsStream(documentEntityScript)) {
            Files.write(tempDocFile, in.readAllBytes());
        }
        final var jsContext = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .allowIO(IOAccess.ALL)
                .option("js.esm-eval-returns-exports", "true")
                .currentWorkingDirectory(tempDir)
                .build();

        try (final InputStream templateResource = JavascriptExporter.class.getResourceAsStream("Template.html");
             final InputStream mustacheResource = JavascriptExporter.class.getResourceAsStream("mustache.min.js");
             final InputStream scriptResource = JavascriptExporter.class.getResourceAsStream("Script.mjs")) {
            final var template = new String(templateResource.readAllBytes(), StandardCharsets.UTF_8);
            final var mustacheSource = Source.newBuilder("js", this.createResourceReader(mustacheResource), "mustache.js")
                    .build();
            jsContext.eval(mustacheSource);
            final var scriptSource = Source.newBuilder("js", createResourceReader(scriptResource), "Script.mjs")
                    .mimeType("application/javascript+module")
                    .build();
            final Value scriptResult = jsContext.eval(scriptSource);
            final Value renderFunction = scriptResult.getMember("renderWithMustache");
            final Object result = renderFunction.execute(template, advisoryJson, createLogoJson());
            return result.toString();
        }
    }

    private Reader createResourceReader(@Nonnull final InputStream input) {
        return new InputStreamReader(input, StandardCharsets.UTF_8);
    }

    private String createLogoJson() throws IOException {
        if (this.companyLogoPath == null || "".equals(this.companyLogoPath)) {
            LOG.info("The company logo path was not set, export result will not contain a logo.");
            return null;
        }
        final Path logoPath = Path.of(this.companyLogoPath);
        final MediaType logoMediaType = determineMediaTypeOfLogo(logoPath);
        final byte[] encoded = Base64.encodeBase64(Files.readAllBytes(logoPath));
        final String data = new String(encoded, StandardCharsets.US_ASCII);
        final ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("mediaType", logoMediaType.toString());
        node.put("data", data);
        return node.toString();
    }

    static MediaType determineMediaTypeOfLogo(@Nonnull final Path path) {
        final Path filename = path.getFileName();
        if (filename != null) {
            final String fileName = filename.toString().toLowerCase();
            if (fileName.endsWith(".png")) {
                return MediaType.IMAGE_PNG;
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return MediaType.IMAGE_JPEG;
            }
            throw new IllegalArgumentException("Unknown company logo format: " + fileName);
        }
        throw new IllegalArgumentException("Got empty path");
    }
}
