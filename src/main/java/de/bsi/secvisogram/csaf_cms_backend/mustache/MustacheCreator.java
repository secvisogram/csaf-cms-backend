package de.bsi.secvisogram.csaf_cms_backend.mustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import de.bsi.secvisogram.csaf_cms_backend.json.Csaf2MapReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Create Html from a mustache template file and Json Input File
 */
public class MustacheCreator {

    private static final Logger LOG = LoggerFactory.getLogger(MustacheCreator.class);

    public String createHtml(final String templateName, final Reader jsonReader) throws IOException {

        // read templates from resources template dir
        final Mustache.Compiler compiler = Mustache.compiler().withLoader(name -> {
            InputStream inputStream = MustacheCreator.class.getResourceAsStream("/templates/" + name);
            if(inputStream != null) {
                return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            } else {
                throw new RuntimeException("Invalid Template: " + name);
            }
        });

        try (InputStream templateStream = MustacheCreator.class.getResourceAsStream("/templates/" + templateName)) {

            if(templateStream == null) {
                throw new RuntimeException("Invalid Template: " + templateName);
            }
            Template htmlTemplate = compiler.compile(new InputStreamReader(templateStream, StandardCharsets.UTF_8));
            Object result = new Csaf2MapReader().readCasfDocument(jsonReader);
            return htmlTemplate.execute(result);
        }
    }


    public static void main(String[] args)  {

        final String jsonFileName =   "exxcellent-2021AB123.json";
        try(InputStream csafJsonStream = Csaf2MapReader.class.getResourceAsStream(jsonFileName)) {
            if(csafJsonStream != null) {
                System.out.println(new MustacheCreator().createHtml("index.mustache"
                        , new InputStreamReader(csafJsonStream, StandardCharsets.UTF_8)));
            } else {
                System.out.println("Invalid Json File: "+ jsonFileName);
            }
        } catch (Exception ex) {
            LOG.error("Error loading Json", ex);
        }
    }
}
