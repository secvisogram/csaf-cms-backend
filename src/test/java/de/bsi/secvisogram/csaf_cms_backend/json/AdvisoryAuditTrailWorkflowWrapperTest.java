package de.bsi.secvisogram.csaf_cms_backend.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class AdvisoryAuditTrailWorkflowWrapperTest {


    @Test
    public void createNewFromCsafTest() throws IOException {

        AdvisoryAuditTrailWorkflowWrapper wrapper = AdvisoryAuditTrailWorkflowWrapper.createNewFrom(WorkflowState.Approved, WorkflowState.Review);
        assertThat(wrapper.getType(), equalTo(ObjectType.AuditTrailWorkflow.name()));
        assertThat(wrapper.getNewWorkflowState(), equalTo(WorkflowState.Approved.name()));
        assertThat(wrapper.getOldWorkflowState(), equalTo(WorkflowState.Review.name()));
    }
}
