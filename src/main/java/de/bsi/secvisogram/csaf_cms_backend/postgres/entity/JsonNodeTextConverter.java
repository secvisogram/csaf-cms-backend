package de.bsi.secvisogram.csaf_cms_backend.postgres.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Maps a {@link JsonNode} to the text column holding the CSAF document.
 */
@Converter
public class JsonNodeTextConverter implements AttributeConverter<JsonNode, String> {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        return attribute == null ? null : MAPPER.writeValueAsString(attribute);
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MAPPER.readTree(dbData);
    }
}
