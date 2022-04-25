package de.bsi.secvisogram.csaf_cms_backend.coudb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.*;
import com.ibm.cloud.sdk.core.security.BasicAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryJsonService;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryJsonService.*;

/**
 * Service to create, update and delete objects in a couchDB
 */
@Service
public class CouchDbService {

    private static final Logger LOG = LoggerFactory.getLogger(CouchDbService.class);
    private static final String CLOUDANT_SERVICE_NAME = "SECVISOGRAM";

    private static final String[] DOCUMENT_TITLE = {"csaf","document", "title"};
    private static final String[] DOCUMENT_TRACKING_ID = {"csaf","document","tracking", "id"};

    @Value("${csaf.couchdb.dbname}")
    private String dbName;

    @Value("${csaf.couchdb.host}")
    private String dbHost;

    @Value("${csaf.couchdb.ssl}")
    private Boolean dbSsl;

    @Value("${csaf.couchdb.port}")
    private int dbPort;

    @Value("${csaf.couchdb.user}")
    private String dbUser;

    @Value("${csaf.couchdb.password}")
    private String dbPassword;

    private final ObjectMapper jacksonMapper = new ObjectMapper();

    /**
     * Get the CouchDB connection string
     * @return CouchDB connection string
     */
    private String getDbUrl() {
        String protocol = this.dbSsl ? "https://" : "http://";
        return protocol + dbHost + ":" + dbPort;
    }

    /**
     * Create a new CouchDB
     * @param nameOfNewDb name of the couchdb database
     */
    public void createDatabase(String nameOfNewDb) {

        Cloudant client = createCloudantClient();
        PutDatabaseOptions putDbOptions =
                new PutDatabaseOptions.Builder().db(nameOfNewDb).build();

        // Try to create database if it doesn't exist
        try {
            Ok putDatabaseResult = client
                    .putDatabase(putDbOptions)
                    .execute()
                    .getResult();

            if (putDatabaseResult.isOk()) {
                LOG.info( "{}' database created.", nameOfNewDb);
            }
        } catch (ServiceResponseException sre) {
            if (sre.getStatusCode() == 412) {
                throw new RuntimeException("Cannot create \"" + nameOfNewDb + "\" database, it already exists.", sre);
            }
        }
    }

    /**
     * Get the Version of the couchdb server
     * @return server version
     */
    public String getServerVersion() {

        Cloudant client = createCloudantClient();
        ServerInformation serverInformation = client
                .getServerInformation()
                .execute()
                .getResult();

        return serverInformation.getVersion();
    }

    /**
     * Get the count of documents in the couchDB
     * @return count of documents
     */
    public Long getDocumentCount() {

        Cloudant client = createCloudantClient();
        GetDatabaseInformationOptions dbInformationOptions =
                new GetDatabaseInformationOptions.Builder(this.dbName).build();

        DatabaseInformation dbInformationResponse = client
                .getDatabaseInformation(dbInformationOptions)
                .execute()
                .getResult();

        // 4. Show document count in database =================================
        return dbInformationResponse.getDocCount();
    }


    /**
     * Write a new CSAF document to the couchDB
     * @param uuid id fo the new document
     * @param rootNode rootNode of the document
     * @return revsion for concurrent control
     * @throws JsonProcessingException error in processing rootNode
     */
    public String writeCsafDocument(final UUID uuid, ObjectNode rootNode) throws JsonProcessingException {

        Cloudant client = createCloudantClient();
        ObjectWriter writer = jacksonMapper.writer(new DefaultPrettyPrinter());
        String createString = writer.writeValueAsString(rootNode);

        PutDocumentOptions createDocumentOptions = new PutDocumentOptions.Builder()
                .db(this.dbName)
                .docId(uuid.toString())
                .contentType("application/json")
                .body(new ByteArrayInputStream(createString.getBytes(StandardCharsets.UTF_8)))
                .build();
        DocumentResult createDocumentResponse = client
                .putDocument(createDocumentOptions)
                .execute()
                .getResult();

        return createDocumentResponse.getRev();
    }

    /**
     * Change a CSAF document in the coudDB
     * @param uuid id of the object to change
     * @param revision old revision of the document
     * @param rootNode new root node
     * @return new revision for concurrent control
     * @throws JsonProcessingException error in processing rootNode
     */
    public String updateCsafDocument(final String uuid, final String revision, ObjectNode rootNode) throws JsonProcessingException {

        Cloudant client = createCloudantClient();
        ObjectWriter writer = jacksonMapper.writer(new DefaultPrettyPrinter());

        rootNode.put(COUCHDB_REVISON_FIELD, revision);
        rootNode.put(COUCHDB_ID_FIELD, uuid);
        String updateString = writer.writeValueAsString(rootNode);
        PostDocumentOptions updateDocumentOptions =
                new PostDocumentOptions.Builder()
                        .db(this.dbName)
                        .contentType("application/json")
                        .body(new ByteArrayInputStream(updateString.getBytes(StandardCharsets.UTF_8)))
                        .build();
        DocumentResult updateDocumentResponse = client
                .postDocument(updateDocumentOptions)
                .execute()
                .getResult();

        return updateDocumentResponse.getRev();
    }

