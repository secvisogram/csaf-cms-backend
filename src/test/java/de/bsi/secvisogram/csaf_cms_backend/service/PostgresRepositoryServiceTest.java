package de.bsi.secvisogram.csaf_cms_backend.service;

import de.bsi.secvisogram.csaf_cms_backend.entity.AdvisoryEntity;
import de.bsi.secvisogram.csaf_cms_backend.entity.AdvisoryVersionEntity;
import de.bsi.secvisogram.csaf_cms_backend.repository.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgresRepositoryServiceTest {

    @Test
    void saveAdvisoryUpdatesDenormalizedListColumns()  {
        AdvisoryRepository advisoryRepository = mock(AdvisoryRepository.class);
        when(advisoryRepository.saveAndFlush(any(AdvisoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PostgresRepositoryService service = new PostgresRepositoryService(
                advisoryRepository,
                mock(AdvisoryVersionRepository.class),
                mock(AuditTrailDocumentRepository.class),
                mock(AuditTrailWorkflowRepository.class),
                mock(CommentRepository.class),
                mock(AuditTrailCommentRepository.class),
                mock(CounterRepository.class));

        AdvisoryEntity entity = new AdvisoryEntity();
        entity.setCsaf(new JsonMapper().readTree("""
                {
                  "document": {
                    "title": "Initial title",
                    "tracking": {
                      "id": "CSAF-2026-0001",
                      "current_release_date": "2026-10-12T06:00:00Z"
                    }
                  }
                }
                """));

        service.saveAdvisory(entity);

        assertEquals("CSAF-2026-0001", entity.getTrackingId());
        assertEquals("Initial title", entity.getDocumentTitle());
        assertEquals(Instant.parse("2026-10-12T06:00:00Z"), entity.getCurrentReleaseDate());

        entity.setCsaf(new JsonMapper().readTree("""
                {
                  "document": {
                    "title": "Updated title",
                    "tracking": {
                      "id": "CSAF-2026-0002",
                      "current_release_date": " "
                    }
                  }
                }
                """));

        service.saveAdvisory(entity);

        assertEquals("CSAF-2026-0002", entity.getTrackingId());
        assertEquals("Updated title", entity.getDocumentTitle());
        assertNull(entity.getCurrentReleaseDate());
    }

    @Test
    void saveAdvisoryVersionUpdatesDenormalizedListColumns() {
        AdvisoryVersionRepository advisoryVersionRepository = mock(AdvisoryVersionRepository.class);
        when(advisoryVersionRepository.save(any(AdvisoryVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PostgresRepositoryService service = new PostgresRepositoryService(
                mock(AdvisoryRepository.class),
                advisoryVersionRepository,
                mock(AuditTrailDocumentRepository.class),
                mock(AuditTrailWorkflowRepository.class),
                mock(CommentRepository.class),
                mock(AuditTrailCommentRepository.class),
                mock(CounterRepository.class));

        AdvisoryVersionEntity entity = new AdvisoryVersionEntity();
        entity.setCsaf(new JsonMapper().readTree("""
                {
                  "document": {
                    "title": "Snapshot title",
                    "tracking": {
                      "id": "CSAF-2026-0003",
                      "current_release_date": "2026-10-13T06:00:00Z"
                    }
                  }
                }
                """));

        service.saveAdvisoryVersion(entity);

        assertEquals("CSAF-2026-0003", entity.getTrackingId());
        assertEquals("Snapshot title", entity.getDocumentTitle());
        assertEquals(Instant.parse("2026-10-13T06:00:00Z"), entity.getCurrentReleaseDate());

        entity.setCsaf(new JsonMapper().readTree("""
                {
                  "document": {
                    "title": "Snapshot title",
                    "tracking": {
                      "id": "CSAF-2026-0003",
                      "current_release_date": "not-a-date"
                    }
                  }
                }
                """));

        service.saveAdvisoryVersion(entity);

        assertNull(entity.getCurrentReleaseDate());
    }
}
