package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Entry in a list of advisory information's
 */
@ApiModel("AdvisoryDocumentInformation")
public class AdvisoryInformationResponse {

    private final long advisoryId;
    private final WorkflowState workflowState;
    private final String documentTrackingId;
    private String title;
    private String owner;
    private final boolean changeable; // User can change/delete
    private final boolean deletable; // User can change/delete
    private List<WorkflowState> allowedStateChanges;

    public AdvisoryInformationResponse(long advisoryId, WorkflowState status) {
        this(advisoryId, status, "");
    }

    public AdvisoryInformationResponse(long advisoryId, WorkflowState workflowState, String documentTrackingId) {
        this.advisoryId = advisoryId;
        this.workflowState = workflowState;
        this.documentTrackingId = documentTrackingId;
        this.changeable = true;
        this.deletable = true;
        if (WorkflowState.Draft == workflowState) {
            this.allowedStateChanges = List.of(WorkflowState.Review);
        } else if (WorkflowState.Approved == workflowState) {
            this.allowedStateChanges = List.of(WorkflowState.Published);
        } else if (WorkflowState.Review == workflowState) {
            this.allowedStateChanges = Arrays.asList(WorkflowState.Draft, WorkflowState.Approved);
        }
    }

    @ApiModelProperty(value = "Unique Id of the advisory", example = "334723")
    public long getAdvisoryId() {
        return advisoryId;
    }

    @ApiModelProperty(value = "Current workflow state of the advisory", example = "Approved")
    public WorkflowState getWorkflowState() {
        return workflowState;
    }

    @ApiModelProperty(value = "CSAF tracking id of the advisory", example = "RHBA-2019_0024")
    public String getDocumentTrackingId() {
        return documentTrackingId;
    }

    @ApiModelProperty(value = "CSAF title of the advisory", example = "Cisco IPv6 Crafted Packet Denial of Service Vulnerability")
    public String getTitle() {
        return title;
    }

    @ApiModelProperty(value = "Current owner of the advisory", example = "Mustermann")
    public String getOwner() {
        return owner;
    }

    @ApiModelProperty(value = "Can the logged in user change this advisory?", example = "true")
    public boolean isChangeable() {
        return changeable;
    }

    @ApiModelProperty(value = "Can the logged in user delete this advisory?", example = "false")
    public boolean isDeletable() {
        return deletable;
    }

    @ApiModelProperty(value = "Allowed state changes of the logged in user", example = "Published")
    public List<WorkflowState> getAllowedStateChanges() {
        return allowedStateChanges;
    }
}
