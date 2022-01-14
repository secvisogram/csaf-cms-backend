package de.exxcellent.bsi.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.exxcellent.bsi.mustache.MustacheCreator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

/**
 * Converts a CSAF JSON File to HashMap.
 *   JSON-Objects are converted to Hashmap
 *   JSON-Arrays are converted to Arrays
 *   JSON-Properties are converted Map-Entries
 */
public class Csaf2MapReader {

    private final ObjectMapper mapper;

    public Csaf2MapReader() {

        this.mapper = new ObjectMapper();
    }


    public Object readCasfDocument(Reader casfDokumnet) throws IOException {

        JsonNode rootNode = mapper.readValue(casfDokumnet, JsonNode.class);

        return convertToObject(rootNode);
    }

    private Object convertToObject(JsonNode startNode) {

        final Object resultObj;
        if(startNode == null) {
            resultObj = null;
        } else if(startNode.isArray()) {
            List<Object> resultList = new ArrayList<>();
            for (JsonNode curValue : startNode) {
                resultList.add(convertToObject(curValue));
            }
            resultObj = resultList;
        } else if(startNode.isObject()) {
            final Map<String,Object> resultMap = new HashMap<>();
            Iterator<String> names = startNode.fieldNames();
            names.forEachRemaining(name -> resultMap.put(name,convertToObject(startNode.get(name))));
            resultObj =resultMap;
        } else if(startNode.isNumber()) {
            resultObj = startNode.asLong();
        } else if(startNode.isTextual()) {
            resultObj = startNode.asText();
        } else {
            resultObj = null;
        }

        return resultObj;
    }

    public static void main(String[] args)  {

        final String jsonFileName =   "exxcellent-2021AB123.json";
        try(InputStream csafJsonStream = Csaf2MapReader.class.getResourceAsStream(jsonFileName)) {

            if(csafJsonStream != null) {
                Object result = new Csaf2MapReader().readCasfDocument(new InputStreamReader(csafJsonStream));
                System.out.println(result);
            } else {
                System.out.println("Invalid Json File: "+ jsonFileName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

