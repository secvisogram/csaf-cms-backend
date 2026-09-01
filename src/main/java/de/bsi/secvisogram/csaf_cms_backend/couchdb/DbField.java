package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import tools.jackson.databind.JsonNode;

/**
 * Interface for database field metadata (field name and JSON path).
 * Retained from the CouchDB era for compatibility with wrapper classes.
 */
public interface DbField {

    String getDbName();

    String[] getFieldPath();

    default String stringVal(JsonNode doc) {
        return doc.get(this.getDbName()).asString();
    }

}
