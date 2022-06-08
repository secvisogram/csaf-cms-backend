package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.AuditTrailField.*;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.TYPE_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.equal;
import static java.util.Comparator.comparing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.*;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.ObjectType;
import de.bsi.secvisogram.csaf_cms_backend.model.ChangeType;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Test for the Advisory service. The required CouchDB container is started in the CouchDBExtension.
 */
@SpringBootTest
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
public class AdvisoryServiceTest {

    @Autowired
    private AdvisoryService advisoryService;

    private static final String csafJson = "{" +
                                           "    \"document\": {" +
                                           "        \"category\": \"CSAF_BASE\"" +
                                           "    }" +
                                           "}";

    private static final String updatedCsafJson = "{" +
                                                  "    \"document\": {" +
                                                  "        \"category\": \"CSAF_INFORMATIONAL_ADVISORY\"," +
                                                  "         \"title\": \"Test Advisory\"" +
                                                  "    }" +
                                                  "}";

    private static final String advisoryTemplateString = "{" +
                                                         "    \"owner\": \"John Doe\"," +
                                                         "    \"type\": \"Advisory\"," +
                                                         "    \"workflowState\": \"Draft\"," +
                                                         "    \"csaf\": %s" +
                                                         "}";

    private static final String advisoryJsonString = String.format(advisoryTemplateString, csafJson);


    @Test
    public void contextLoads() {
        Assertions.assertNotNull(advisoryService);
    }

    @Test
    public void getAdvisoryCount_empty() {
        assertEquals(0, this.advisoryService.getDocumentCount());
    }

    @Test
    public void getAdvisoryCount() throws IOException {
        this.advisoryService.addAdvisory(csafJson);
        // creates advisory and 1 audit trail
        assertEquals(2, this.advisoryService.getDocumentCount());
    }

    @Test
    public void getAdvisoryIdsTest_empty() {
        List<AdvisoryInformationResponse> ids = this.advisoryService.getAdvisoryInformations();
        assertEquals(0, ids.size());
    }

    @Test
    public void getAdvisoryIdsTest() throws IOException {
        IdAndRevision idRev1 = this.advisoryService.addAdvisory(csafJson);
        IdAndRevision idRev2 = this.advisoryService.addAdvisory(csafJson);
        List<AdvisoryInformationResponse> infos = this.advisoryService.getAdvisoryInformations();
        List<String> expectedIDs = List.of(idRev1.getId(), idRev2.getId());
        List<String> ids = infos.stream().map(AdvisoryInformationResponse::getAdvisoryId).toList();
        Assertions.assertTrue(ids.size() == expectedIDs.size()
                              && ids.containsAll(expectedIDs)
                              && expectedIDs.containsAll(ids));
    }

    @Test
    public void addAdvisoryTest_invalidJson() {
        String invalidJson = "no json string";

        Assertions.assertThrows(JsonProcessingException.class, () -> this.advisoryService.addAdvisory(invalidJson));
    }

    @Test
    public void addAdvisoryTest_invalidAdvisory() {
        String invalidAdvisory = "{\"no\": \"CSAF document\"}";

        Assertions.assertThrows(IllegalArgumentException.class, () -> this.advisoryService.addAdvisory(invalidAdvisory));
    }

