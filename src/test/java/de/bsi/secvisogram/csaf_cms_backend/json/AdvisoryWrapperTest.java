package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonTitle;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper.calculateFileName;
import static de.bsi.secvisogram.csaf_cms_backend.json.VersioningType.Integer;
import static de.bsi.secvisogram.csaf_cms_backend.json.VersioningType.Semantic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    public void setDocumentTrackingGeneratorEngineNameAndVersionTest() throws IOException, CsafException {

        var csafJson = """
                { "document": {
                      "category": "CSAF_BASE"    }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJson), "Mustermann", Semantic.name());
        advisory.setDocumentTrackingGeneratorEngineName("CSAF CMS Backend");
        advisory.setDocumentTrackingGeneratorEngineVersion("0.0.1");
        assertThat(advisory.getDocumentTrackingGeneratorEngineName(), equalTo("CSAF CMS Backend"));
        assertThat(advisory.getDocumentTrackingGeneratorEngineVersion(), equalTo("0.0.1"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void addRevisionHistoryElementTest_semanticVersioning() throws IOException, CsafException, InterruptedException {

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonTitle("Title1")),
                "Mustermann", Semantic.name());

        String dateNow = getCurrentTimestamp();
        String dateNowMinutes = dateNow.substring(0, 16);
        advisory.setDocumentTrackingVersion("0.1.1");
        advisory.addRevisionHistoryElement(new CreateAdvisoryRequest("Summary1", "LegacyVersion1"), dateNow);

        assertThat(getRevisionAt(advisory, 0, "number"), equalTo("0.1.1"));
        assertThat(getRevisionAt(advisory, 0, "summary"), equalTo("Summary1"));
        assertThat(getRevisionAt(advisory, 0, "legacy_version"), equalTo("LegacyVersion1"));
        assertThat(getRevisionAt(advisory, 0, "date"), equalTo(dateNow));

        Thread.sleep(5);
        advisory.setDocumentTrackingVersion("0.1.2");
        advisory.addRevisionHistoryElement(new CreateAdvisoryRequest("Summary2", "LegacyVersion2"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 1, "number"), equalTo("0.1.2"));
        assertThat(getRevisionAt(advisory, 1, "summary"), equalTo("Summary2"));
        assertThat(getRevisionAt(advisory, 1, "legacy_version"), equalTo("LegacyVersion2"));
        assertThat(getRevisionAt(advisory, 1, "date"), startsWith(dateNowMinutes));

        Thread.sleep(5);
        advisory.setDocumentTrackingVersion("1.0.0");
        advisory.addRevisionHistoryElement(new CreateAdvisoryRequest("Summary3", "LegacyVersion3"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 2, "number"), equalTo("1.0.0"));
        assertThat(getRevisionAt(advisory, 2, "summary"), equalTo("Summary3"));
        assertThat(getRevisionAt(advisory, 2, "legacy_version"), equalTo("LegacyVersion3"));
        assertThat(getRevisionAt(advisory, 2, "date"), startsWith(dateNowMinutes));

        Thread.sleep(5);
        advisory.setLastVersion("1.0.0");
        advisory.setDocumentTrackingVersion("1.0.1");
        advisory.addRevisionHistoryElement(new CreateAdvisoryRequest("Summary4", "LegacyVersion4"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 3, "number"), equalTo("1.0.1"));
        assertThat(getRevisionAt(advisory, 3, "summary"), equalTo("Summary4"));
        assertThat(getRevisionAt(advisory, 3, "legacy_version"), equalTo("LegacyVersion4"));
        assertThat(getRevisionAt(advisory, 3, "date"), startsWith(dateNowMinutes));

        Thread.sleep(5);
        advisory.setDocumentTrackingVersion("1.0.2");
        advisory.editLastRevisionHistoryElement(new CreateAdvisoryRequest("Summary5", "LegacyVersion5"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 3, "number"), equalTo("1.0.2"));
        assertThat(getRevisionAt(advisory, 3, "summary"), equalTo("Summary5"));
        assertThat(getRevisionAt(advisory, 3, "legacy_version"), equalTo("LegacyVersion5"));
        assertThat(getRevisionAt(advisory, 3, "date"), startsWith(dateNowMinutes));

        Thread.sleep(5);
        advisory.setDocumentTrackingVersion("2.0.0");
        advisory.editLastRevisionHistoryElement(new CreateAdvisoryRequest("Summary6", "LegacyVersion6"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 3, "number"), equalTo("2.0.0"));
        assertThat(getRevisionAt(advisory, 3, "summary"), equalTo("Summary6"));
        assertThat(getRevisionAt(advisory, 3, "legacy_version"), equalTo("LegacyVersion6"));
        assertThat(getRevisionAt(advisory, 3, "date"), startsWith(dateNowMinutes));
        assertThat(advisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(4));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void addRevisionHistoryElementTest_integerVersioning() throws IOException, CsafException {

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonTitle("Title1")),
                "Mustermann", Integer.name());

        String dateNow = getCurrentTimestamp();
        String dateNowMinutes = dateNow.substring(0, 16);
        advisory.setDocumentTrackingVersion("0");
        advisory.addRevisionHistoryElement(new CreateAdvisoryRequest("Summary1", "LegacyVersion1"), dateNow);

        assertThat(getRevisionAt(advisory, 0, "number"), equalTo("0"));
        assertThat(getRevisionAt(advisory, 0, "summary"), equalTo("Summary1"));
        assertThat(getRevisionAt(advisory, 0, "legacy_version"), equalTo("LegacyVersion1"));
        assertThat(getRevisionAt(advisory, 0, "date"), equalTo(dateNow));
        assertThat(advisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(1));

        advisory.setDocumentTrackingVersion("1");
        advisory.addRevisionHistoryElement(new CreateAdvisoryRequest("Summary2", "LegacyVersion2"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 1, "number"), equalTo("1"));
        assertThat(getRevisionAt(advisory, 1, "summary"), equalTo("Summary2"));
        assertThat(getRevisionAt(advisory, 1, "legacy_version"), equalTo("LegacyVersion2"));
        assertThat(getRevisionAt(advisory, 1, "date"), startsWith(dateNowMinutes));
        assertThat(advisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));

        advisory.setDocumentTrackingVersion("1");
        advisory.editLastRevisionHistoryElement(new CreateAdvisoryRequest("Summary3", "LegacyVersion3"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 1, "number"), equalTo("1"));
        assertThat(getRevisionAt(advisory, 1, "summary"), equalTo("Summary3"));
        assertThat(getRevisionAt(advisory, 1, "legacy_version"), equalTo("LegacyVersion3"));
        assertThat(getRevisionAt(advisory, 1, "date"), startsWith(dateNowMinutes));
        assertThat(advisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(2));

        advisory.setLastVersion("1");
        advisory.setDocumentTrackingVersion("2");
        advisory.addRevisionHistoryElement(new CreateAdvisoryRequest("Summary4", "LegacyVersion4"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 2, "number"), equalTo("2"));
        assertThat(getRevisionAt(advisory, 2, "summary"), equalTo("Summary4"));
        assertThat(getRevisionAt(advisory, 2, "legacy_version"), equalTo("LegacyVersion4"));
        assertThat(getRevisionAt(advisory, 2, "date"), startsWith(dateNowMinutes));
        assertThat(advisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));

        advisory.setDocumentTrackingVersion("2");
        advisory.editLastRevisionHistoryElement(new CreateAdvisoryRequest("Summary5", "LegacyVersion5"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 2, "number"), equalTo("2"));
        assertThat(getRevisionAt(advisory, 2, "summary"), equalTo("Summary5"));
        assertThat(getRevisionAt(advisory, 2, "legacy_version"), equalTo("LegacyVersion5"));
        assertThat(getRevisionAt(advisory, 2, "date"), startsWith(dateNowMinutes));
        assertThat(advisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));

        advisory.setDocumentTrackingVersion("2");
        advisory.editLastRevisionHistoryElement(new CreateAdvisoryRequest("Summary6", "LegacyVersion6"), getCurrentTimestamp());
        assertThat(getRevisionAt(advisory, 2, "number"), equalTo("2"));
        assertThat(getRevisionAt(advisory, 2, "summary"), equalTo("Summary6"));
        assertThat(getRevisionAt(advisory, 2, "legacy_version"), equalTo("LegacyVersion6"));
        assertThat(getRevisionAt(advisory, 2, "date"), startsWith(dateNowMinutes));
        assertThat(advisory.getCsaf().at("/document/tracking/revision_history").size(), equalTo(3));
    }

    private String getCurrentTimestamp() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    private String getRevisionAt(AdvisoryWrapper advisory, int pos, String field) {

        return advisory.getCsaf().at("/document/tracking/revision_history/" + pos).get(field).asText();
    }

    private void addArtificialHistory(AdvisoryWrapper advisory, List<String> revisions) {
        final ObjectMapper jacksonMapper = new ObjectMapper();
        ArrayNode historyNode = jacksonMapper.createArrayNode();

        revisions.forEach(rev -> {
            ObjectNode revNode = jacksonMapper.createObjectNode();
            revNode.put("number", rev);
            revNode.put("summary", "some summary");
            historyNode.add(revNode);
        });
        ObjectNode trackingNode = (ObjectNode) advisory.at("/csaf/document/tracking");
        // set version of advisory to last revision history element version
        trackingNode.put("version", revisions.get(revisions.size() - 1));
        trackingNode.set("revision_history", historyNode);
    }

    private List<String> getRevisionHistoryVersions(AdvisoryWrapper advisory) {
        List<String> versionNumbers = new ArrayList<>();
        advisory.getCsaf().at("/document/tracking/revision_history").forEach(
                revHistElem -> versionNumbers.add(revHistElem.at("/number").asText())
        );
        return versionNumbers;
    }

    private void assertRevisionHistoryVersionsMatch(AdvisoryWrapper advisory, List<String> expectedVersions, String message) {
        List<String> revisionHistoryVersions = getRevisionHistoryVersions(advisory);
        assertEquals(expectedVersions, revisionHistoryVersions, message);
    }

    private static Stream<Arguments> removeAllPrereleaseVersions_IntegerArgs() {
        return Stream.of(
                Arguments.of(Semantic, List.of("0.0.1", "1.0.0", "1.0.1"), WorkflowState.Draft, List.of("1.0.0", "1.0.1")),
                Arguments.of(Semantic, List.of("0.0.1", "1.0.0", "1.0.1"), WorkflowState.Published, List.of("1.0.0", "1.0.1")),
                Arguments.of(Semantic, List.of("0.0.1", "0.0.1-1.0", "0.0.2"), WorkflowState.Draft, List.of()),
                Arguments.of(Semantic, List.of("1.0.0", "1.1.0-1.0", "2.0.0-1.0"), WorkflowState.Draft, List.of("1.0.0")),
                Arguments.of(Semantic, List.of("1.0.0", "1.1.0-1.0", "2.0.0-1.0"), WorkflowState.Published, List.of("1.0.0")),
                Arguments.of(Integer, List.of("0", "1"), WorkflowState.Draft, List.of("1")),
                Arguments.of(Integer, List.of("0", "1"), WorkflowState.Published, List.of("1")),
                Arguments.of(Integer, List.of("1", "2", "3"), WorkflowState.Draft, List.of("1", "2", "3")),
                Arguments.of(Integer, List.of("1", "2", "3"), WorkflowState.Published, List.of("1", "2", "3"))
        );
    }

    @ParameterizedTest()
    @MethodSource("removeAllPrereleaseVersions_IntegerArgs")
    public void removeAllPrereleaseVersionsTest(
            VersioningType versioningType,
            List<String> initialVersionHistory,
            WorkflowState workflowState,
            List<String> expectedVersionHistoryAfterRemove) throws IOException, CsafException {

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonTitle("Title1")),
                "Mustermann", versioningType.name());

        addArtificialHistory(advisory, initialVersionHistory);
        advisory.setDocumentTrackingVersion(initialVersionHistory.get(initialVersionHistory.size() - 1));
        advisory.setWorkflowState(workflowState);

        assertRevisionHistoryVersionsMatch(advisory, initialVersionHistory,
                "the version history should have been added correctly");

        advisory.removeAllPrereleaseVersions();

        assertRevisionHistoryVersionsMatch(advisory, expectedVersionHistoryAfterRemove,
                "revision history elements should have been removed as expected");

        advisory.removeAllPrereleaseVersions();

        assertRevisionHistoryVersionsMatch(advisory, expectedVersionHistoryAfterRemove,
                "calling `removeAllPrereleaseVersions` should not alter the result anymore");


    }

    @Test
    public void getLastHistoryNodeByDateTest() throws IOException, CsafException {

        final ObjectMapper jacksonMapper = new ObjectMapper();
        ArrayNode historyNode = jacksonMapper.createArrayNode();

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonTitle("Title1")),
                "Mustermann", Semantic.name());

        ObjectNode trackingNode = (ObjectNode) advisory.at("/csaf/document/tracking");
        trackingNode.set("revision_history", historyNode);

        ObjectNode hist1 = historyNode.addObject();
        hist1.put("number", "someOldVersion");
        hist1.put("date", "2022-09-10T13:14:15.167Z");
        hist1.put("summary", "some summary");

        ObjectNode hist2 = historyNode.addObject();
        hist2.put("number", "someOldVersion");
        hist2.put("date", "2022-09-12T11:22:33.444Z");
        hist2.put("summary", "some summary");

        ObjectNode hist3 = historyNode.addObject();
        hist3.put("number", "someOldVersion");
        hist3.put("date", "2022-09-08T01:23:45.678Z");
        hist3.put("summary", "some summary");

        String editVersion = "newVersion";
        String editDate = "2022-09-08T01:23:49.999Z";

        advisory.setLastRevisionHistoryElementNumberAndDate(editVersion, editDate);

        assertEquals(editVersion, hist2.at("/number").asText(),
                "history element with most recent date should have been edited");
        assertEquals(editDate, hist2.at("/date").asText(),
                "history element with most recent date should have been edited");

        ObjectNode hist4 = historyNode.addObject();
        hist4.put("number", "someOldVersion");
        hist4.put("date", "2022-09-14T01:02:03.004Z");
        hist4.put("summary", "last summary");

        assertEquals("last summary", advisory.getLastRevisionHistoryElementSummary(),
                "summary of history element with most recent date should have been retrieved");

    }

    @Test
    public void calculateFileNameTest() throws IOException, CsafException {

        assertEquals("exxcellent-2023-00333.json", calculateFileName("eXXcellent-2023-00333"));
        assertEquals("exx_-2023-00333.json", calculateFileName("eXXäöü-2023-00333"));
        assertEquals("red_flag_+_2023.json", calculateFileName("red flag + 2023"));
   }

    @Test
    public void formatNumberTest() throws IOException, CsafException {

        assertEquals("0000790", AdvisoryWrapper.formatNumber("7", 790));
        assertEquals("00790", AdvisoryWrapper.formatNumber("abc", 790));
        assertEquals("7345790", AdvisoryWrapper.formatNumber("5", 7345790));
    }

    @Test
    public void calculatePublishYearTest() throws IOException, CsafException {

        var csafJsonWithReleaseDate = """
                { "document": {
                      "tracking": {
                         "initial_release_date": "2022-05-27T10:00:00.000Z"
                      }
                   }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonWithReleaseDate), "Mustermann", Semantic.name());
        assertEquals(2022, advisory.calculatePublishYear());

        var csafJsonWithoutReleaseDate = """
                { "document": {
                   }
                }""";

        AdvisoryWrapper advisory2 = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonWithoutReleaseDate), "Mustermann", Semantic.name());
        assertEquals(LocalDate.now().getYear(), advisory2.calculatePublishYear());

    }

    @Test
    public void calculateCompanyNameTest() throws IOException, CsafException {

        var csafJsonWithReleaseDate = """
                { "document": {
                      "publisher": {
                         "name": "Red flag company"
                      }
                   }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonWithReleaseDate), "Mustermann", Semantic.name());
        assertEquals("Red", advisory.calculateCompanyName(""));

        assertEquals("ConfiguredCompany", advisory.calculateCompanyName("ConfiguredCompany"));
    }

    @Test
    public void setTemporaryTrackingIdTest() throws IOException, CsafException {

        var csafJsonWithReleaseDate = """
                { "document": {
                      "publisher": {
                         "name": "Red flag company"
                      }
                   }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonWithReleaseDate), "Mustermann", Semantic.name());
        advisory.setTemporaryTrackingId("example", "5", 158L);

        assertEquals("example-TEMP-00158", advisory.getDocumentTrackingId());
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void setFinalTrackingIdTest() throws IOException, CsafException {

        var csafJsonWithReleaseDate = """
                { "document": {
                      "publisher": {
                         "name": "Red flag company"
                      }
                   }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonWithReleaseDate), "Mustermann", Semantic.name());
        advisory.setTemporaryTrackingId("tempExamle", "7", 123L);
        advisory.setFinalTrackingIdAndUrl("https://example.com", "example", "5", 158L);
        long year = ZonedDateTime.now().getYear();
        assertEquals("example-" + year + "-00158", advisory.getDocumentTrackingId());
        assertEquals("https://example.com/WHITE/" + year + "/example-" + year + "-00158.json", advisory.at("/csaf/document/references/0/url").asText());
        assertEquals("URL generated by system", advisory.at("/csaf/document/references/0/summary").asText());
        assertEquals("self", advisory.at("/csaf/document/references/0/category").asText());
        assertEquals("tempExamle-TEMP-0000123", advisory.getTempTrackingIdInFromMeta());
    }

}