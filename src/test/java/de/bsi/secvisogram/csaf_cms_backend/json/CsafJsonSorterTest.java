package de.bsi.secvisogram.csaf_cms_backend.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

class CsafJsonSorterTest {

    @Test
    void sortsObjectKeysRecursivelyWithoutChangingArrayOrder() throws Exception {
        JsonNode csaf = new JsonMapper().readTree("""
                {
                  "vulnerabilities": [
                    { "zebra": { "beta": 1, "alpha": 2 }, "alpha": "first" },
                    { "delta": "second", "charlie": "third" }
                  ],
                  "document": {
                    "tracking": { "version": "1.0.0", "id": "CSAF-2026-1" },
                    "category": "csaf_base"
                  }
                }
                """);

        CsafJsonSorter.sortRecursively(csaf);

        assertEquals(List.of("document", "vulnerabilities"), propertyNames(csaf));
        assertEquals(List.of("category", "tracking"), propertyNames(csaf.get("document")));
        assertEquals(List.of("id", "version"), propertyNames(csaf.at("/document/tracking")));
        assertEquals(List.of("alpha", "zebra"), propertyNames(csaf.at("/vulnerabilities/0")));
        assertEquals(List.of("alpha", "beta"), propertyNames(csaf.at("/vulnerabilities/0/zebra")));
        assertEquals("first", csaf.at("/vulnerabilities/0/alpha").asString());
        assertEquals("second", csaf.at("/vulnerabilities/1/delta").asString());
    }

    private List<String> propertyNames(JsonNode node) {
        return node.properties().stream().map(entry -> entry.getKey()).toList();
    }
}