package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.json.VersioningType.Semantic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class AdvisoryWrapperTest {

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void createNewFromCsafTest() throws IOException, CsafException {

        var csafJson = """
                { "document": {
                      "category": "CSAF_BASE"    }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann", Semantic.name());

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
    public void updateFromExistingTest() throws IOException, CsafException {

        var revision = "rev-aa-12";
        var id = "id-aaa-bbb";
        var advisoryDbString = """
                {   "owner": "Musterfrau",
                    "type": "Advisory",
                    "workflowState": "Draft",
                    "versioningType": "Semantic",
                    "lastMajorVersion": 0,
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

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void setDocumentTrackingVersionTest() throws IOException, CsafException {

        var csafJson = """
                { "document": {
                      "category": "CSAF_BASE"    }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann", Semantic.name());
        advisory.setDocumentTrackingVersion("0.0.1");
        assertThat(advisory.getDocumentTrackingVersion(), equalTo("0.0.1"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void setDocumentTrackingVersionTest_updateVersion() throws IOException, CsafException {

        var csafJson = """
                { "document": {
                      "category": "CSAF_BASE",
                      "tracking": {
                            "current_release_date": "2022-01-11T11:00:00.000Z",
                            "id": "exxcellent-2021AB123",
                            "initial_release_date": "2022-01-12T11:00:00.000Z",
                            "revision_history": [
                              {
                                "date": "2022-01-12T11:00:00.000Z",
                                "number": "0.0.1",
                                "summary": "Test rsvSummary"
                              }
                            ],
                            "status": "draft",
                            "version": "2.0.1"
                      }
                  }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann", Semantic.name());
        advisory.setDocumentTrackingVersion("0.0.1");
        assertThat(advisory.getDocumentTrackingVersion(), equalTo("0.0.1"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void setDocumentTrackingStatusTest() throws IOException, CsafException {

        var csafJson = """
                { "document": {
                      "category": "CSAF_BASE"    }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann", Semantic.name());
        advisory.setDocumentTrackingStatus(DocumentTrackingStatus.Interim);
        assertThat(advisory.getDocumentTrackingStatus(), equalTo("interim"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void setDocumentTrackingCurrentReleaseDateTest() throws IOException, CsafException {

        var csafJson = """
                { "document": {
                      "category": "CSAF_BASE"    }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann", Semantic.name());
        advisory.setDocumentTrackingCurrentReleaseDate("2019-09-07T15:50Z");
        assertThat(advisory.getDocumentTrackingCurrentReleaseDate(), equalTo("2019-09-07T15:50Z"));
    }

}