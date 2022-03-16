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

    private String advisoryId;
    private WorkflowState workflowState;
    private String documentTrackingId;
    private String title;
    private String owner;
    private boolean changeable;
    private boolean deletable;
    private List<WorkflowState> allowedStateChanges;


    public AdvisoryInformationResponse() {

        this.changeable = false;
        this.deletable = false;

    }

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

    public void setAdvisoryId(String advisoryId) {
        this.advisoryId = advisoryId;
    }

    @ApiModelProperty(value = "Current workflow state of the advisory", example = "Approved")
    public WorkflowState getWorkflowState() {
        return workflowState;
    }

    public void setWorkflowState(WorkflowState workflowState) {
        this.workflowState = workflowState;
    }

    public void setAllowedStateChanges(List<WorkflowState> allowedStateChanges) {
        this.allowedStateChanges = allowedStateChanges;
    }

    @ApiModelProperty(value = "CSAF tracking id of the advisory", example = "RHBA-2019_0024")
    public String getDocumentTrackingId() {
        return documentTrackingId;
    }

    public void setDocumentTrackingId(String documentTrackingId) {
        this.documentTrackingId = documentTrackingId;
    }

    @ApiModelProperty(value = "CSAF title of the advisory", example = "Cisco IPv6 Crafted Packet Denial of Service Vulnerability")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ApiModelProperty(value = "Current owner of the advisory", example = "Mustermann")
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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
