package de.bsi.secvisogram.csaf_cms_backend.json.field;

import tools.jackson.databind.JsonNode;

/**
 * Metadata for a JSON field, including its serialized name and path.
 */
public interface DbField {

    String getDbName();

    String[] getFieldPath();

    default String stringVal(JsonNode doc) {
        return doc.get(this.getDbName()).asString();
    }

}