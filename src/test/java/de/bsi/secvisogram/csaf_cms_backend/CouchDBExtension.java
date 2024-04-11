package de.bsi.secvisogram.csaf_cms_backend;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.extension.*;
import org.testcontainers.containers.GenericContainer;

/**
 * Test extension to start a CouchDB container, create a database and set corresponding application properties.
 * The test database is cleared (created before and deleted after) for each single test
 */
public class CouchDBExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private GenericContainer<?> couchDb;

    public static final String couchDbVersion = "3.3.3";
    private static final String user = "testUser";
    private static final String password = "testPassword";
    private static final int initialPort = 5984;
    private static final String dbName = "test-db";

    @Override
    public void beforeAll(ExtensionContext context) {
        couchDb = new GenericContainer<>("couchdb:" + couchDbVersion)
                .withEnv("COUCHDB_USER", user)
                .withEnv("COUCHDB_PASSWORD", password)
                .withCommand()
                .withExposedPorts(initialPort);

        couchDb.start();

        System.setProperty("csaf.couchdb.host", couchDb.getHost());
        System.setProperty("csaf.couchdb.port", couchDb.getMappedPort(initialPort).toString());
        System.setProperty("csaf.couchdb.ssl", "false");
        System.setProperty("csaf.couchdb.user", user);
        System.setProperty("csaf.couchdb.password", password);
        System.setProperty("csaf.couchdb.dbname", dbName);

    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        createDatabase();
    }

    @Override
    public void afterEach(ExtensionContext context) throws IOException {
        deleteDatabase();
    }

    @SuppressFBWarnings(value = "URLCONNECTION_SSRF_FD", justification = "URL is built from dynamic values but not user input")
    private void createDatabase() throws IOException {

        HttpURLConnection connection = (HttpURLConnection) createCreateDeleteDatabaseUrl().openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Authorization", createBasicAuth());
        connection.getResponseCode();
    }

    @SuppressFBWarnings(value = "URLCONNECTION_SSRF_FD", justification = "URL is built from dynamic values but not user input")
    private void deleteDatabase() throws IOException {

        HttpURLConnection connection = (HttpURLConnection) createCreateDeleteDatabaseUrl().openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", createBasicAuth());
        connection.getResponseCode();
    }


    private String createBasicAuth() {

        String auth = user + ":" + password;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    private URL createCreateDeleteDatabaseUrl() throws IOException {
        return new URL("http://"
                          + couchDb.getHost() + ":"
                          + couchDb.getMappedPort(initialPort)
                          + "/" + dbName);
    }


    @Override
    public void afterAll(ExtensionContext context) {
        couchDb.stop();
    }

}