    @Test
    public void addAdvisoryTest() throws IOException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafJson);
        Assertions.assertNotNull(idRev);
    }

    @Test
    public void getAdvisoryTest_notPresent() {
        UUID noAdvisoryId = UUID.randomUUID();
        Assertions.assertThrows(IdNotFoundException.class, () -> advisoryService.getAdvisory(noAdvisoryId.toString()));
    }

    @Test
    public void getAdvisoryTest() throws IOException, DatabaseException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafJson);
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());
        assertEquals(csafJson.replaceAll("\\s+", ""), advisory.getCsaf().toString().replaceAll("\\s+", ""));
        assertEquals(idRev.getId(), advisory.getAdvisoryId());
    }

    @Test
    public void deleteAdvisoryTest_notPresent() {
        UUID noAdvisoryId = UUID.randomUUID();
        Assertions.assertThrows(IdNotFoundException.class, () -> advisoryService.deleteAdvisory(noAdvisoryId.toString(), "redundant-revision"));
    }

    @Test
    public void deleteAdvisoryTest_badRevision() throws IOException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafJson);
        // creates advisory and 1 audit trail
        assertEquals(2, advisoryService.getDocumentCount());
        String revision = "bad revision";
        Assertions.assertThrows(DatabaseException.class, () -> this.advisoryService.deleteAdvisory(idRev.getId(), revision));
    }

    @Test
    public void deleteAdvisoryTest() throws IOException, DatabaseException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafJson);
        advisoryService.addAdvisory(csafJson);
        // creates advisory and audit trail
        assertEquals(4, advisoryService.getDocumentCount());
        this.advisoryService.deleteAdvisory(idRev.getId(), idRev.getRevision());
        assertEquals(2, advisoryService.getDocumentCount());
    }

    @Test
    public void   updateAdvisoryTest() throws IOException, DatabaseException {

        var updateJsafJson = csafDocumentJson("CSAF_INFORMATIONAL_ADVISORY", "Test Advisory");

        IdAndRevision idRev = advisoryService.addAdvisory(csafJson);
        advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), updateJsafJson);
        // an advisory and 2 audit trails are created
        assertEquals(3, advisoryService.getDocumentCount());
        AdvisoryResponse updatedAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertEquals(updatedCsafJson.replaceAll("\\s+", ""),
                updatedAdvisory.getCsaf().toString().replaceAll("\\s+", ""));
    }

    @Test
    public void   updateAdvisoryTest_auditTrail() throws IOException, DatabaseException {

        var idRev = advisoryService.addAdvisory(csafDocumentJson("Category1", "Title1"));
        var revision = advisoryService.updateAdvisory(idRev.getId(), idRev.getRevision(), csafDocumentJson("Category2", "Title2"));
        revision = advisoryService.updateAdvisory(idRev.getId(), revision, csafDocumentJson("Category3", "Title3"));
        advisoryService.updateAdvisory(idRev.getId(), revision, csafDocumentJson("Category4", "Title4"));
        // an advisory and 4 audit trail are created
        assertEquals(5, advisoryService.getDocumentCount());
        AdvisoryResponse updatedAdvisory = advisoryService.getAdvisory(idRev.getId());
        assertEquals(csafDocumentJson("Category4", "Title4").replaceAll("\\s+", ""),
                updatedAdvisory.getCsaf().toString().replaceAll("\\s+", ""));

        List<JsonNode> auditTrails = readAllAuditTrailDocumentsFromDb();

        assertEquals(4, auditTrails.size());
        auditTrails.sort(comparing(CREATED_AT::stringVal));
        assertThat(CHANGE_TYPE.stringVal(auditTrails.get(0)), equalTo(ChangeType.Create.name()));
        assertThat(CHANGE_TYPE.stringVal(auditTrails.get(1)), equalTo(ChangeType.Update.name()));
        // recreate Advisory from diffs
        AdvisoryWrapper rootWrapper = AdvisoryWrapper.createNewFromCsaf(AdvisoryWrapper.emptyCsafDocument, "");
        JsonNode patch0 = auditTrails.get(0).get(DIFF.getDbName());
        AdvisoryWrapper node1 = rootWrapper.applyJsonPatch(patch0);
        assertThat(node1.at(AdvisorySearchField.DOCUMENT_TITLE).asText(), equalTo("Title1"));
        AdvisoryWrapper node2 = node1.applyJsonPatch(auditTrails.get(1).get(DIFF.getDbName()));
        assertThat(node2.at(AdvisorySearchField.DOCUMENT_TITLE).asText(), equalTo("Title2"));
        AdvisoryWrapper node3 = node2.applyJsonPatch(auditTrails.get(2).get(DIFF.getDbName()));
        assertThat(node3.at(AdvisorySearchField.DOCUMENT_TITLE).asText(), equalTo("Title3"));
        AdvisoryWrapper node4 = node2.applyJsonPatch(auditTrails.get(3).get(DIFF.getDbName()));
        assertThat(node4.at(AdvisorySearchField.DOCUMENT_TITLE).asText(), equalTo("Title4"));
    }

    private List<JsonNode> readAllAuditTrailDocumentsFromDb() throws IOException {

        Collection<DbField> fields = Arrays.asList(CouchDbField.ID_FIELD, AuditTrailField.ADVISORY_ID, CREATED_AT,
                CHANGE_TYPE, DIFF, AuditTrailField.DOC_VERSION);
        Map<String, Object> selector = expr2CouchDBFilter(equal(ObjectType.AuditTrailDocument.name(), TYPE_FIELD.getDbName()));
        return advisoryService.findDocuments(selector, fields);
    }

    @Test
    public void updateAdvisoryTest_badData() {
        UUID noAdvisoryId = UUID.randomUUID();
        Assertions.assertThrows(DatabaseException.class, () -> advisoryService.updateAdvisory(noAdvisoryId.toString(), "redundant", advisoryJsonString));
    }

    @Test
    public void changeAdvisoryWorkflowStateTest() throws IOException, DatabaseException {
        IdAndRevision idRev = advisoryService.addAdvisory(csafJson);
        advisoryService.changeAdvisoryWorkflowState(idRev.getId(), idRev.getRevision(), WorkflowState.Review);
        // an advisory and 2 audit trails are created
        assertEquals(3, advisoryService.getDocumentCount());
        AdvisoryResponse advisory = advisoryService.getAdvisory(idRev.getId());
        assertEquals(WorkflowState.Review, advisory.getWorkflowState());
    }

    private String csafDocumentJson(String documentCategory, String documentTitle) {

        return """
                { "document": {
                      "category": "%s",
                      "title":"%s"
                   }
                }""".formatted(documentCategory, documentTitle);
    }
}