    /**
     *
     * @param uuid id of the object to read
     * @return the document
     * @throws IOException error read document
     */
    public JsonNode readCsafDokument(final String uuid) throws IOException {

        Cloudant client = createCloudantClient();
        GetDocumentOptions documentOptions =
                new GetDocumentOptions.Builder()
                        .db(this.dbName)
                        .docId(uuid)
                        .build();

        InputStream response =
                client.getDocumentAsStream(documentOptions).execute().getResult();
        ObjectReader jsonReader = jacksonMapper.reader();

        return jsonReader.readTree(response);

    }

    /**
     * read the information of all CSAF documents
     * @return list of all document information
     */
    public List<AdvisoryInformationResponse> readAllCsafDocuments() {

        Cloudant client = createCloudantClient();

        String titlePath = String.join(".", DOCUMENT_TITLE);
        String trackIdPath = String.join(".", DOCUMENT_TRACKING_ID);

        Map<String, Object> selector = new HashMap<>();
        selector.put("type", Map.of("$eq", AdvisoryJsonService.ObjectType.Advisory.name()));
        PostFindOptions findOptions = new PostFindOptions.Builder()
                .db(this.dbName)
                .selector(selector)
                .fields(Arrays.asList(WORKFLOW_STATE_FIELD, OWNER_FIELD, AdvisoryJsonService.TYPE_FIELD
                        , COUCHDB_REVISON_FIELD, COUCHDB_ID_FIELD, titlePath, trackIdPath))
                .build();

        FindResult updateDocumentResponse = client
                .postFind(findOptions)
                .execute()
                .getResult();

        List<Document> documents = updateDocumentResponse.getDocs();
        return documents.stream()
                .map(this::convertToAdvisoryInformationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete a CSAF document from the database
     * @param uuid id of the object to delete
     * @param revision revision of the document to delete
     */
    public void deleteCsafDokument(final String uuid, final String revision) throws DatabaseException {

        Cloudant client = createCloudantClient();
        DeleteDocumentOptions documentOptions =
                new DeleteDocumentOptions.Builder()
                        .db(this.dbName)
                        .docId(uuid)
                        .rev(revision)
                        .build();

        try {
            DocumentResult response = client.deleteDocument(documentOptions).execute().getResult();
            if (!response.isOk()) {
                throw new DatabaseException(response.getError());
            }
        } catch (BadRequestException ex){
            throw new DatabaseException("Possible wrong revision",ex);
        } catch (NotFoundException ex2){
            throw new DatabaseException("Possible wrong uuid",ex2);
        }

    }

    /**
     * Create a client to access couchDB
     * @return the new client
     */
    private Cloudant createCloudantClient() {
        BasicAuthenticator authenticator = createBasicAuthenticator();
        Cloudant cloudant = new Cloudant(CLOUDANT_SERVICE_NAME, authenticator);
        cloudant.setServiceUrl(getDbUrl());
        return cloudant;
    }

    /**
     * Create authenticator for the couchDB
     * @return a new base authenticato
     */
    private BasicAuthenticator createBasicAuthenticator() {

        return new BasicAuthenticator.Builder()
                .username(this.dbUser)
                .password(this.dbPassword)
                .build();
    }


    private AdvisoryInformationResponse convertToAdvisoryInformationResponse(Document document) {

        AdvisoryInformationResponse response = new AdvisoryInformationResponse();
        response.setAdvisoryId(document.getId());
        response.setOwner(getStringFieldValue(OWNER_FIELD, document));
        response.setTitle(getStringFieldValue(DOCUMENT_TITLE, document));
        response.setDocumentTrackingId(getStringFieldValue(DOCUMENT_TRACKING_ID, document));
        String workflowState = getStringFieldValue(WORKFLOW_STATE_FIELD, document);
        response.setWorkflowState(WorkflowState.valueOf(WorkflowState.class, workflowState));
        response.setAllowedStateChanges(Collections.emptyList());
        return response;
    }

    /**
     * Convenience Method for {@link #getStringFieldValue(String[], Document)}
     * Get the string value for the given path from the given document
     * @param path the path to the value
     * @param document the document
     * @return the value at the path
     */
    private static String getStringFieldValue(String path, Document document) {

        return getStringFieldValue( new String[]{path}, document);
    }

    /**
     * Get the string value for the given path from the given document
     * @param path the path to the value
     * @param document the document
     * @return the value at the path
     */
    public static String getStringFieldValue(String[] path, Document document) {

        String result = null;
        if(path.length == 1 ) {
            Object value = document.get(path[0]);
            if (value instanceof  String) {
                result = (String) value;
            } else if (value != null) {
                throw new RuntimeException("Value is not of type String");
            }
        } else {
            Object value = document.get(path[0]);
            Map<Object, Object> object;
            if (value instanceof Map) {
                object = (Map<Object, Object>) value;
            } else if (value == null) {
                object = null;
            }
            else {
                throw new RuntimeException("Value is not of type Object");
            }
            for (int i = 1; i < path.length-1 && object != null; i++) {
                value = object.get(path[i]);
                if (value instanceof Map) {
                    object = (Map<Object, Object>) value;
                } else if (value == null) {
                    object = null;
                }
                else {
                    throw new RuntimeException("Value is not of type Object");
                }
            }
            if(object != null) {
                value =object.get(path[path.length-1]);
                if (value instanceof  String) {
                    result = (String) value;
                } else if (value != null) {
                    throw new RuntimeException("Value is not of type String");
                }
            }
        }
        return result;
    }
}
