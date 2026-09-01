package de.bsi.secvisogram.csaf_cms_backend;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Test extension that starts a PostgreSQL container and sets the corresponding Spring datasource
 * properties. The container is shared across all tests in the same JVM (static) and cleaned up
 * on JVM shutdown. Flyway runs automatically on Spring context startup (creates the schema).
 *
 * <p>The database is truncated before each test to isolate test data, mirroring the per-test
 * database recreation that the former CouchDBExtension performed.</p>
 *
 * <p>Usage: {@code @ExtendWith(PostgreSQLExtension.class)} on integration test classes.</p>
 */
public class PostgreSQLExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("csaf_test")
                    .withUsername("test")
                    .withPassword("test");

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRES.getUsername());
        System.setProperty("spring.datasource.password", POSTGRES.getPassword());
        // Enable JPA and Flyway for integration tests (overrides test application.properties exclusion)
        System.setProperty("spring.autoconfigure.exclude", "");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        System.setProperty("spring.flyway.enabled", "true");
        System.setProperty("spring.flyway.clean-disabled", "false");
    }

    @Override
    public void beforeEach(@NonNull ExtensionContext context) throws SQLException {
        // Truncate all application tables before each test to ensure isolation.
        // Uses TRUNCATE ... CASCADE to handle foreign key dependencies.
        try (Connection conn = java.sql.DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    TRUNCATE TABLE audit_trail_comments,
                                   audit_trail_documents,
                                   audit_trail_workflows,
                                   advisory_versions,
                                   comments,
                                   advisories,
                                   counters
                    CASCADE
                    """);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Container is shared (static), stopped by JVM shutdown hook
    }
}
