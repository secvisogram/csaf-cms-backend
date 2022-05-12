package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelRoot.ROOT_PRIMITIVE_FIELDS;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.*;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.internal.LazilyParsedNumber;
import com.ibm.cloud.cloudant.v1.model.Document;
import de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelRoot;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryJsonService;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Test for the CouchDB service. The required CouchDB container is started in the CouchDBExtension.
 */
@SpringBootTest
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
public class CouchDbServiceTest {

    private final String[] DOCUMENT_TITLE = {"csaf", "document", "title"};
    private static final String[] DOCUMENT_TRACKING_ID = {"csaf", "document", "tracking", "id"};

    @Autowired
    private CouchDbService couchDbService;

    @Test
    public void getServerVersionTest() {

        Assertions.assertEquals(CouchDBExtension.couchDbVersion, this.couchDbService.getServerVersion());
    }

    @Test
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "document count should increase")
    public void writeNewCsafDocumentToDb() throws IOException {

        long countBefore = this.couchDbService.getDocumentCount();

        UUID uuid = UUID.randomUUID();
        String revision = insertTestDocument(uuid);

        Assertions.assertNotNull(revision);

        Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
    }

    @Test
    public void updateCsafDocumentToDb() throws IOException, DatabaseException {

        final UUID uuid = UUID.randomUUID();
        insertTestDocument(uuid);

        long countBeforeUpdate = this.couchDbService.getDocumentCount();
        final Document responseBeforeUpdate = this.couchDbService.readCsafDocument(uuid.toString());
        String trackingIdBeforeUpdate = CouchDbService.getStringFieldValue(DOCUMENT_TRACKING_ID, responseBeforeUpdate);
        Assertions.assertEquals("exxcellent-2021AB123", trackingIdBeforeUpdate);
        String revision = responseBeforeUpdate.getRev();

        String newOwner = "Musterfrau";
        try (InputStream csafJsonStream = CouchDbServiceTest.class.getResourceAsStream("exxcellent-2022CC.json")) {
            ObjectNode objectNode = toAdvisoryJson(csafJsonStream, newOwner);
            this.couchDbService.updateCsafDocument(uuid.toString(), revision, objectNode);
        }

        long countAfterUpdate = this.couchDbService.getDocumentCount();
        final Document responseAfterUpdate = this.couchDbService.readCsafDocument(uuid.toString());
        Assertions.assertEquals(countBeforeUpdate, countAfterUpdate);

        String trackingIdAfterUpdate = CouchDbService.getStringFieldValue(DOCUMENT_TRACKING_ID, responseAfterUpdate);
        Assertions.assertEquals("exxcellent-2022CC", trackingIdAfterUpdate);
        Assertions.assertEquals(uuid.toString(), responseAfterUpdate.getId());

    }

    @Test
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "document should not change")
    public void deleteCsafDocumentFromDb() throws IOException, DatabaseException {

        long countBefore = this.couchDbService.getDocumentCount();

        final UUID uuid = UUID.randomUUID();
        String revision = insertTestDocument(uuid);

        Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
        this.couchDbService.deleteCsafDocument(uuid.toString(), revision);

        Assertions.assertEquals(countBefore, this.couchDbService.getDocumentCount());
    }

    @Test
    public void deleteCsafDocumentFromDb_invalidRevision() throws IOException {

        final UUID uuid = UUID.randomUUID();
        insertTestDocument(uuid);

        Assertions.assertThrows(DatabaseException.class,
                () -> this.couchDbService.deleteCsafDocument(uuid.toString(), "invalid revision"));
    }

    @Test
    public void deleteCsafDocumentFromDb_invalidUuid() throws IOException {

        final UUID uuid = UUID.randomUUID();
        final String revision = insertTestDocument(uuid);

        Assertions.assertThrows(DatabaseException.class,
                () -> this.couchDbService.deleteCsafDocument("invalid user id", revision));
    }

    @Test
    public void readAllCsafDocumentsFromDbTest() throws IOException {

        UUID advisoryId = UUID.randomUUID();
        insertTestDocument(advisoryId);

        long docCount = this.couchDbService.getDocumentCount();

        final List<Document> docs = this.couchDbService.readAllCsafDocuments();
        Assertions.assertEquals(docCount, docs.size());
        Assertions.assertEquals(advisoryId.toString(), docs.get(0).getId());
    }

    @Test
    public void readCsafDocumentTest() throws IOException, IdNotFoundException {

        final UUID uuid = UUID.randomUUID();
        insertTestDocument(uuid);

        final Document response = this.couchDbService.readCsafDocument(uuid.toString());
        Assertions.assertEquals("TestRSc", CouchDbService.getStringFieldValue(DOCUMENT_TITLE, response));
        Assertions.assertEquals(uuid.toString(), response.getId());
    }


    private String insertTestDocument(UUID documentUuid) throws IOException {
        String owner = "Mustermann";
        String jsonFileName = "exxcellent-2021AB123.json";
        try (InputStream csafJsonStream = CouchDbServiceTest.class.getResourceAsStream(jsonFileName)) {
            ObjectNode objectNode = toAdvisoryJson(csafJsonStream, owner);
            return this.couchDbService.writeCsafDocument(documentUuid, objectNode);
        }
    }

    @Test
    public void getStringFieldValueTest() {

        Document document = new Document.Builder().build();
        Assertions.assertNull(CouchDbService.getStringFieldValue(DOCUMENT_TITLE, document));
        document = new Document.Builder().add("csaf", null).build();
        Assertions.assertNull(CouchDbService.getStringFieldValue(DOCUMENT_TITLE, document));
        document = new Document.Builder().add("csaf", Map.of("document", Map.of("title", "TestTitle"))).build();
        Assertions.assertEquals(CouchDbService.getStringFieldValue(DOCUMENT_TITLE, document), "TestTitle");
    }

    /**
     * Test find with Native Cloudant selecto
     * @throws IOException unexpected exception
     */
    @Test
    public void findDocumentsTest_native() throws IOException {

        this.couchDbService.deleteDatabase(this.couchDbService.getDbName());
        this.couchDbService.createDatabase(this.couchDbService.getDbName());

        final TestModelRoot node1 = new TestModelRoot().setFirstString("Hans").setSecondString("Dampf").setDecimalValue(12.55);
        this.couchDbService.writeDocument(UUID.randomUUID(), node1);

        final TestModelRoot node2 = new TestModelRoot().setFirstString("Franz").setSecondString("Dampf");
        this.couchDbService.writeDocument(UUID.randomUUID(), node2);

        List<Document> foundDocs = this.couchDbService.findDocuments(Map.of("secondString", "Dampf"),
                ROOT_PRIMITIVE_FIELDS);

        assertThat(foundDocs.size(), equalTo(2));

        foundDocs = this.couchDbService.findDocuments(Map.of("firstString", "Hans"),
                ROOT_PRIMITIVE_FIELDS);
        assertThat(foundDocs.size(), equalTo(1));
    }

    @Test
    public void findDocumentsTest_operatorEqual() throws IOException {

        this.couchDbService.deleteDatabase(this.couchDbService.getDbName());
        this.couchDbService.createDatabase(this.couchDbService.getDbName());

        final TestModelRoot node1 = new TestModelRoot().setFirstString("zzz").setSecondString("AAA").setDecimalValue(12.55);
        this.couchDbService.writeDocument(UUID.randomUUID(), node1);

        final TestModelRoot node2 = new TestModelRoot().setFirstString("yyy").setSecondString("AAA");
        this.couchDbService.writeDocument(UUID.randomUUID(), node2);

        final TestModelRoot node3 = new TestModelRoot().setFirstString("xxx").setSecondString("BBB");
        this.couchDbService.writeDocument(UUID.randomUUID(), node3);

        Map<String, Object> filter = expr2CouchDBFilter(equal("AAA","secondString"));
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, "firstString"), containsInAnyOrder("yyy","zzz"));

        Map<String, Object> filterNe = expr2CouchDBFilter(notEqual("yyy","firstString"));
        foundDocs = this.couchDbService.findDocuments(filterNe, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, "firstString"), containsInAnyOrder("xxx","zzz"));
    }

    @Test
    public void findDocumentsTest_operatorsGreaterAndLess() throws IOException {

        this.couchDbService.deleteDatabase(this.couchDbService.getDbName());
        this.couchDbService.createDatabase(this.couchDbService.getDbName());

        this.couchDbService.writeDocument(UUID.randomUUID(), new TestModelRoot().setFirstString("AAA"));
        this.couchDbService.writeDocument(UUID.randomUUID(), new TestModelRoot().setFirstString("BBB"));
        this.couchDbService.writeDocument(UUID.randomUUID(), new TestModelRoot().setFirstString("CCC"));

        OperatorExpression gteExpr = greaterOrEqual("BBB","firstString");
        List<Document> foundDocs = this.couchDbService.findDocuments(expr2CouchDBFilter(gteExpr), ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, "firstString"), containsInAnyOrder("BBB", "CCC"));

        OperatorExpression gtExpr = greater("BBB","firstString");
        foundDocs = this.couchDbService.findDocuments(expr2CouchDBFilter(gtExpr), ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, "firstString"), containsInAnyOrder("CCC"));

        OperatorExpression lteExpr = lessOrEqual("BBB","firstString");
        foundDocs = this.couchDbService.findDocuments(expr2CouchDBFilter(lteExpr), ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, "firstString"), containsInAnyOrder("AAA", "BBB"));

        OperatorExpression ltExpr = less("BBB","firstString");
        foundDocs = this.couchDbService.findDocuments(expr2CouchDBFilter(ltExpr), ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, "firstString"), containsInAnyOrder("AAA"));
    }

     @Test
    public void findDocumentsTest_numericValue() throws IOException {

        this.couchDbService.deleteDatabase(this.couchDbService.getDbName());
        this.couchDbService.createDatabase(this.couchDbService.getDbName());

        final TestModelRoot node1 = new TestModelRoot().setDecimalValue(12.55);
        this.couchDbService.writeDocument(UUID.randomUUID(), node1);

        final TestModelRoot node2 = new TestModelRoot().setDecimalValue(2374.332);
        this.couchDbService.writeDocument(UUID.randomUUID(), node2);

        Map<String, Object> filter = expr2CouchDBFilter(equal(12.55,"decimalValue"));
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttributeDouble(foundDocs, "decimalValue"), containsInAnyOrder(12.55));
    }

    @Test
    public void findDocumentsTest_booleanValue() throws IOException {

        this.couchDbService.deleteDatabase(this.couchDbService.getDbName());
        this.couchDbService.createDatabase(this.couchDbService.getDbName());

        final TestModelRoot node1 = new TestModelRoot().setBooleanValue(true);
        this.couchDbService.writeDocument(UUID.randomUUID(), node1);

        final TestModelRoot node2 = new TestModelRoot().setBooleanValue(false);
        this.couchDbService.writeDocument(UUID.randomUUID(), node2);

        Map<String, Object> filter = expr2CouchDBFilter(equal(true,"booleanValue"));
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, "booleanValue"), containsInAnyOrder(true));
    }

    private List<Object> mapAttribute(List<Document> foundDocs, String attributeName) {
        return foundDocs.stream()
                .map(doc -> doc.get(attributeName))
                .collect(toList());
    }

    private List<Object> mapAttributeDouble(List<Document> foundDocs, String attributeName) {
        return foundDocs.stream()
                .map(doc -> ((LazilyParsedNumber)doc.get(attributeName)).doubleValue())
                .collect(toList());
    }



    private ObjectNode toAdvisoryJson(InputStream csafJsonStream, String owner) throws IOException {

        ObjectMapper jacksonMapper = new ObjectMapper();

        JsonNode csafRootNode = jacksonMapper.readValue(csafJsonStream, JsonNode.class);

        ObjectNode rootNode = jacksonMapper.createObjectNode();
        rootNode.put("workflowState", "Draft");
        rootNode.put("owner", owner);
        rootNode.put("type", CouchDbService.ObjectType.Advisory.name());
        rootNode.set("csaf", csafRootNode);

        return rootNode;
    }

}
