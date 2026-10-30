package de.bsi.secvisogram.csaf_cms_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.postgres.entity.AdvisoryEntity;
import de.bsi.secvisogram.csaf_cms_backend.postgres.entity.AdvisoryVersionEntity;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntityConverterTest {

    @Test
    void toEntityMapsAdvisoryReference() throws IOException, CsafException {
        UUID advisoryReference = UUID.randomUUID();
        AdvisoryWrapper wrapper = createAdvisoryWrapper(advisoryReference);

        AdvisoryEntity entity = EntityConverter.toEntity(wrapper, null);

        assertEquals(advisoryReference, entity.getAdvisoryReference());
    }

    @Test
    void toWrapperSortsCsafObjectKeysFromExistingEntity() throws IOException, CsafException {
        AdvisoryEntity entity = new AdvisoryEntity();
        entity.setId(UUID.randomUUID());
        entity.setVersion(1L);
        entity.setWorkflowState("Draft");
        entity.setOwner("test-user");
        entity.setVersioningType("Semantic");
        entity.setCsaf(new tools.jackson.databind.json.JsonMapper().readTree("""
                {
                  "vulnerabilities": [{ "zebra": 1, "alpha": 2 }],
                  "document": { "tracking": { "version": "1.0.0", "id": "CSAF-2026-1" } }
                }
                """));

        AdvisoryWrapper wrapper = EntityConverter.toWrapper(entity);

        assertEquals(List.of("document", "vulnerabilities"), propertyNames(wrapper.getCsaf()));
        assertEquals(List.of("id", "version"), propertyNames(wrapper.getCsaf().at("/document/tracking")));
        assertEquals(List.of("alpha", "zebra"), propertyNames(wrapper.getCsaf().at("/vulnerabilities/0")));
    }

    @Test
    void toWrapperMapsAdvisoryReference() throws IOException, CsafException {
        UUID advisoryId = UUID.randomUUID();
        UUID advisoryReference = UUID.randomUUID();
        AdvisoryEntity entity = new AdvisoryEntity();
        entity.setId(advisoryId);
        entity.setVersion(1L);
        entity.setWorkflowState("Draft");
        entity.setOwner("test-user");
        entity.setVersioningType("Semantic");
        entity.setAdvisoryReference(advisoryReference);

        AdvisoryWrapper wrapper = EntityConverter.toWrapper(entity);

        assertEquals(advisoryReference.toString(), wrapper.getAdvisoryReference());
    }

    @Test
    void toVersionEntityMapsParentAdvisoryAsReference() throws IOException, CsafException {
        UUID parentAdvisoryId = UUID.randomUUID();
        AdvisoryEntity parentEntity = new AdvisoryEntity();
        parentEntity.setId(parentAdvisoryId);
        AdvisoryWrapper versionWrapper = createAdvisoryWrapper(parentAdvisoryId);

        AdvisoryVersionEntity versionEntity = EntityConverter.toVersionEntity(versionWrapper, parentEntity);

        assertEquals(parentEntity, versionEntity.getAdvisory());
        assertEquals(parentAdvisoryId, versionEntity.getAdvisory().getId());
    }

    private AdvisoryWrapper createAdvisoryWrapper(UUID advisoryReference) throws IOException, CsafException {
        String json = """
                {
                  "type": "Advisory",
                  "workflowState": "Draft",
                  "owner": "test-user",
                  "versioningType": "Semantic",
                  "advisoryReference": "%s",
                  "csaf": { "document": {} }
                }
                """.formatted(advisoryReference);
        return AdvisoryWrapper.createFromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    private List<String> propertyNames(tools.jackson.databind.JsonNode node) {
        return node.properties().stream().map(entry -> entry.getKey()).toList();
    }
}