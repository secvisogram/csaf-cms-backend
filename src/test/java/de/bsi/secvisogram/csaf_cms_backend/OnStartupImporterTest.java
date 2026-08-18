package de.bsi.secvisogram.csaf_cms_backend;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
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
    void missingImportDirectoryIsANoOp() {
        importer.importAdvisories(Path.of("does-not-exist-a1b2c3"));

        verifyNoInteractions(advisoryService);
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
}