package de.bsi.secvisogram.csaf_cms_backend.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

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
