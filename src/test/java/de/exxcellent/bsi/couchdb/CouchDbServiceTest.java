package de.exxcellent.bsi.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.cloud.cloudant.v1.model.Document;
import de.exxcellent.bsi.coudb.CouchDbService;
import de.exxcellent.bsi.coudb.DatabaseException;
import de.exxcellent.bsi.json.AdvisoryJsonService;
import de.exxcellent.bsi.model.WorkflowState;
import de.exxcellent.bsi.rest.response.AdvisoryInformationResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test for the CouchDB service. Needs couchDB running to succeed
 */

@SpringBootTest
@Disabled("Needs CouchDb to run")
public class CouchDbServiceTest {

    @Autowired
    private CouchDbService couchDbService;
    private final AdvisoryJsonService jsonService = new AdvisoryJsonService();


    @Test
    public void getServerVersionTest() {

        Assertions.assertEquals("3.2.1", this.couchDbService.getServerVersion());
    }

    @Test
    public void writeNewCsafDocumentToDb() throws IOException {

        long countBefore = this.couchDbService.getDocumentCount();
        final String jsonFileName = "exxcellent-2021AB123.json";
        try (InputStream csafJsonStream = CouchDbServiceTest.class.getResourceAsStream(jsonFileName)) {

            final String owner = "Musterman";
            ObjectNode objectNode = jsonService.convertCsafToJson(csafJsonStream, owner, WorkflowState.Draft);
            final UUID uuid = UUID.randomUUID();
            final String revision = this.couchDbService.writeCsafDocument(uuid, objectNode);
            Assertions.assertNotNull(revision);
        }
        Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
    }

    @Test
    public void updateCsafDocumentToDb() throws IOException {

        long countBefore = this.couchDbService.getDocumentCount();
        final UUID uuid = UUID.randomUUID();
        final String revision;
        try (InputStream csafJsonStream = CouchDbServiceTest.class.getResourceAsStream("exxcellent-2021AB123.json")) {

            final String owner = "Musterman";
            ObjectNode objectNode = jsonService.convertCsafToJson(csafJsonStream, owner, WorkflowState.Draft);
            revision = this.couchDbService.writeCsafDocument(uuid, objectNode);
            Assertions.assertNotNull(revision);
            Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
        }

        try (InputStream csafChangeJsonStream = CouchDbServiceTest.class.getResourceAsStream("exxcellent-2022CC.json")) {
            final String owner = "Musterfrau";
            ObjectNode objectNode = jsonService.convertCsafToJson(csafChangeJsonStream, owner, WorkflowState.Draft);
            this.couchDbService.updateCsafDocument(uuid.toString(), revision, objectNode);
            Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());
        }
        final JsonNode response = this.couchDbService.readCsafDokument(uuid.toString());
        JsonNode changedTrekingId = response.at("/csaf/document/tracking/id");
        Assertions.assertEquals("exxcellent-2022CC", changedTrekingId.asText());

    }

    @Test
    public void deleteCsafDocumentToDb() throws IOException, DatabaseException {

        long countBefore = this.couchDbService.getDocumentCount();
        final UUID uuid= UUID.randomUUID();
        final String revision;
        try (InputStream csafJsonStream = CouchDbServiceTest.class.getResourceAsStream("exxcellent-2021AB123.json")) {

            final String owner = "Musterman";
            ObjectNode objectNode = jsonService.convertCsafToJson(csafJsonStream, owner, WorkflowState.Draft);
            revision = this.couchDbService.writeCsafDocument(uuid, objectNode);
            Assertions.assertNotNull(revision);
            Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());

            this.couchDbService.deleteCsafDokument(uuid.toString(), revision);
        }
    }

    @Test
    public void deleteCsafDocumentToDb_invalidRevision() throws IOException {

        long countBefore = this.couchDbService.getDocumentCount();
        final UUID uuid= UUID.randomUUID();
        final String revision;
        try (InputStream csafJsonStream = CouchDbServiceTest.class.getResourceAsStream("exxcellent-2021AB123.json")) {

            final String owner = "Musterman";
            ObjectNode objectNode = jsonService.convertCsafToJson(csafJsonStream, owner, WorkflowState.Draft);
            revision = this.couchDbService.writeCsafDocument(uuid, objectNode);
            Assertions.assertNotNull(revision);
            Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());

            Assertions.assertThrows(DatabaseException.class
                    , () -> this.couchDbService.deleteCsafDokument(uuid.toString(), "invalid revision"));
        }
    }

    @Test
    public void deleteCsafDocumentToDb_invalidUuid() throws IOException {

        long countBefore = this.couchDbService.getDocumentCount();
        final UUID uuid= UUID.randomUUID();
        final String revision;
        try (InputStream csafJsonStream = CouchDbServiceTest.class.getResourceAsStream("exxcellent-2021AB123.json")) {

            final String owner = "Musterman";
            ObjectNode objectNode = jsonService.convertCsafToJson(csafJsonStream, owner, WorkflowState.Draft);
            revision = this.couchDbService.writeCsafDocument(uuid, objectNode);
            Assertions.assertNotNull(revision);
            Assertions.assertEquals(countBefore + 1, this.couchDbService.getDocumentCount());

            Assertions.assertThrows(DatabaseException.class
                    , () -> this.couchDbService.deleteCsafDokument("invalid user id", revision));
        }
    }

    @Test
    public void readAllCsafDocumentsFromDbTest() {

        final List<AdvisoryInformationResponse> revisions = this.couchDbService.readAllCsafDocuments();
        System.out.println(revisions.size());
        System.out.println(revisions.get(0).getTitle());
    }

    @Test
    public void readCsafDokumentTest() throws IOException {

        final List<AdvisoryInformationResponse> revisions = this.couchDbService.readAllCsafDocuments();

        final JsonNode response = this.couchDbService.readCsafDokument(revisions.get(0).getAdvisoryId());
        System.out.println(response);
    }


    @Test
    public void getStringFieldValueTest() {

        final String[] DOKUMENT_TITLE = {"csaf","document", "title"};
        Document document = new Document.Builder().build();
        Assertions.assertNull( CouchDbService.getStringFieldValue(DOKUMENT_TITLE, document));
        document = new Document.Builder().add("csaf",null).build();
        Assertions.assertNull( CouchDbService.getStringFieldValue(DOKUMENT_TITLE, document));
        document = new Document.Builder().add("csaf",Map.of("document", Map.of("title", "TestTitle"))).build();
        Assertions.assertEquals( CouchDbService.getStringFieldValue(DOKUMENT_TITLE, document), "TestTitle");
    }
}
