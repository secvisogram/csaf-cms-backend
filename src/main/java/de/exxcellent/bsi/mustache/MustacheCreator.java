package de.exxcellent.bsi.mustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import de.exxcellent.bsi.json.Csaf2MapReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Create Html from a mustache template file and Json Input File
 */
public class MustacheCreator {

    public String createHtml(final String templateName, final Reader jsonReader) throws IOException {

        // read templates from resources template dir
        final Mustache.Compiler compiler = Mustache.compiler().withLoader(name -> {
            InputStream inputStream = MustacheCreator.class.getResourceAsStream("/templates/" + name);
            if(inputStream != null) {
                return new InputStreamReader(inputStream);
            } else {
                throw new RuntimeException("Invalid Template: " + name);
            }
        });

        try (InputStream templateStream = MustacheCreator.class.getResourceAsStream("/templates/" + templateName)) {

            if(templateStream == null) {
                throw new RuntimeException("Invalid Template: " + templateName);
            }
            Template htmlTemplate = compiler.compile(new InputStreamReader(templateStream));
            Object result = new Csaf2MapReader().readCasfDocument(jsonReader);
            return htmlTemplate.execute(result);
        }
    }


    public static void main(String[] args)  {

        final String jsonFileName =   "exxcellent-2021AB123.json";
        try(InputStream csafJsonStream = Csaf2MapReader.class.getResourceAsStream(jsonFileName)) {
            if(csafJsonStream != null) {
                System.out.println(new MustacheCreator().createHtml("index.mustache", new InputStreamReader(csafJsonStream)));
            } else {
                System.out.println("Invalid Json File: "+ jsonFileName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
