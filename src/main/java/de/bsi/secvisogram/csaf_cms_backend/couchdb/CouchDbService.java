package de.bsi.secvisogram.csaf_cms_backend.couchdb;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.*;
import com.ibm.cloud.sdk.core.security.BasicAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * Service to create, update and delete objects in a couchDB
 */
@Repository
public class CouchDbService {

    public static final String REVISION_FIELD = "_rev";
    public static final String ID_FIELD = "_id";

    private static final Logger LOG = LoggerFactory.getLogger(CouchDbService.class);
    private static final String CLOUDANT_SERVICE_NAME = "SECVISOGRAM";

    public enum ObjectType {
        Advisory
    }

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

    /**
     * Get the CouchDB connection string
     *
     * @return CouchDB connection string
     */
    private String getDbUrl() {
        String protocol = this.dbSsl ? "https://" : "http://";
        return protocol + dbHost + ":" + dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    /**
     * Create a new CouchDB
     *
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
                LOG.info("{}' database created.", nameOfNewDb);
            }
        } catch (ServiceResponseException sre) {
            if (sre.getStatusCode() == 412) {
                throw new RuntimeException("Cannot create \"" + nameOfNewDb + "\" database, it already exists.", sre);
            }
        }
    }

    /**
     * Delete a new CouchDB
     *
     * @param nameOfNewDb name of the couchdb database
     */
    public void deleteDatabase(String nameOfNewDb) {

        Cloudant client = createCloudantClient();
        DeleteDatabaseOptions deleteDbOptions =
                new DeleteDatabaseOptions.Builder().db(nameOfNewDb).build();

        // Try to create database if it doesn't exist
        try {
            Ok deleteResult = client
                    .deleteDatabase(deleteDbOptions)
                    .execute()
                    .getResult();

            if (deleteResult.isOk()) {
                LOG.info("{}' database deleted.", nameOfNewDb);
            }
        } catch (ServiceResponseException sre) {
            if (sre.getStatusCode() == 412) {
                throw new RuntimeException("Cannot create \"" + nameOfNewDb + "\" database, it already exists.", sre);
            }
        }
    }



    /**
     * Get the Version of the couchdb server
     *
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
     *
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

        return dbInformationResponse.getDocCount();
    }


    /**
     * Write a new CSAF document to the couchDB
     *
     * @param uuid     id fo the new document
     * @param rootNode rootNode of the document
     * @return revision for concurrent control
     */
    public String writeCsafDocument(final UUID uuid, ObjectNode rootNode) {

        return writeDocument(uuid, rootNode);
    }

    public String writeDocument(final UUID uuid, Object rootNode) throws JsonProcessingException {

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
     * Change a CSAF document in the couchDB
     *
     * @param uuid     id of the object to change
     * @param revision old revision of the document
     * @param rootNode new root node
     * @return new revision for concurrent control
     */
    public String updateCsafDocument(final String uuid, final String revision, ObjectNode rootNode) throws DatabaseException {

        Cloudant client = createCloudantClient();

        if (rootNode.has(ID_FIELD) && !rootNode.get(ID_FIELD).asText().equals(uuid)) {
            throw new IllegalArgumentException("The updated object has an ID set that does not match!");
        }
        if (rootNode.has(REVISION_FIELD) && !rootNode.get(REVISION_FIELD).asText().equals(revision)) {
            throw new IllegalArgumentException("The updated object has a revision set that does not match!");
        }

        rootNode.put(ID_FIELD, uuid);
        rootNode.put(REVISION_FIELD, revision);

        String updateString = rootNode.toPrettyString();

        PostDocumentOptions updateDocumentOptions =
                new PostDocumentOptions.Builder()
                        .db(this.dbName)
                        .contentType("application/json")
                        .body(new ByteArrayInputStream(updateString.getBytes(StandardCharsets.UTF_8)))
                        .build();

        try {
            DocumentResult response = client
                    .postDocument(updateDocumentOptions)
                    .execute()
                    .getResult();
            if (!response.isOk()) {
                throw new DatabaseException(response.getError());
            }
            return response.getRev();
        } catch (BadRequestException brEx) {
            String msg = "Bad request, possibly the given revision is invalid";
            LOG.error(msg);
            throw new DatabaseException(msg, brEx);
        } catch (NotFoundException nfEx) {
            String msg = String.format("No element with such an ID: %s", rootNode.at(ID_FIELD).asText());
            LOG.error(msg);
            throw new IdNotFoundException(msg, nfEx);
        }
    }

    /**
     * @param uuid id of the object to read
     * @return the requested document
     * @throws IdNotFoundException if the requested document was not found
     */
    public Document readCsafDocument(final String uuid) throws IdNotFoundException {

        Cloudant client = createCloudantClient();
        GetDocumentOptions documentOptions =
                new GetDocumentOptions.Builder()
                        .db(this.dbName)
                        .docId(uuid)
                        .build();

        try {
            return client.getDocument(documentOptions).execute().getResult();
        } catch (NotFoundException nfEx) {
            String msg = String.format("No element with such an ID: %s", uuid);
            LOG.error(msg);
            throw new IdNotFoundException(msg, nfEx);
        }

    }

    /**
     * read the information of all CSAF documents
     *
     * @param fields the fields of information to select
     * @return list of all requested document information
     */
    public List<Document> readAllCsafDocuments(List<String> fields) {


        Map<String, Object> selector = new HashMap<>();
        selector.put("type", Map.of("$eq", AdvisoryJsonService.ObjectType.Advisory.name()));

        return findCsafDocuments(selector);
    }

    /**
     * read the information of the CSAF documents matching the selector
     *
     * @return list of all document information that match the selector
     */
    public List<AdvisoryInformationResponse> findCsafDocuments(Map<String, Object> selector) {


        String titlePath = String.join(".", DOCUMENT_TITLE);
        String trackIdPath = String.join(".", DOCUMENT_TRACKING_ID);
        List<String> fields = Arrays.asList(WORKFLOW_STATE_FIELD, OWNER_FIELD, TYPE_FIELD,
                COUCHDB_REVISON_FIELD, COUCHDB_ID_FIELD, titlePath, trackIdPath);
        List<Document> documents = this.findDocuments(selector, fields);
        return documents.stream()
                .map(this::convertToAdvisoryInformationResponse)
                .collect(Collectors.toList());
    }


    /**
     * read the information of the documents matching the selector
     *
     * @return list of all document information that match the selector
     */
    public List<Document> findDocuments(Map<String, Object> selector, List<String> fields) {

        Cloudant client = createCloudantClient();

        PostFindOptions findOptions = new PostFindOptions.Builder()
                .db(this.dbName)
                .selector(selector)
                .fields(fields)
                .build();

        FindResult findDocumentResult = client
                .postFind(findOptions)
                .execute()
                .getResult();

        return findDocumentResult.getDocs();
    }
    /**
     * Delete a CSAF document from the database
     *
     * @param uuid     id of the object to delete
     * @param revision revision of the document to delete
     */
    public void deleteCsafDocument(final String uuid, final String revision) throws DatabaseException {

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
        } catch (BadRequestException brEx) {
            String msg = "Bad request, possibly the given revision is invalid";
            LOG.error(msg);
            throw new DatabaseException(msg, brEx);
        } catch (NotFoundException nfEx) {
            String msg = String.format("No element with such an ID: %s", uuid);
            LOG.error(msg);
            throw new IdNotFoundException(msg, nfEx);
        }

    }

    /**
     * Create a client to access couchDB
     *
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
     *
     * @return a new base authentication
     */
    private BasicAuthenticator createBasicAuthenticator() {

        return new BasicAuthenticator.Builder()
                .username(this.dbUser)
                .password(this.dbPassword)
                .build();
    }

    /**
     * Convenience Method for {@link #getStringFieldValue(String[], Document)}
     * Get the string value for the given path from the given document
     *
     * @param path     the path to the value
     * @param document the document
     * @return the value at the path
     */
    public static String getStringFieldValue(String path, Document document) {

        return getStringFieldValue(new String[] {path}, document);
    }

    /**
     * Get the string value for the given path from the given document
     *
     * @param path     the path to the value
     * @param document the document
     * @return the value at the path
     */
    public static String getStringFieldValue(String[] path, Document document) {

        String result = null;
        if (path.length == 1) {
            Object value = document.get(path[0]);
            if (value instanceof String) {
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
            } else {
                throw new RuntimeException("Value is not of type Object");
            }
            for (int i = 1; i < path.length - 1 && object != null; i++) {
                value = object.get(path[i]);
                if (value instanceof Map) {
                    object = (Map<Object, Object>) value;
                } else if (value == null) {
                    object = null;
                } else {
                    throw new RuntimeException("Value is not of type Object");
                }
            }
            if (object != null) {
                value = object.get(path[path.length - 1]);
                if (value instanceof String) {
                    result = (String) value;
                } else if (value != null) {
                    throw new RuntimeException("Value is not of type String");
                }
            }
        }
        return result;
    }
}
