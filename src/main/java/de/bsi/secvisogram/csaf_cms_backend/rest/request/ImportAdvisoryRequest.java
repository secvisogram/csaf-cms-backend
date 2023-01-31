package de.bsi.secvisogram.csaf_cms_backend.rest.request;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ImportAdvisoryRequest")
public class ImportAdvisoryRequest {

    private JsonNode csaf;

    public ImportAdvisoryRequest() {
    }

    @Schema(
            description = "The CSAF document in JSON format.",
            example = "{ document: { category: \"CSAF Base\",... }, vulnerabilities: {...}}"
    )    public JsonNode getCsaf() {
        return csaf;
    }

    public ImportAdvisoryRequest setCsaf(JsonNode csaf) {
        this.csaf = csaf;
        return this;
    }
}
