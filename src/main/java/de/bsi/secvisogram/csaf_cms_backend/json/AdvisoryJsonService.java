package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonPatch;

public class AdvisoryJsonService {


    /**
     * Apply path o JsonNode
     * @param patch the patch to apply
     * @param source the JsonNode the pacht is applied o
     * @return the patched JsonNode
     */
    public JsonNode applyJsonPatch(JsonNode patch, JsonNode source) {

        return JsonPatch.apply(patch, source);
    }

}
