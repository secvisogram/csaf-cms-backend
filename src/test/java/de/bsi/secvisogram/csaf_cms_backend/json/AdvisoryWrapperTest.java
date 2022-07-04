package de.bsi.secvisogram.csaf_cms_backend.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdvisoryWrapperTest {

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void createNewFromCsafTest() throws IOException {

        var csafJson = """
                { "document": {
                      "category": "CSAF_BASE"    }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann");

        assertThat(advisory.getWorkflowState(), equalTo(WorkflowState.Draft));
        assertThat(advisory.getWorkflowStateString(), equalTo("Draft"));
        assertThat(advisory.getOwner(), equalTo("Mustermann"));
        assertThat(advisory.getRevision(), is(nullValue()));
        assertThat(advisory.getAdvisoryId(), is(nullValue()));
        assertThat(advisory.at("/csaf/document/category").asText(), equalTo("CSAF_BASE"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void createFromCouchDbTest() throws IOException {

        var revision = "rev-aa-12";
        var id = "id-aaa-bbb";
        var advisoryDbString = """
                {   "owner": "Musterfrau",
                    "type": "Advisory",
                    "workflowState": "Draft",
                    "csaf": { "document": {
                                "category": "CSAF_BASE"
                              }
                            },
                    "_rev": "%s",
                    "_id": "%s"}""".formatted(revision, id);

        var advisoryStream = new ByteArrayInputStream(advisoryDbString.getBytes(StandardCharsets.UTF_8));
        var advisory = AdvisoryWrapper.createFromCouchDb(advisoryStream);

        assertThat(advisory.getWorkflowState(), equalTo(WorkflowState.Draft));
        assertThat(advisory.getWorkflowStateString(), equalTo("Draft"));
        assertThat(advisory.getOwner(), equalTo("Musterfrau"));
        assertThat(advisory.getRevision(), equalTo(revision));
        assertThat(advisory.getAdvisoryId(), equalTo(id));
        assertThat(advisory.at("/csaf/document/category").asText(), equalTo("CSAF_BASE"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void updateFromExistingTest() throws IOException {

        var revision = "rev-aa-12";
        var id = "id-aaa-bbb";
        var advisoryDbString = """
                {   "owner": "Musterfrau",
                    "type": "Advisory",
                    "workflowState": "Draft",
                    "csaf": { "document": {
                                "category": "CSAF_BASE"
                              }
                            },
                    "_rev": "%s",
                    "_id": "%s"}""".formatted(revision, id);

        var updateCsafJson = """
                { "document": {
                      "category": "CHANGED",
                          "title": "New Title"
                       }
                }""";


        var advisoryStream = new ByteArrayInputStream(advisoryDbString.getBytes(StandardCharsets.UTF_8));
        var advisory = AdvisoryWrapper.createFromCouchDb(advisoryStream);
        AdvisoryWrapper updatedWrapper = AdvisoryWrapper.updateFromExisting(advisory, updateCsafJson);
        assertThat(updatedWrapper.getWorkflowState(), equalTo(WorkflowState.Draft));
        assertThat(updatedWrapper.getOwner(), equalTo("Musterfrau"));
        assertThat(updatedWrapper.getRevision(), is(nullValue()));
        assertThat(updatedWrapper.getAdvisoryId(), equalTo(id));
        assertThat(updatedWrapper.at("/csaf/document/category").asText(), equalTo("CHANGED"));
        assertThat(updatedWrapper.at("/csaf/document/title").asText(), equalTo("New Title"));
    }

}
