package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import com.fasterxml.jackson.databind.JsonNode;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Advisory content response
 */
@Schema(name = "AdvisoryDocument")
public class AdvisoryResponse extends AdvisoryInformationResponse {

    private JsonNode csaf;

    public AdvisoryResponse(String advisoryId, WorkflowState workflowState, JsonNode csafJsonWithComments) {
        super(advisoryId, workflowState);
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


}
