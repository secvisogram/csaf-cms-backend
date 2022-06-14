package de.bsi.secvisogram.csaf_cms_backend.mustache;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.script.ScriptException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class JavascriptExporter {


    public String createHtml(String jsonString) throws IOException {

        var ctx = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .allowIO(true)
                .option("js.esm-eval-returns-exports", "true")
                .currentWorkingDirectory(Path.of("C:/Project/exxcellent/bsi/csaf-cms-backend/src/main/resources/de/bsi/secvisogram/csaf_cms_backend/mustache"))
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

    private static final String json = """
                    {
                      "document": {
                        "category": "generic_csaf",
                        "csaf_version": "2.0",
                        "publisher": {
                          "category": "coordinator",
                          "name": "exccellent",
                          "namespace": "https://exccellent.de"
                        },
                        "title": "TestRSc",
                        "tracking": {
                          "current_release_date": "2022-01-11T11:00:00.000Z",
                          "id": "exxcellent-2021AB123",
                          "initial_release_date": "2022-01-12T11:00:00.000Z",
                          "revision_history": [
                            {
                              "date": "2022-01-12T11:00:00.000Z",
                              "number": "0.0.1",
                              "summary": "Test rsvSummary"
                            }
                          ],
                          "status": "draft",
                          "version": "0.0.1",
                          "generator": {
                            "date": "2022-01-11T04:07:27.246Z",
                            "engine": {
                              "version": "1.10.0",
                              "name": "Secvisogram"
                            }
                          }
                        },
                        "acknowledgments": [
                          {
                            "names": [
                              "Rainer",
                              "Gregor",
                              "Timo"
                            ],
                            "organization": "exxcellent contribute",
                            "summary": "Summary 1234",
                            "urls": [
                              "https://exccellent.de",
                              "https:/heise.de"
                            ]
                          }
                        ]
                      }
                    }
                    """;

    public static void main(String[] args) throws IOException, ScriptException, NoSuchMethodException {

        var ctx = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .allowIO(true)
                .option("js.esm-eval-returns-exports", "true")
                .currentWorkingDirectory(Path.of("C:/Project/exxcellent/bsi/csaf-cms-backend/src/main/resources/de/bsi/secvisogram/csaf_cms_backend/mustache"))
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

            Object result = member.execute(template, json);
            System.out.println(result);

        }
    }
}
