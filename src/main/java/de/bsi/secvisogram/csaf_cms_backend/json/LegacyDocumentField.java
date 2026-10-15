package de.bsi.secvisogram.csaf_cms_backend.json;

import de.bsi.secvisogram.csaf_cms_backend.json.field.DbField;

/**
 * Metadata used by JSON documents retained from the previous CouchDB-based
 * representation. New persistence code must not depend on these fields.
 */
public enum LegacyDocumentField implements DbField {

    TYPE_FIELD("type"),
    REVISION_FIELD("_rev"),
    ID_FIELD("_id");

    private final String dbName;
    private final String[] fieldPath;

    LegacyDocumentField(String dbName) {
        this.dbName = dbName;
        this.fieldPath = new String[] {dbName};
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    @Override
    public String[] getFieldPath() {
        return this.fieldPath.clone();
    }
}