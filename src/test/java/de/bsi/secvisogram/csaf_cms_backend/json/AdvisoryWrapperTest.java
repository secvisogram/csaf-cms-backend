package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonTitle;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static de.bsi.secvisogram.csaf_cms_backend.json.VersioningType.Semantic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class AdvisoryWrapperTest {

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void createNewFromCsafTest() throws IOException, CsafException {

        var csafJson = """
                { "document": {
                      "category": "CSAF_BASE"    }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJson), "Mustermann", Semantic.name());

        assertThat(advisory.getWorkflowState(), equalTo(WorkflowState.Draft));
        assertThat(advisory.getWorkflowStateString(), equalTo("Draft"));
        assertThat(advisory.getOwner(), equalTo("Mustermann"));
        assertThat(advisory.getRevision(), is(nullValue()));
        assertThat(advisory.getAdvisoryId(), is(nullValue()));
        assertThat(advisory.at("/csaf/document/category").asText(), equalTo("CSAF_BASE"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void createFromCouchDbTest() throws IOException, CsafException {

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
    public void createFromCouchDbTest_noType() throws IOException, CsafException {

        var revision = "rev-aa-12";
        var id = "id-aaa-bbb";
        var advisoryDbString = """
                {   "owner": "Musterfrau",
                    "workflowState": "Draft",
                    "csaf": { "document": {
                                "category": "CSAF_BASE"
                              }
                            },
                    "_rev": "%s",
                    "_id": "%s"}""".formatted(revision, id);

        var advisoryStream = new ByteArrayInputStream(advisoryDbString.getBytes(StandardCharsets.UTF_8));

        CsafException exception = assertThrows(CsafException.class,
                () -> AdvisoryWrapper.createFromCouchDb(advisoryStream));
        assertThat(exception.getRecommendedHttpState(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void createFromCouchDbTest_wrongType() {

        var revision = "rev-aa-12";
        var id = "id-aaa-bbb";
        var advisoryDbString = """
                {   "owner": "Musterfrau",
                    "workflowState": "Draft",
                    "type": "WRONG",
                    "csaf": { "document": {
                                "category": "CSAF_BASE"
                              }
                            },
                    "_rev": "%s",
                    "_id": "%s"}""".formatted(revision, id);

        var advisoryStream = new ByteArrayInputStream(advisoryDbString.getBytes(StandardCharsets.UTF_8));

        CsafException exception = assertThrows(CsafException.class,
                () -> AdvisoryWrapper.createFromCouchDb(advisoryStream));
        assertThat(exception.getRecommendedHttpState(), equalTo(HttpStatus.BAD_REQUEST));
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
        AdvisoryWrapper updatedWrapper = AdvisoryWrapper.updateFromExisting(advisory, csafToRequest(updateCsafJson));
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

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJson), "Mustermann", Semantic.name());
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

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJson), "Mustermann", Semantic.name());
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

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJson), "Mustermann", Semantic.name());
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

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJson), "Mustermann", Semantic.name());
        advisory.setDocumentTrackingCurrentReleaseDate("2019-09-07T15:50Z");
        assertThat(advisory.getDocumentTrackingCurrentReleaseDate(), equalTo("2019-09-07T15:50Z"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void addRevisionHistoryEntryTest_semanticVersioning() throws IOException, CsafException {

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonTitle("Title1")),
                "Mustermann", Semantic.name());

        advisory.setDocumentTrackingVersion("0.1.1");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary1", "LegacyVersion1"));

        String dateNowMinutes =  DateTimeFormatter.ISO_INSTANT.format(Instant.now()).substring(0, 16);
        assertThat(getRevisionAt(advisory, 0, "number"), equalTo("0.1.1"));
        assertThat(getRevisionAt(advisory, 0, "summary"), equalTo("Summary1"));
        assertThat(getRevisionAt(advisory, 0, "legacy_revision"), equalTo("LegacyVersion1"));
        assertThat(getRevisionAt(advisory, 0, "date"), startsWith(dateNowMinutes));

        advisory.setDocumentTrackingVersion("0.1.2");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary2", "LegacyVersion2"));
        assertThat(getRevisionAt(advisory, 1, "number"), equalTo("0.1.2"));
        assertThat(getRevisionAt(advisory, 1, "summary"), equalTo("Summary2"));
        assertThat(getRevisionAt(advisory, 1, "legacy_revision"), equalTo("LegacyVersion2"));
        assertThat(getRevisionAt(advisory, 1, "date"), startsWith(dateNowMinutes));

        advisory.setDocumentTrackingVersion("1.0.0");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary3", "LegacyVersion3"));
        assertThat(getRevisionAt(advisory, 2, "number"), equalTo("1.0.0"));
        assertThat(getRevisionAt(advisory, 2, "summary"), equalTo("Summary3"));
        assertThat(getRevisionAt(advisory, 2, "legacy_revision"), equalTo("LegacyVersion3"));
        assertThat(getRevisionAt(advisory, 2, "date"), startsWith(dateNowMinutes));

        advisory.setLastVersion("1.0.0");
        advisory.setDocumentTrackingVersion("1.0.1");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary4", "LegacyVersion4"));
        assertThat(getRevisionAt(advisory, 3, "number"), equalTo("1.0.1"));
        assertThat(getRevisionAt(advisory, 3, "summary"), equalTo("Summary4"));
        assertThat(getRevisionAt(advisory, 3, "legacy_revision"), equalTo("LegacyVersion4"));
        assertThat(getRevisionAt(advisory, 3, "date"), equalTo(advisory.getDocumentTrackingCurrentReleaseDate()));


        advisory.setDocumentTrackingVersion("1.0.2");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary5", "LegacyVersion5"));
        assertThat(getRevisionAt(advisory, 3, "number"), equalTo("1.0.2"));
        assertThat(getRevisionAt(advisory, 3, "summary"), equalTo("Summary5"));
        assertThat(getRevisionAt(advisory, 3, "legacy_revision"), equalTo("LegacyVersion5"));
        assertThat(getRevisionAt(advisory, 3, "date"), equalTo(advisory.getDocumentTrackingCurrentReleaseDate()));

        advisory.setDocumentTrackingVersion("2.0.0");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary6", "LegacyVersion6"));
        assertThat(getRevisionAt(advisory, 3, "number"), equalTo("2.0.0"));
        assertThat(getRevisionAt(advisory, 3, "summary"), equalTo("Summary6"));
        assertThat(getRevisionAt(advisory, 3, "legacy_revision"), equalTo("LegacyVersion6"));
        assertThat(getRevisionAt(advisory, 3, "date"), equalTo(advisory.getDocumentTrackingCurrentReleaseDate()));
        assertThat(advisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(4));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void addRevisionHistoryEntryTest_integerVersioning() throws IOException, CsafException {

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonTitle("Title1")),
                "Mustermann", VersioningType.Integer.name());

        advisory.setDocumentTrackingVersion("0");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary1", "LegacyVersion1"));

        assertThat(getRevisionAt(advisory, 0, "number"), equalTo("0"));
        assertThat(getRevisionAt(advisory, 0, "summary"), equalTo("Summary1"));
        assertThat(getRevisionAt(advisory, 0, "legacy_revision"), equalTo("LegacyVersion1"));
        assertThat(getRevisionAt(advisory, 0, "date"), equalTo(advisory.getDocumentTrackingCurrentReleaseDate()));

        advisory.setDocumentTrackingVersion("1");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary2", "LegacyVersion2"));
        assertThat(getRevisionAt(advisory, 0, "number"), equalTo("1"));
        assertThat(getRevisionAt(advisory, 0, "summary"), equalTo("Summary2"));
        assertThat(getRevisionAt(advisory, 0, "legacy_revision"), equalTo("LegacyVersion2"));
        assertThat(getRevisionAt(advisory, 0, "date"), equalTo(advisory.getDocumentTrackingCurrentReleaseDate()));

        advisory.setDocumentTrackingVersion("1");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary3", "LegacyVersion3"));
        assertThat(getRevisionAt(advisory, 0, "number"), equalTo("1"));
        assertThat(getRevisionAt(advisory, 0, "summary"), equalTo("Summary3"));
        assertThat(getRevisionAt(advisory, 0, "legacy_revision"), equalTo("LegacyVersion3"));
        assertThat(getRevisionAt(advisory, 0, "date"), equalTo(advisory.getDocumentTrackingCurrentReleaseDate()));

        advisory.setLastVersion("1");
        advisory.setDocumentTrackingVersion("2");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary4", "LegacyVersion4"));
        assertThat(getRevisionAt(advisory, 1, "number"), equalTo("2"));
        assertThat(getRevisionAt(advisory, 1, "summary"), equalTo("Summary4"));
        assertThat(getRevisionAt(advisory, 1, "legacy_revision"), equalTo("LegacyVersion4"));
        assertThat(getRevisionAt(advisory, 1, "date"), equalTo(advisory.getDocumentTrackingCurrentReleaseDate()));

        advisory.setDocumentTrackingVersion("2");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary5", "LegacyVersion5"));
        assertThat(getRevisionAt(advisory, 1, "number"), equalTo("2"));
        assertThat(getRevisionAt(advisory, 1, "summary"), equalTo("Summary5"));
        assertThat(getRevisionAt(advisory, 1, "legacy_revision"), equalTo("LegacyVersion5"));
        assertThat(getRevisionAt(advisory, 1, "date"), equalTo(advisory.getDocumentTrackingCurrentReleaseDate()));

        advisory.setDocumentTrackingVersion("2");
        advisory.addRevisionHistoryEntry(new CreateAdvisoryRequest("Summary6", "LegacyVersion6"));
        assertThat(getRevisionAt(advisory, 1, "number"), equalTo("2"));
        assertThat(getRevisionAt(advisory, 1, "summary"), equalTo("Summary6"));
        assertThat(getRevisionAt(advisory, 1, "legacy_revision"), equalTo("LegacyVersion6"));
        assertThat(getRevisionAt(advisory, 1, "date"), equalTo(advisory.getDocumentTrackingCurrentReleaseDate()));
        assertThat(advisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));
    }
    private String getRevisionAt(AdvisoryWrapper advisory, int pos, String field) {

        return advisory.getCsaf().at("/document/tracking/revision_history/" + pos).get(field).asText();
    }

}