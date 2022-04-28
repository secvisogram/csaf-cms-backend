package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Entry in a list of advisory information's
 */
@Schema(name="AdvisoryDocumentInformation")
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

    @Schema(description  = "Unique Id of the advisory", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getAdvisoryId() {
        return advisoryId;
    }

    public void setAdvisoryId(String advisoryId) {
        this.advisoryId = advisoryId;
    }

    @Schema(description  = "Current workflow state of the advisory", example = "Approved")
    public WorkflowState getWorkflowState() {
        return workflowState;
    }

    public void setWorkflowState(WorkflowState workflowState) {
        this.workflowState = workflowState;
    }

    public void setAllowedStateChanges(List<WorkflowState> allowedStateChanges) {
        this.allowedStateChanges = Collections.unmodifiableList(allowedStateChanges);
    }

    @Schema(description  = "CSAF tracking id of the advisory", example = "RHBA-2019_0024")
    public String getDocumentTrackingId() {
        return documentTrackingId;
    }

    public void setDocumentTrackingId(String documentTrackingId) {
        this.documentTrackingId = documentTrackingId;
    }

    @Schema(description  = "CSAF title of the advisory", example = "Cisco IPv6 Crafted Packet Denial of Service Vulnerability")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Schema(description  = "Current owner of the advisory", example = "Mustermann")
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Schema(description  = "Can the logged in user change this advisory?", example = "true")
    public boolean isChangeable() {
        return changeable;
    }

    @Schema(description  = "Can the logged in user delete this advisory?", example = "false")
    public boolean isDeletable() {
        return deletable;
    }

    @Schema(description  = "Allowed state changes of the logged in user", example = "Published")
    public List<WorkflowState> getAllowedStateChanges() {
        return Collections.unmodifiableList(allowedStateChanges);
    }

}
