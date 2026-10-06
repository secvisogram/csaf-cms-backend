package de.bsi.secvisogram.csaf_cms_backend;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class OnStartupImporterTest {

    private static final String CSAF_DOC = "{\"document\":{\"category\":\"CSAF_BASE\"}}";

    @Mock
    AdvisoryService advisoryService;

    @InjectMocks
    OnStartupImporter importer;

    @Test
    void importsEachValidFileInTheDirectory(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("advisory1.json"), CSAF_DOC);
        Files.writeString(importDir.resolve("advisory2.json"), CSAF_DOC);

        importer.importAdvisories(importDir);

        verify(advisoryService, times(2)).importAdvisoryForSystem(any(JsonNode.class));
    }

    @Test
    void malformedJsonIsSkippedAndRemainingFilesAreStillImported(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("broken.json"), "{ not valid json");
        Files.writeString(importDir.resolve("good.json"), CSAF_DOC);

        assertDoesNotThrow(() -> importer.importAdvisories(importDir));

        verify(advisoryService, times(1)).importAdvisoryForSystem(any(JsonNode.class));
    }

    @Test
    void csafExceptionFromOneFileDoesNotStopTheRemainingImports(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("advisory1.json"), CSAF_DOC);
        Files.writeString(importDir.resolve("advisory2.json"), CSAF_DOC);

        when(advisoryService.importAdvisoryForSystem(any(JsonNode.class)))
                .thenThrow(new CsafException("Duplicate", CsafExceptionKey.DuplicateImport))
                .thenReturn(null);

        assertDoesNotThrow(() -> importer.importAdvisories(importDir));

        verify(advisoryService, times(2)).importAdvisoryForSystem(any(JsonNode.class));
    }

    @Test
    void validationServiceUnavailableDoesNotStopTheRemainingImports(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("advisory1.json"), CSAF_DOC);
        Files.writeString(importDir.resolve("advisory2.json"), CSAF_DOC);

        when(advisoryService.importAdvisoryForSystem(any(JsonNode.class)))
                .thenThrow(new CsafException("Validation server unreachable", CsafExceptionKey.ErrorAccessingValidationServer,
                        HttpStatus.SERVICE_UNAVAILABLE))
                .thenReturn(null);

        assertDoesNotThrow(() -> importer.importAdvisories(importDir));

        verify(advisoryService, times(2)).importAdvisoryForSystem(any(JsonNode.class));
    }

    @Test
    void subdirectoriesAreSkippedWithoutBeingImported(@TempDir Path importDir) throws Exception {
        Files.createDirectory(importDir.resolve("subdir"));
        Files.writeString(importDir.resolve("advisory.json"), CSAF_DOC);

        importer.importAdvisories(importDir);

        verify(advisoryService, times(1)).importAdvisoryForSystem(any(JsonNode.class));
    }

    @Test
    void missingImportDirectoryJustLogsWarning(CapturedOutput output) {
        var nonExistentPath = Path.of("does-not-exist-a1b2c3");

        importer.importAdvisories(nonExistentPath);

        verifyNoInteractions(advisoryService);
        assertTrue(output.getAll().contains("WARN"));
        assertTrue(output.getAll().contains("No directory " + nonExistentPath.toAbsolutePath() + " found, nothing to import."));
    }

    @Test
    void ioExceptionFromTheServiceDoesNotStopTheRemainingImports(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("advisory1.json"), CSAF_DOC);
        Files.writeString(importDir.resolve("advisory2.json"), CSAF_DOC);

        when(advisoryService.importAdvisoryForSystem(any(JsonNode.class)))
                .thenThrow(new java.io.IOException("disk error"))
                .thenReturn(null);

        assertDoesNotThrow(() -> importer.importAdvisories(importDir));

        verify(advisoryService, times(2)).importAdvisoryForSystem(any(JsonNode.class));
    }

    @Test
    void successfullyImportedFileIsMovedToProcessedSubdirectory(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("advisory.json"), CSAF_DOC);

        importer.importAdvisories(importDir);

        assertFalse(Files.exists(importDir.resolve("advisory.json")));
        assertTrue(Files.exists(importDir.resolve("processed").resolve("advisory.json")));
        assertFalse(Files.exists(importDir.resolve("processed").resolve("advisory.json.err")));
    }

    @Test
    void duplicateImportIsMovedToFailedSubdirectoryWithAnErrorLogFile(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("advisory.json"), CSAF_DOC);

        when(advisoryService.importAdvisoryForSystem(any(JsonNode.class)))
                .thenThrow(new CsafException("Duplicate", CsafExceptionKey.DuplicateImport));

        importer.importAdvisories(importDir);

        assertFalse(Files.exists(importDir.resolve("advisory.json")));
        assertTrue(Files.exists(importDir.resolve("failed").resolve("advisory.json")));
        String errorLog = Files.readString(importDir.resolve("failed").resolve("advisory.json.err"));
        assertTrue(errorLog.contains("Duplicate"));
    }

    @Test
    void malformedJsonFileIsMovedToFailedSubdirectoryWithAnErrorLogFile(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("broken.json"), "{ not valid json");

        importer.importAdvisories(importDir);

        assertFalse(Files.exists(importDir.resolve("broken.json")));
        assertTrue(Files.exists(importDir.resolve("failed").resolve("broken.json")));
        String errorLog = Files.readString(importDir.resolve("failed").resolve("broken.json.err"));
        assertTrue(errorLog.contains("Unexpected character"));
    }

    @Test
    void validationServiceUnavailableLeavesFileInPlaceForRetry(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("advisory.json"), CSAF_DOC);

        when(advisoryService.importAdvisoryForSystem(any(JsonNode.class)))
                .thenThrow(new CsafException("Validation server unreachable", CsafExceptionKey.ErrorAccessingValidationServer,
                        HttpStatus.SERVICE_UNAVAILABLE));

        importer.importAdvisories(importDir);

        assertTrue(Files.exists(importDir.resolve("advisory.json")));
    }

    @Test
    void aNameCollisionInTheProcessedSubdirectoryGetsAUniqueSuffix(@TempDir Path importDir) throws Exception {
        Files.createDirectories(importDir.resolve("processed"));
        Files.writeString(importDir.resolve("processed").resolve("advisory.json"), "already there");
        Files.writeString(importDir.resolve("advisory.json"), CSAF_DOC);

        importer.importAdvisories(importDir);

        assertTrue(Files.exists(importDir.resolve("processed").resolve("advisory.json.1")));
    }
    // -----------------------------------------------------------------------------------------------------------
    // several versions of one advisory in the import directory, see
    // https://github.com/secvisogram/csaf-cms-backend/issues/103
    // -----------------------------------------------------------------------------------------------------------

    @Test
    void versionsOfTheSameAdvisoryAreImportedOldestFirst(@TempDir Path importDir) throws Exception {
        // file names are deliberately sorted in the opposite order to the versions they contain
        Files.writeString(importDir.resolve("a-second-release.json"), csafDoc("TRACK-1", "2.0.0"));
        Files.writeString(importDir.resolve("b-first-release.json"), csafDoc("TRACK-1", "1.0.0"));

        importer.importAdvisories(importDir);

        assertEquals(List.of("1.0.0", "2.0.0"), importedVersionsInOrder());
    }

    @Test
    void versionsOfTheSameAdvisoryAreOrderedNumericallyWithIntegerVersioning(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("a-version-10.json"), csafDoc("TRACK-1", "10"));
        Files.writeString(importDir.resolve("b-version-9.json"), csafDoc("TRACK-1", "9"));

        importer.importAdvisories(importDir);

        assertEquals(List.of("9", "10"), importedVersionsInOrder(),
                "integer versions must not be ordered as strings");
    }

    @Test
    void versionsAlreadyInAscendingOrderKeepThatOrder(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("a-first-release.json"), csafDoc("TRACK-1", "1.0.0"));
        Files.writeString(importDir.resolve("b-second-release.json"), csafDoc("TRACK-1", "2.0.0"));

        importer.importAdvisories(importDir);

        assertEquals(List.of("1.0.0", "2.0.0"), importedVersionsInOrder());
    }

    @Test
    void anUncheckedExceptionFromTheServiceDoesNotStopTheRemainingImports(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("advisory1.json"), csafDoc("TRACK-1", "1.0.0"));
        Files.writeString(importDir.resolve("advisory2.json"), csafDoc("TRACK-2", "1.0.0"));

        when(advisoryService.importAdvisoryForSystem(any(JsonNode.class)))
                .thenThrow(new NullPointerException("malformed advisory in the database"))
                .thenReturn(null);

        // an unchecked exception must never escape - in production this runs in @PostConstruct,
        // where it would stop the application from starting at all
        assertDoesNotThrow(() -> importer.importAdvisories(importDir));

        verify(advisoryService, times(2)).importAdvisoryForSystem(any(JsonNode.class));
    }

    @Test
    void aFileThatFailedWithAnUncheckedExceptionIsMovedToFailedSubdirectory(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("advisory.json"), csafDoc("TRACK-1", "1.0.0"));

        when(advisoryService.importAdvisoryForSystem(any(JsonNode.class)))
                .thenThrow(new IllegalArgumentException("No enum constant WorkflowState.null"));

        importer.importAdvisories(importDir);

        // the file must not be left behind, otherwise every restart hits the same error again
        assertFalse(Files.exists(importDir.resolve("advisory.json")));
        assertTrue(Files.exists(importDir.resolve("failed").resolve("advisory.json")));
        String errorLog = Files.readString(importDir.resolve("failed").resolve("advisory.json.err"));
        assertTrue(errorLog.contains("No enum constant"));
    }

    @Test
    void versionsOfDifferentAdvisoriesAreAllImported(@TempDir Path importDir) throws Exception {
        Files.writeString(importDir.resolve("one.json"), csafDoc("TRACK-1", "1.0.0"));
        Files.writeString(importDir.resolve("two.json"), csafDoc("TRACK-2", "1.0.0"));

        importer.importAdvisories(importDir);

        verify(advisoryService, times(2)).importAdvisoryForSystem(any(JsonNode.class));
    }

    private static String csafDoc(String trackingId, String version) {
        return """
               {
                 "document": {
                   "category": "CSAF_BASE",
                   "tracking": {
                     "id": "%s",
                     "status": "final",
                     "version": "%s"
                   }
                 }
               }""".formatted(trackingId, version);
    }

    private List<String> importedVersionsInOrder() throws Exception {
        ArgumentCaptor<JsonNode> importedDocs = ArgumentCaptor.forClass(JsonNode.class);
        verify(advisoryService, atLeastOnce()).importAdvisoryForSystem(importedDocs.capture());
        return importedDocs.getAllValues().stream()
                .map(csafDoc -> csafDoc.at("/document/tracking/version").asString())
                .toList();
    }
}
