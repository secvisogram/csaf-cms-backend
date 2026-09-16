package de.bsi.secvisogram.csaf_cms_backend;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbService;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import de.bsi.secvisogram.csaf_cms_backend.validator.ValidatorServiceClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Integration test for the import of several versions of one advisory on startup, see
 * <a href="https://github.com/secvisogram/csaf-cms-backend/issues/103">issue #103</a>.
 */
@SpringBootTest(properties = {"csaf.workflow.allowOwnDocumentsApproved=true"})
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@SpringJUnitConfig
class OnStartupImporterIntegrationTest {

    private static final String TRACKING_ID = "CSAF-CMS-103";

    @Autowired
    private OnStartupImporter importer;

    @Autowired
    private AdvisoryService advisoryService;

    @Autowired
    private CouchDbService couchDbService;

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_EDITOR})
    void everyVersionOfAnAdvisoryIsImportedAndMovedToProcessed(@TempDir Path importDir) throws Exception {

        // file names are deliberately sorted in the opposite order to the versions they contain
        Files.writeString(importDir.resolve("a-second-release.json"), csafDocument(TRACKING_ID, "2.0.0"));
        Files.writeString(importDir.resolve("b-first-release.json"), csafDocument(TRACKING_ID, "1.0.0"));

        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {
            importer.importAdvisories(importDir);
        }

        assertTrue(Files.exists(importDir.resolve("processed").resolve("a-second-release.json")),
                "the newer version should have been imported");
        assertTrue(Files.exists(importDir.resolve("processed").resolve("b-first-release.json")),
                "the older version should have been imported as well");
        assertFalse(Files.exists(importDir.resolve("failed")),
                "no version should have been rejected");

        assertEquals("2.0.0", liveVersionOf(TRACKING_ID), "the newest version should be the live advisory");
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_EDITOR})
    void anOutdatedVersionOfAnAlreadyImportedAdvisoryIsMovedToFailed(@TempDir Path importDir) throws Exception {

        Files.writeString(importDir.resolve("second-release.json"), csafDocument(TRACKING_ID, "2.0.0"));
        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {
            importer.importAdvisories(importDir);
        }
        assertTrue(Files.exists(importDir.resolve("processed").resolve("second-release.json")));

        // a later run of the importer finds a version that is older than what is already in the system
        Files.writeString(importDir.resolve("first-release.json"), csafDocument(TRACKING_ID, "1.0.0"));
        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {
            importer.importAdvisories(importDir);
        }

        assertTrue(Files.exists(importDir.resolve("failed").resolve("first-release.json")),
                "an outdated version should be rejected");
        assertTrue(Files.readString(importDir.resolve("failed").resolve("first-release.json.err")).length() > 0,
                "a rejected version should be documented in an error log file");
        assertEquals("2.0.0", liveVersionOf(TRACKING_ID),
                "the live advisory must not be replaced by an outdated version");
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_EDITOR})
    void aMalformedAdvisoryInTheDatabaseDoesNotStopTheImportRun(@TempDir Path importDir) throws Exception {

        // an advisory without a workflowState - e.g. written by an older schema or edited by hand.
        // Reading it makes WorkflowState.valueOf(null) throw an unchecked exception while the import
        // checks whether the stored advisory may be replaced.
        couchDbService.writeDocument(UUID.randomUUID(), advisoryWithoutWorkflowState(TRACKING_ID, "1.0.0"));

        Files.writeString(importDir.resolve("newer-release.json"), csafDocument(TRACKING_ID, "2.0.0"));
        Files.writeString(importDir.resolve("other-advisory.json"), csafDocument("CSAF-CMS-103-OTHER", "1.0.0"));

        // importAdvisories runs in @PostConstruct - anything escaping it stops the application from
        // starting, and the file would still be there on the next attempt
        try (final MockedStatic<ValidatorServiceClient> validatorMock = mockValidCsaf()) {
            assertDoesNotThrow(() -> importer.importAdvisories(importDir));
        }

        assertTrue(Files.exists(importDir.resolve("failed").resolve("newer-release.json")),
                "the file that could not be imported should be moved out of the import directory");
        assertTrue(Files.exists(importDir.resolve("processed").resolve("other-advisory.json")),
                "an unrelated advisory should still be imported");
    }

    private static String advisoryWithoutWorkflowState(String trackingId, String version) {

        return """
               {
                 "type": "Advisory",
                 "owner": "someone",
                 "versioningType": "Semantic",
                 "csaf": {
                   "document": {
                     "category": "CSAF Base",
                     "title": "Advisory without a workflow state",
                     "tracking": {
                       "id": "%s",
                       "status": "final",
                       "version": "%s"
                     }
                   }
                 }
               }""".formatted(trackingId, version);
    }

    private static MockedStatic<ValidatorServiceClient> mockValidCsaf() {

        MockedStatic<ValidatorServiceClient> validatorMock = Mockito.mockStatic(ValidatorServiceClient.class);
        validatorMock.when(() -> ValidatorServiceClient.isCsafValid(any(), any())).thenReturn(Boolean.TRUE);
        validatorMock.when(() -> ValidatorServiceClient.isAdvisoryValid(any(), any())).thenReturn(Boolean.TRUE);
        return validatorMock;
    }

    private String liveVersionOf(String trackingId) throws IOException, DatabaseException, CsafException {

        List<AdvisoryInformationResponse> advisories = advisoryService.getAdvisoryInformations(null).stream()
                .filter(info -> trackingId.equals(info.getDocumentTrackingId()))
                .toList();
        assertEquals(1, advisories.size(), "there should be exactly one live advisory for " + trackingId);
        return advisoryService.getAdvisory(advisories.get(0).getAdvisoryId())
                .getCsaf().at("/document/tracking/version").asString();
    }

    private static String csafDocument(String trackingId, String version) {

        return """
               {
                 "document": {
                   "category": "CSAF Base",
                   "csaf_version": "2.0",
                   "title": "Import of different versions from one advisory",
                   "lang": "en",
                   "distribution": {
                     "tlp": {
                       "label": "WHITE"
                     }
                   },
                   "publisher": {
                     "category": "other",
                     "name": "Secvisogram Automated Tester",
                     "namespace": "https://github.com/secvisogram/secvisogram"
                   },
                   "tracking": {
                     "current_release_date": "2022-09-08T12:33:45.678Z",
                     "id": "%s",
                     "initial_release_date": "2022-09-08T12:33:45.678Z",
                     "revision_history": [
                       {
                         "number": "%s",
                         "date": "2022-09-08T12:33:45.678Z",
                         "summary": "imported version %s"
                       }
                     ],
                     "status": "final",
                     "version": "%s"
                   }
                 }
               }""".formatted(trackingId, version, version, version);
    }
}
