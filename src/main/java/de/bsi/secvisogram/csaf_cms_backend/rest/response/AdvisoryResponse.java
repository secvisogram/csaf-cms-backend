package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import com.fasterxml.jackson.databind.JsonNode;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Advisory content response
 */
@Schema(name = "AdvisoryDocument")
public class AdvisoryResponse extends AdvisoryInformationResponse {

    private String revision;
    private JsonNode csaf;

    public AdvisoryResponse(String advisoryId, WorkflowState status, JsonNode csafJsonWithComments) {
        super(advisoryId, status);
        this.csaf = csafJsonWithComments;
    }

    @Schema(
            description = "The current CSAF document enhanced with comment IDs.",
            example = "{$nodeId: \"nodeId123\", document: { $nodeId: \"nodeId567\", category: \"CSAF Base\",... }, vulnerabilities: {...}}"
    )
    public JsonNode getCsaf() {
        return this.csaf;
    }

    public void setCsaf(JsonNode csaf) {
        this.csaf = csaf;
    }

    @Schema(
            description = "The document revision for optimistic concurrency.",
            example = "2-efaa5db9409b2d4300535c70aaf6a66b"
    )
    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}
