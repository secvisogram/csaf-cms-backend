package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RemoveIdHelper {

    public static final String COMMNENT_NODE_ID = "nodeId";

    public static void removeCommentIds(JsonNode jsonNode) {

        removeIds(jsonNode, COMMNENT_NODE_ID);
    }

    public static void removeIds(JsonNode jsonNode, String idName) {
        if (jsonNode.isArray()) {
            for (JsonNode arrayItem : jsonNode) {
                removeIds(arrayItem, idName);
            }
        } else if (jsonNode.isObject()) {
            for (JsonNode field : jsonNode) {
                removeIds(field, idName);
            }
            ((ObjectNode) jsonNode).remove(idName);
        }
    }
}
