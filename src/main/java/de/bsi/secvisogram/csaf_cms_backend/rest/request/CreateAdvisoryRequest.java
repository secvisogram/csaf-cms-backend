package de.bsi.secvisogram.csaf_cms_backend.rest.request;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateAdvisoryRequest")
public class CreateAdvisoryRequest {

    private String summary;
    private String legacyVersion;
    private JsonNode csaf;

    public String getSummary() {
        return summary;
    }

    public String getLegacyVersion() {
        return legacyVersion;
    }

    public JsonNode getCsaf() {
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
