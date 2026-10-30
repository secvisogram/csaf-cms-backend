package de.bsi.secvisogram.csaf_cms_backend.json;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Normalizes CSAF JSON object member order as required by CSAF 2.0 section 6.2.13.
 *
 * <p>JSON arrays are deliberately not reordered because their item order is semantically relevant.</p>
 */
public final class CsafJsonSorter {

    private CsafJsonSorter() {
        // utility class
    }

    /**
     * Recursively sorts all object keys in their natural alphabetical order.
     *
     * @param node CSAF JSON node to normalize; {@code null} is accepted
     * @return the supplied node after normalization
     */
    public static JsonNode sortRecursively(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<Map.Entry<String, JsonNode>> entries = objectNode.properties().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .toList();
            entries.forEach(entry -> sortRecursively(entry.getValue()));
            objectNode.removeAll();
            entries.forEach(entry -> objectNode.set(entry.getKey(), entry.getValue()));
        } else if (node.isArray()) {
            node.forEach(CsafJsonSorter::sortRecursively);
        }
        return node;
    }
}