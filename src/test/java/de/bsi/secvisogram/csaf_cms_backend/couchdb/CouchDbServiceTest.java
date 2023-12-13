package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelArray.ENTRY_VALUE;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelFirstLevel.SECOND_LEVEL;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelRoot.*;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelSecondLevel.LEVEL_2_VALUE;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.*;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.internal.LazilyParsedNumber;
import com.ibm.cloud.cloudant.v1.model.Document;
import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelField;
import de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelRoot;
import de.bsi.secvisogram.csaf_cms_backend.json.ObjectType;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.AndExpression;
import de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression;
import de.bsi.secvisogram.csaf_cms_backend.service.IdAndRevision;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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

    private static final String[] ARRAY_FIELD_SELECTOR = {ARRAY_VALUES};

    private static final List<DbField> ROOT_PRIMITIVE_FIELDS = Arrays.asList(TestModelField.FIRST_STRING, TestModelField.SECOND_STRING,
            TestModelField.DECIMAL_VALUE, TestModelField.BOOLEAN_VALUE);

    @Autowired
    private CouchDbService couchDbService;

    @Test
    public void getServerVersionTest() {
        Assertions.assertEquals(CouchDBExtension.couchDbVersion, this.couchDbService.getServerVersion());
    }

    
    @Test
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "document count should increase")
    public void writeDocumentTest() throws IOException {

        long countBefore = this.couchDbService.getDocumentCount();

        UUID uuid = UUID.randomUUID();
        String revision = insertTestDocument(uuid);

        Assertions.assertNotNull(revision);

        Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
    }

    @Test
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "document count should increase")
    public void updateDocumentTest() throws DatabaseException {

        long countBefore = this.couchDbService.getDocumentCount();

        UUID uuid = UUID.randomUUID();
        var initialDoc = """
                {
                    "testKey": "TestValue"
                }
                """;
        var revision = this.couchDbService.writeDocument(uuid, initialDoc);
        Assertions.assertNotNull(revision);
        Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
        var updateDoc = """
                {   "_rev": "%s",
                    "_id": "%s",
                    "testKey": "ChangeValue"
                }
                """.formatted(revision, uuid.toString());
        revision = this.couchDbService.updateDocument(updateDoc);

        Assertions.assertNotNull(revision);
        Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
    }

    @Test
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "document count should increase")
    public void updateDocumentTest_invalidRevision() {

        long countBefore = this.couchDbService.getDocumentCount();

        UUID uuid = UUID.randomUUID();
        var initialDoc = """
                {
                    "testKey": "TestValue"
                }
                """;
        var revision = this.couchDbService.writeDocument(uuid, initialDoc);
        Assertions.assertNotNull(revision);
        Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
        var updateDoc = """
                {   "_rev": "%s",
                    "_id": "%s",
                    "testKey": "ChangeValue"
                }
                """.formatted("Invalid Revison", uuid.toString());
        assertThrows(DatabaseException.class,
                () -> this.couchDbService.updateDocument(updateDoc));
    }

    @Test
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "document should not change")
    public void deleteDocumentTest() throws IOException, DatabaseException {

        long countBefore = this.couchDbService.getDocumentCount();

        final UUID uuid = UUID.randomUUID();
        String revision = insertTestDocument(uuid);

        Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
        this.couchDbService.deleteDocument(uuid.toString(), revision);

        Assertions.assertEquals(countBefore, this.couchDbService.getDocumentCount());
    }

    @Test
    public void deleteCsafDocumentFromDb_invalidRevision() throws IOException {

        final UUID uuid = UUID.randomUUID();
        insertTestDocument(uuid);

        assertThrows(DatabaseException.class,
                () -> this.couchDbService.deleteDocument(uuid.toString(), "invalid revision"));
    }

    @Test
    public void deleteCsafDocumentFromDb_doesNotExist() throws IOException {

        final UUID uuid = UUID.randomUUID();
        final String revision = insertTestDocument(uuid);

        assertThrows(IdNotFoundException.class,
                () -> this.couchDbService.deleteDocument("idDoesNotExist", revision));
    }

    @Test
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "document should not change")
    public void bulkDeleteDocuments() throws IOException, DatabaseException {

        long countBefore = this.couchDbService.getDocumentCount();

        final UUID uuid1 = UUID.randomUUID();
        String revision1 = insertTestDocument(uuid1);
        final UUID uuid2 = UUID.randomUUID();
        String revision2 = insertTestDocument(uuid2);

        Assertions.assertEquals(countBefore + 2, this.couchDbService.getDocumentCount());
        this.couchDbService.bulkDeleteDocuments(Arrays.asList(new IdAndRevision(uuid1.toString(), revision1),
                new IdAndRevision(uuid2.toString(), revision2)));

        Assertions.assertEquals(countBefore, this.couchDbService.getDocumentCount());
    }

    @Test
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "document should not change")
    public void bulkDeleteDocumentsTest_invalidRevision() throws IOException {

        long countBefore = this.couchDbService.getDocumentCount();

        final UUID uuid1 = UUID.randomUUID();
        String revision1 = insertTestDocument(uuid1);
        final UUID uuid2 = UUID.randomUUID();
        insertTestDocument(uuid2);

        Assertions.assertEquals(countBefore + 2, this.couchDbService.getDocumentCount());
        assertThrows(DatabaseException.class,
                () -> this.couchDbService.bulkDeleteDocuments(Arrays.asList(
                        new IdAndRevision(uuid1.toString(), revision1),
                        new IdAndRevision(uuid2.toString(), "Invalid Revision"))));

        Assertions.assertEquals(countBefore + 2, this.couchDbService.getDocumentCount());
    }

    @Test
    public void readAllDocumentsTest() throws IOException {

        UUID advisoryId = UUID.randomUUID();
        insertTestDocument(advisoryId);

        long docCount = this.couchDbService.getDocumentCount();

        List<DbField> infoFields = List.of(
                AdvisorySearchField.DOCUMENT_TITLE,
                AdvisorySearchField.DOCUMENT_TRACKING_ID,
                CouchDbField.REVISION_FIELD,
                CouchDbField.ID_FIELD
        );

        final List<Document> docs = this.couchDbService.readAllDocuments(ObjectType.Advisory, infoFields);
        Assertions.assertEquals(docCount, docs.size());
        Assertions.assertEquals(advisoryId.toString(), docs.get(0).getId());
    }

    private String insertTestDocument(UUID documentUuid) throws IOException {
        String owner = "Mustermann";
        String jsonFileName = "exxcellent-2021AB123.json";
        try (InputStream csafJsonStream = CouchDbServiceTest.class.getResourceAsStream(jsonFileName)) {
            ObjectNode objectNode = toAdvisoryJson(csafJsonStream, owner);
            return this.couchDbService.writeDocument(documentUuid, objectNode.toString());
        }
    }

    /**
     * Test find with Native Cloudant selector
     * @throws IOException unexpected exception
     */
    @Test
    public void findDocumentsTest_native() throws IOException {

        this.writeToDb(new TestModelRoot().setFirstString("Hans").setSecondString("Dampf").setDecimalValue(12.55));
        this.writeToDb(new TestModelRoot().setFirstString("Franz").setSecondString("Dampf"));

        List<Document> foundDocs = this.couchDbService.findDocuments(Map.of(SECOND_STRING, "Dampf"),
                ROOT_PRIMITIVE_FIELDS);

        assertThat(foundDocs.size(), equalTo(2));

        foundDocs = this.couchDbService.findDocuments(Map.of(FIRST_STRING, "Hans"),
                ROOT_PRIMITIVE_FIELDS);
        assertThat(foundDocs.size(), equalTo(1));
    }

    @Test
    public void findDocumentsTest_operatorEqual() throws IOException {

        this.writeToDb(new TestModelRoot().setFirstString("zzz").setSecondString("AAA").setDecimalValue(12.55));
        this.writeToDb(new TestModelRoot().setFirstString("yyy").setSecondString("AAA"));
        this.writeToDb(new TestModelRoot().setFirstString("xxx").setSecondString("BBB"));

        Map<String, Object> filter = expr2CouchDBFilter(equal("AAA", SECOND_STRING));
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, FIRST_STRING), containsInAnyOrder("yyy", "zzz"));

        Map<String, Object> filterNe = expr2CouchDBFilter(notEqual("yyy", FIRST_STRING));
        foundDocs = this.couchDbService.findDocuments(filterNe, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, FIRST_STRING), containsInAnyOrder("xxx", "zzz"));
    }

    @Test
    public void findDocumentsTest_operatorContainsIgnoreCase() throws IOException {

        this.writeToDb(new TestModelRoot().setFirstString("123Abc45"));
        this.writeToDb(new TestModelRoot().setFirstString("123abC45"));
        this.writeToDb(new TestModelRoot().setFirstString("123abD45"));

        Map<String, Object> filter = expr2CouchDBFilter(containsIgnoreCase("3abc4", FIRST_STRING));
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, FIRST_STRING), containsInAnyOrder("123Abc45", "123abC45"));
    }

    @Test
    public void findDocumentsTest_operatorsGreaterAndLess() throws IOException {

        this.writeToDb(new TestModelRoot().setFirstString("AAA"));
        this.writeToDb(new TestModelRoot().setFirstString("BBB"));
        this.writeToDb(new TestModelRoot().setFirstString("CCC"));

        OperatorExpression gteExpr = greaterOrEqual("BBB", FIRST_STRING);
        List<Document> foundDocs = this.couchDbService.findDocuments(expr2CouchDBFilter(gteExpr), ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, FIRST_STRING), containsInAnyOrder("BBB", "CCC"));

        OperatorExpression gtExpr = greater("BBB", FIRST_STRING);
        foundDocs = this.couchDbService.findDocuments(expr2CouchDBFilter(gtExpr), ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, FIRST_STRING), containsInAnyOrder("CCC"));

        OperatorExpression lteExpr = lessOrEqual("BBB", FIRST_STRING);
        foundDocs = this.couchDbService.findDocuments(expr2CouchDBFilter(lteExpr), ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, FIRST_STRING), containsInAnyOrder("AAA", "BBB"));

        OperatorExpression ltExpr = less("BBB", FIRST_STRING);
        foundDocs = this.couchDbService.findDocuments(expr2CouchDBFilter(ltExpr), ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, FIRST_STRING), containsInAnyOrder("AAA"));
    }

     @Test
    public void findDocumentsTest_numericValue() throws IOException {

        this.writeToDb(new TestModelRoot().setDecimalValue(12.55));
        this.writeToDb(new TestModelRoot().setDecimalValue(2374.332));

        Map<String, Object> filter = expr2CouchDBFilter(equal(12.55, DECIMAL_VALUE));
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttributeDouble(foundDocs, DECIMAL_VALUE), containsInAnyOrder(12.55));
    }

    @Test
    public void findDocumentsTest_booleanValue() throws IOException {

        this.writeToDb(new TestModelRoot().setBooleanValue(Boolean.TRUE));
        this.writeToDb(new TestModelRoot().setBooleanValue(Boolean.FALSE));

        Map<String, Object> filter = expr2CouchDBFilter(equal(true, BOOLEAN_VALUE));
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, BOOLEAN_VALUE), containsInAnyOrder(Boolean.TRUE));
    }

    @Test
    public void findDocumentsTest_operatorAnd() throws IOException {

        this.writeToDb(new TestModelRoot().setFirstString("zzz").setSecondString("AAA").setDecimalValue(11.11));
        this.writeToDb(new TestModelRoot().setFirstString("zzz").setSecondString("AAA").setDecimalValue(22.22));
        this.writeToDb(new TestModelRoot().setFirstString("zzz").setSecondString("BBB").setDecimalValue(11.11));
        this.writeToDb(new TestModelRoot().setFirstString("zzz").setSecondString("BBB").setDecimalValue(22.22));
        this.writeToDb(new TestModelRoot().setFirstString("xxx").setSecondString("AAA").setDecimalValue(11.11));
        this.writeToDb(new TestModelRoot().setFirstString("xxx").setSecondString("AAA").setDecimalValue(22.22));
        this.writeToDb(new TestModelRoot().setFirstString("xxx").setSecondString("BBB").setDecimalValue(11.11));
        this.writeToDb(new TestModelRoot().setFirstString("xxx").setSecondString("BBB").setDecimalValue(22.22));

        AndExpression andExpr = new AndExpression(equal("xxx", FIRST_STRING),
                equal("AAA", SECOND_STRING), equal(22.22, DECIMAL_VALUE));
        Map<String, Object> filter = expr2CouchDBFilter(andExpr);
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(foundDocs.size(), equalTo(1));
        assertThat(foundDocs.get(0).get(FIRST_STRING), equalTo("xxx"));
        assertThat(foundDocs.get(0).get(SECOND_STRING), equalTo("AAA"));
        assertThat(((LazilyParsedNumber) foundDocs.get(0).get(DECIMAL_VALUE)).doubleValue(), equalTo(22.22));
    }

    @Test
    public void findDocumentsTest_multLevelSelector() throws IOException {

        this.writeToDb(new TestModelRoot().setFirstString("AAA").setLevelValues("Lev1A", "Lev2A"));
        this.writeToDb(new TestModelRoot().setFirstString("BBB").setLevelValues("Lev1B", "Lev2B"));
        this.writeToDb(new TestModelRoot().setFirstString("CCC").setLevelValues("Lev1C", "Lev2A"));
        this.writeToDb(new TestModelRoot().setFirstString("DDD").setLevelValues("Lev1D", "Lev2B"));

        Map<String, Object> filter = expr2CouchDBFilter(equal("Lev2B", FIRST_LEVEL, SECOND_LEVEL, LEVEL_2_VALUE));
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, FIRST_STRING), containsInAnyOrder("BBB", "DDD"));
    }

    @Test
    public void findDocumentsTest_searchInArray() throws IOException {

        this.writeToDb(new TestModelRoot().setFirstString("AAA").addListValues("ListVal1", "ListVal2", "ListVal3"));
        this.writeToDb(new TestModelRoot().setFirstString("BBB").addListValues("ListVal1", "ListVal5", "ListVal6"));
        this.writeToDb(new TestModelRoot().setFirstString("CCC").addListValues("ListVal7", "ListVal8", "ListVal2"));
        this.writeToDb(new TestModelRoot().setFirstString("DDD").addListValues("ListVal9", "ListVal3", "ListVal20"));

        Map<String, Object> filter = expr2CouchDBFilter(equal("ListVal2", ARRAY_VALUES, ENTRY_VALUE),
            ARRAY_FIELD_SELECTOR);
        List<Document> foundDocs = this.couchDbService.findDocuments(filter, ROOT_PRIMITIVE_FIELDS);
        assertThat(mapAttribute(foundDocs, FIRST_STRING), containsInAnyOrder("AAA", "CCC"));
    }


    private List<Object> mapAttribute(Collection<Document> foundDocs, String attributeName) {
        return foundDocs.stream()
                .map(doc -> doc.get(attributeName))
                .collect(toList());
    }

    private List<Object> mapAttributeDouble(Collection<Document> foundDocs, String attributeName) {
        return foundDocs.stream()
                .map(doc -> ((LazilyParsedNumber) doc.get(attributeName)).doubleValue())
                .collect(toList());
    }



    private ObjectNode toAdvisoryJson(InputStream csafJsonStream, String owner) throws IOException {

        ObjectMapper jacksonMapper = new ObjectMapper();

        JsonNode csafRootNode = jacksonMapper.readValue(csafJsonStream, JsonNode.class);

        ObjectNode rootNode = jacksonMapper.createObjectNode();
        rootNode.put("workflowState", "Draft");
        rootNode.put("owner", owner);
        rootNode.put("type", ObjectType.Advisory.name());
        rootNode.set("csaf", csafRootNode);

        return rootNode;
    }

    public void writeToDb(Object objectToWrite) throws JsonProcessingException {
      final ObjectMapper jacksonMapper = new ObjectMapper();
      ObjectWriter writer = jacksonMapper.writer(new DefaultPrettyPrinter());
      String createString = writer.writeValueAsString(objectToWrite);
      this.couchDbService.writeDocument(UUID.randomUUID(), createString);
    }
}
