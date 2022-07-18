package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An entry in a list of advisory information items.
 */
@Schema(name = "AdvisoryDocumentInformation")
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
        this(advisoryId, workflowState, documentTrackingId, "Title of: " + documentTrackingId, "unknown");
    }

    public AdvisoryInformationResponse(String advisoryId, WorkflowState workflowState, String documentTrackingId, String title, String owner) {
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
        this.title = title;
        this.owner = owner;
    }

    @Schema(description = "The unique ID of the advisory.", example = "9690e3a3-614f-44be-8709-3aa8d58b6cb5")
    public String getAdvisoryId() {
        return advisoryId;
    }

    public void setAdvisoryId(String advisoryId) {
        this.advisoryId = advisoryId;
    }

    @Schema(description = "The current workflow state of the advisory.", example = "Approved")
    public WorkflowState getWorkflowState() {
        return workflowState;
    }

    public AdvisoryInformationResponse setWorkflowState(WorkflowState workflowState) {
        this.workflowState = workflowState;
        return this;
    }

    public void setWorkflowState(String workflowStateString) {

        this.workflowState = WorkflowState.valueOf(workflowStateString);
        if (WorkflowState.Draft == this.workflowState) {
            this.allowedStateChanges = List.of(WorkflowState.Review);
        } else if (WorkflowState.Approved == this.workflowState) {
            this.allowedStateChanges = List.of(WorkflowState.Published);
        } else if (WorkflowState.Review == this.workflowState) {
            this.allowedStateChanges = Arrays.asList(WorkflowState.Draft, WorkflowState.Approved);
        }
    }


    public void setAllowedStateChanges(List<WorkflowState> allowedStateChanges) {
        this.allowedStateChanges = Collections.unmodifiableList(allowedStateChanges);
    }

    @Schema(description = "The CSAF tracking ID of the advisory.", example = "RHBA-2019_0024")
    public String getDocumentTrackingId() {
        return documentTrackingId;
    }

    public void setDocumentTrackingId(String documentTrackingId) {
        this.documentTrackingId = documentTrackingId;
    }

    @Schema(
            description = "The CSAF title of the advisory.",
            example = "Cisco IPv6 Crafted Packet Denial of Service Vulnerability"
    )
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Schema(description = "The current owner of the advisory.", example = "Mustermann")
    public String getOwner() {
        return owner;
    }

    public AdvisoryInformationResponse setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    @Schema(description = "Indicates if the currently logged in user can change this advisory.", example = "true")
    public boolean isChangeable() {
        return changeable;
    }

    @Schema(description = "Indicates if the logged in user can delete this advisory.", example = "false")
    public boolean isDeletable() {
        return deletable;
    }

    public AdvisoryInformationResponse setChangeable(boolean changeable) {
        this.changeable = changeable;
        return this;
    }

    public AdvisoryInformationResponse setDeletable(boolean deletable) {
        this.deletable = deletable;
        return this;
    }

    @Schema(
            description = "A list of allowed state changes of the logged in user.",
            example = "[\"Approved\", \"Published\"]"
    )
    public List<WorkflowState> getAllowedStateChanges() {
        return Collections.unmodifiableList(allowedStateChanges);
    }

}
