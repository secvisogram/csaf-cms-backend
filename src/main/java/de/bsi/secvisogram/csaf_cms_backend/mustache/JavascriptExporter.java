package de.bsi.secvisogram.csaf_cms_backend.mustache;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.annotation.Nonnull;

/**
 * Create Html String from a mustache template file and Json Input File
 */
public class JavascriptExporter {


    public String createHtml(String jsonString) throws IOException {

        // DocumentEntity.mjs is imported from Script.mjs
        // It has to bbe available in the Context WorkingDirectory
        final String documentEntityScript = "DocumentEntity.mjs";
        final Path tempDir = Files.createTempDirectory("mustache");
        final Path tempDocFile = tempDir.resolve(documentEntityScript);

        try (InputStream in = JavascriptExporter.class.getResourceAsStream(documentEntityScript)) {
            Files.write(tempDocFile, in.readAllBytes());
        }
        var jsContext = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .allowIO(true)
                .option("js.esm-eval-returns-exports", "true")
                .currentWorkingDirectory(tempDir)
                .build();

        try (InputStream templateResource = JavascriptExporter.class.getResourceAsStream("Template.html");
             InputStream mustacheResource = JavascriptExporter.class.getResourceAsStream("mustache.min.js");
             InputStream scriptResource = JavascriptExporter.class.getResourceAsStream("Script.mjs")) {
            final var template = new String(templateResource.readAllBytes(), StandardCharsets.UTF_8);
            final var mustacheSource = Source.newBuilder("js", this.createResourceReader(mustacheResource), "mustache.js")
                    .build();
            jsContext.eval(mustacheSource);
            final var scriptSource = Source.newBuilder("js", createResourceReader(scriptResource), "Script.mjs")
                    .mimeType("application/javascript+module")
                    .build();
            final Value scriptResult = jsContext.eval(scriptSource);
            final Value renderFunction = scriptResult.getMember("renderWithMustache");
            final Object result = renderFunction.execute(template, jsonString);
            return result.toString();
        }
    }

    private Reader createResourceReader(@Nonnull final InputStream input) {
        return new InputStreamReader(input, StandardCharsets.UTF_8);
    }
}
