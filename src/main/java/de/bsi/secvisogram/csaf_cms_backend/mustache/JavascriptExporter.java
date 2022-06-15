package de.bsi.secvisogram.csaf_cms_backend.mustache;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * Create Html String from a mustache template file and Json Input File
 */
public class JavascriptExporter {


    public String createHtml(String jsonString) throws IOException {

        // DocumentEntity.mjs is imported from Script.mjs
        // It has to bbe available in the Context WorkingDirectory
        final String documentEntityScript = "DocumentEntity.mjs";
        Path tempDir = Files.createTempDirectory("mustache");
        Path tempDocFile = tempDir.resolve(documentEntityScript);

        try (InputStream in = JavascriptExporter.class.getResourceAsStream(documentEntityScript)) {
            Files.write(tempDocFile, in.readAllBytes());
        }
        var ctx = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .allowIO(true)
                .option("js.esm-eval-returns-exports", "true")
                .currentWorkingDirectory(tempDir)
                .build();

        String template = "";
        try (InputStream in = JavascriptExporter.class.getResourceAsStream("Template.html")) {
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (InputStream in = JavascriptExporter.class.getResourceAsStream("Script.mjs");
             InputStream mustache = JavascriptExporter.class.getResourceAsStream("mustache.min.js")) {

            ctx.eval(Source.newBuilder("js", new InputStreamReader(mustache, StandardCharsets.UTF_8), "mustache.js").build());
            Value scipts = ctx.eval(Source.newBuilder("js", new InputStreamReader(in, StandardCharsets.UTF_8), "Script.mjs").mimeType("application/javascript+module").build());

            Value member = scipts.getMember("renderWithMustache");

            Object result = member.execute(template, jsonString);
            return result.toString();
        }
    }
}
