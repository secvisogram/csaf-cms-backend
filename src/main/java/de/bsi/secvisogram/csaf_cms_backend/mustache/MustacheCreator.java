package de.bsi.secvisogram.csaf_cms_backend.mustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create Html from a mustache template file and Json Input File
 */
public class MustacheCreator {

    private static final Logger LOG = LoggerFactory.getLogger(MustacheCreator.class);

    public String createHtml(final String templateName, final Map<String, Object> objectToConvert) throws IOException {

        // read templates from resources template dir
        final Mustache.Compiler compiler = Mustache.compiler().defaultValue("").withLoader(name -> {
            InputStream inputStream = MustacheCreator.class.getResourceAsStream("/templates/" + name);
            if (inputStream != null) {
                return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            } else {
                throw new RuntimeException("Invalid Template: " + name);
            }
        });


        try (InputStream templateStream = MustacheCreator.class.getResourceAsStream("/templates/" + templateName)) {

            if (templateStream == null) {
                throw new RuntimeException("Invalid Template: " + templateName);
            }
            Template htmlTemplate = compiler.compile(new InputStreamReader(templateStream, StandardCharsets.UTF_8));
            return htmlTemplate.execute(objectToConvert);
        }
    }
}
