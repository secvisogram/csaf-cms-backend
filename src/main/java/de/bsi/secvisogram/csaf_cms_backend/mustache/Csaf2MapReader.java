package de.bsi.secvisogram.csaf_cms_backend.mustache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a CSAF JSON File to HashMap.
 * JSON-Objects are converted to Hashmap
 * JSON-Arrays are converted to Arrays
 * JSON-Properties are converted to Map-Entries
 */
public class Csaf2MapReader {

    private static final Logger LOG = LoggerFactory.getLogger(Csaf2MapReader.class);

    private final ObjectMapper mapper;

    public Csaf2MapReader() {

        this.mapper = new ObjectMapper();
    }


    public Object readCsafDocument(Reader csafDocument) throws IOException {

        JsonNode rootNode = mapper.readValue(csafDocument, JsonNode.class);
        return convertToObject(rootNode);
    }

    public Object convertToObject(JsonNode startNode) {

        final Object resultObj;
        if (startNode == null) {
            resultObj = null;
        } else if (startNode.isArray()) {
            List<Object> resultList = new ArrayList<>();
            for (JsonNode curValue : startNode) {
                resultList.add(convertToObject(curValue));
            }
            resultObj = resultList;
        } else if (startNode.isObject()) {
            final Map<String, Object> resultMap = new HashMap<>();
            Iterator<String> names = startNode.fieldNames();
            names.forEachRemaining(name -> resultMap.put(name, convertToObject(startNode.get(name))));
            resultObj = resultMap;
        } else if (startNode.isNumber()) {
            resultObj = startNode.asLong();
        } else if (startNode.isTextual()) {
            resultObj = startNode.asText();
        } else {
            resultObj = null;
        }

        return resultObj;
    }

}

