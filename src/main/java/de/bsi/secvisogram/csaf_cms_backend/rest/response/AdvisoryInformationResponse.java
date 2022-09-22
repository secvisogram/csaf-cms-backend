package de.bsi.secvisogram.csaf_cms_backend.rest.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

/**
 * An entry in a list of advisory information items.
 */
@Schema(name = "AdvisoryDocumentInformation")
public class AdvisoryInformationResponse {

    protected String revision;
    private String advisoryId;
    private WorkflowState workflowState;
    private String documentTrackingId;
    private String title;
    private String owner;
    private boolean changeable;
    private boolean deletable;

    private boolean canCreateVersion;
    private List<WorkflowState> allowedStateChanges;

    private String currentReleaseDate;

    public AdvisoryInformationResponse() {

        this.changeable = false;
        this.deletable = false;
        this.canCreateVersion = false;
        this.allowedStateChanges = Collections.emptyList();

    }

    public AdvisoryInformationResponse(String advisoryId) {

        this.advisoryId = advisoryId;
        this.changeable = false;
        this.deletable = false;
        this.canCreateVersion = false;
        this.allowedStateChanges = Collections.emptyList();
    }

    public AdvisoryInformationResponse(String advisoryId, WorkflowState workflowState) {
        this.advisoryId = advisoryId;
        this.workflowState = workflowState;
        this.changeable = false;
        this.deletable = false;
        this.canCreateVersion = false;
        this.allowedStateChanges = Collections.emptyList();
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

    @Schema(description = "Indicates if the logged in user can create a new csaf document version of this advisory.", example = "false")
    public boolean isCanCreateVersion() {
        return canCreateVersion;
    }

    public AdvisoryInformationResponse setCanCreateVersion(boolean canCreateVersion) {
        this.canCreateVersion = canCreateVersion;
        return this;
    }

    @Schema(
            description = "A list of allowed state changes of the logged in user.",
            example = "[\"Approved\", \"Published\"]"
    )
    public List<WorkflowState> getAllowedStateChanges() {
        return Collections.unmodifiableList(allowedStateChanges);
    }

    /**
     * The value of the advisory at "document/tracking/current_release_date"
     * @return the date as iso string
     */
    public String getCurrentReleaseDate() {
        return currentReleaseDate;
    }

    public AdvisoryInformationResponse setCurrentReleaseDate(String currentReleaseDate) {
        this.currentReleaseDate = currentReleaseDate;
        return this;
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
