package de.bsi.secvisogram.csaf_cms_backend.rest.request;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateAdvisoryRequest")
public class CreateAdvisoryRequest {

    private String summary;
    private String legacyVersion;
    private JsonNode csaf;

    public CreateAdvisoryRequest() {
    }

    public CreateAdvisoryRequest(String summary, String legacyVersion) {
        this.summary = summary;
        this.legacyVersion = legacyVersion;
    }

    @Schema(
            description = "The text of the summary in the revision history.",
            example = "This is a summary."
    )
    public String getSummary() {
        return summary;
    }

    @Schema(
            description = "The text of the legacy version in the revision history.",
            example = "This is a legcy notice."
    )
    public String getLegacyVersion() {
        return legacyVersion;
    }

    @Schema(
            description = "The CSAF document in JSON format including additional node IDs.",
            example = "{$nodeId: \"nodeId123\", document: { $nodeId: \"nodeId567\", category: \"CSAF Base\",... }, vulnerabilities: {...}}"
    )    public JsonNode getCsaf() {
        return csaf;
    }

    public CreateAdvisoryRequest setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public CreateAdvisoryRequest setLegacyVersion(String legacyVersion) {
        this.legacyVersion = legacyVersion;
        return this;
    }

    public CreateAdvisoryRequest setCsaf(JsonNode csaf) {
        this.csaf = csaf;
        return this;
    }
}
