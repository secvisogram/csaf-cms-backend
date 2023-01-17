package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDBFilterCreator.expr2CouchDBFilter;
import static de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbField.TYPE_FIELD;
import static de.bsi.secvisogram.csaf_cms_backend.model.filter.OperatorExpression.equal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.*;
import com.ibm.cloud.sdk.core.security.BasicAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.exception.ConflictException;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.json.ObjectType;
import de.bsi.secvisogram.csaf_cms_backend.service.IdAndRevision;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * Service to create, update and delete objects in a couchDB database
 */
@Repository
public class CouchDbService {

    private static final Logger LOG = LoggerFactory.getLogger(CouchDbService.class);
    private static final String CLOUDANT_SERVICE_NAME = "SECVISOGRAM";

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

    /**
     * Get the version of the couchdb server
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
     * Get the count of all documents in the database
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
     * Write a new document to the database with a UUID as objectId
     *
     * @param uuid         id fo the new document
     * @param createString JSON encoded string of the document to add
     * @return revision for concurrent control
     */
    public String writeDocument(final UUID uuid, String createString) {

         return writeDocument(uuid.toString(), createString);
    }

    /**
     * Write a new document to the database
     *
     * @param objectId         id fo the new document
     * @param createString JSON encoded string of the document to add
     * @return revision for concurrent control
     */
    public String writeDocument(final String objectId, String createString) {

        Cloudant client = createCloudantClient();

        PutDocumentOptions createDocumentOptions = new PutDocumentOptions.Builder()
                .db(this.dbName)
                .docId(objectId)
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
     * Write a new document to the database
     *
     * @param uuid     id fo the new document
     * @param rootNode object to encode as json and write to the database
     * @return revision for concurrent control
     */
    public String writeDocument(final UUID uuid, Object rootNode) throws JsonProcessingException {

        Cloudant client = createCloudantClient();
        final ObjectMapper jacksonMapper = new ObjectMapper();
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
     * Change a document in the couchDB
     *
     * @param updateString the new root node as string
     * @return new revision for concurrent control
     */
    public String updateDocument(String updateString) throws DatabaseException {

        Cloudant client = createCloudantClient();

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
        } catch (ConflictException | NotFoundException nfEx) {
            String msg = "No element with given ID";
            LOG.error(msg);
            throw new IdNotFoundException(msg, nfEx);
        }
    }

    /**
     * Read a document from the database as stream
     *
     * @param uuid id of the document to read
     * @return the requested document as stream
     * @throws IdNotFoundException if the requested document was not found
     */
    public InputStream readDocumentAsStream(final String uuid) throws IdNotFoundException {

        Cloudant client = createCloudantClient();
        GetDocumentOptions documentOptions =
                new GetDocumentOptions.Builder()
                        .db(this.dbName)
                        .docId(uuid)
                        .build();

        try {
            return client.getDocumentAsStream(documentOptions).execute().getResult();
        } catch (NotFoundException nfEx) {
            String msg = "No element with such an ID";
            LOG.error(msg);
            throw new IdNotFoundException(msg, nfEx);
        }
    }

    /**
     * Read the information of all documents of a given type
     *
     * @param type   the {@link ObjectType} of objects to retrieve information on
     * @param fields the fields of information to select
     * @return list of all requested document information
     */
    public List<Document> readAllDocuments(ObjectType type, Collection<DbField> fields) {

        Map<String, Object> selector = expr2CouchDBFilter(equal(type.name(), TYPE_FIELD.getDbName()));
        return findDocuments(selector, fields);
    }

    /**
     * Read the information of the documents matching the selector
     *
     * @param selector the selector to search for
     * @param fields   the fields of information to select
     * @return list of all document information that match the selector
     */
    public List<Document> findDocuments(Map<String, Object> selector, Collection<DbField> fields) {

        Cloudant client = createCloudantClient();

        PostFindOptions findOptions = new PostFindOptions.Builder()
                .db(this.dbName)
                .selector(selector)
                .fields(fields.stream().map(DbField::getDbName).collect(Collectors.toList()))
                .build();

        FindResult findDocumentResult = client
                .postFind(findOptions)
                .execute()
                .getResult();

        return findDocumentResult.getDocs();
    }

    /**
     * Read the information of the documents matching the selector
     *
     * @param selector the selector to search for
     * @param fields   the fields of information to select
     * @return the result as stream
     */
    public InputStream findDocumentsAsStream(Map<String, Object> selector, Collection<DbField> fields) {

        Cloudant client = createCloudantClient();

        PostFindOptions findOptions = new PostFindOptions.Builder()
                .db(this.dbName)
                .selector(selector)
                .fields(fields.stream().map(DbField::getDbName).collect(Collectors.toList()))
                .build();

        return client
                .postFindAsStream(findOptions)
                .execute()
                .getResult();
    }

    /**
     * Delete a document from the database
     *
     * @param uuid     id of the object to delete
     * @param revision revision of the document to delete
     */
    public void deleteDocument(final String uuid, final String revision) throws DatabaseException {

        Cloudant client = createCloudantClient();
        DeleteDocumentOptions documentOptions =
                new DeleteDocumentOptions.Builder()
                        .db(this.dbName)
                        .docId(uuid)
                        .rev(revision)
                        .build();

        try {
            DocumentResult response = client.deleteDocument(documentOptions).execute().getResult();
            if (response.isOk() == null || !response.isOk()) {
                throw new DatabaseException(response.getError());
            }
        } catch (BadRequestException brEx) {
            String msg = "Bad request, possibly the given revision is invalid";
            LOG.error(msg);
            throw new DatabaseException(msg, brEx);
        } catch (NotFoundException nfEx) {
            String msg = "No element with such an ID";
            LOG.error(msg);
            throw new IdNotFoundException(msg, nfEx);
        }

    }

    /**
     * Delete multiple objects from the database
     *
     * @param objectsToDelete collection of ids and revisions of all documents to delete
     * @throws DatabaseException Deletion of at least one object failed
     */
    public void bulkDeleteDocuments(final Collection<IdAndRevision> objectsToDelete) throws DatabaseException {

        Cloudant client = createCloudantClient();
        List<Document> documents = objectsToDelete.stream()
                .map(this::createBulkDelete)
                .collect(Collectors.toList());


        BulkDocs bulkDocs = new BulkDocs.Builder()
                .docs(documents)
                .build();

        PostBulkDocsOptions bulkDocsOptions = new PostBulkDocsOptions.Builder()
                .db(this.dbName)
                .bulkDocs(bulkDocs)
                .build();

        try {
            List<DocumentResult> responses =
                    client.postBulkDocs(bulkDocsOptions).execute()
                            .getResult();
            for (DocumentResult response : responses) {
                if (response.isOk() == null || !response.isOk()) {
                    throw new DatabaseException(response.getError());
                }
            }
        } catch (BadRequestException brEx) {
            String msg = "Bad request, possibly one of the given revisions is invalid";
            LOG.error(msg);
            throw new DatabaseException(msg, brEx);
        }
    }

    /**
     * Convert IdAndRevision to delete document
     *
     * @param object the id and revision to convert
     * @return the converted object
     */
    private Document createBulkDelete(IdAndRevision object) {

        Document eventDoc1 = new Document();
        eventDoc1.setId(object.getId());
        eventDoc1.setRev(object.getRevision());
        eventDoc1.setDeleted(Boolean.TRUE);
        return eventDoc1;
    }

    /**
     * Create a client to access the couchDB database
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
     * Create authenticator for the couchDB database
     *
     * @return a new base authentication
     */
    private BasicAuthenticator createBasicAuthenticator() {

        return new BasicAuthenticator.Builder()
                .username(this.dbUser)
                .password(this.dbPassword)
                .build();
    }

}