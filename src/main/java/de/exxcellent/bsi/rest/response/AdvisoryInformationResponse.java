package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.WorkflowState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Entry in a list of advisory information's
 */
@ApiModel("AdvisoryDocumentInformation")
public class AdvisoryInformationResponse {

    private final String advisoryId;
    private final WorkflowState workflowState;
    private final String documentTrackingId;
    private final String title;
    private final String owner;
    private final boolean changeable;
    private final boolean deletable;
    private List<WorkflowState> allowedStateChanges;

    public AdvisoryInformationResponse(String advisoryId, WorkflowState status) {
        this(advisoryId, status, "");
    }

    public AdvisoryInformationResponse(String advisoryId, WorkflowState workflowState, String documentTrackingId) {
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
        this.title = "Title of: " + documentTrackingId;
        this.owner = "Mustermann";
    }

    @ApiModelProperty(value = "Unique Id of the advisory", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getAdvisoryId() {
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
        return Collections.unmodifiableList(allowedStateChanges);
    }

}
