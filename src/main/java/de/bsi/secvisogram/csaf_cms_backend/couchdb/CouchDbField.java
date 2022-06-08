package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public enum CouchDbField implements DbField {

    TYPE_FIELD("type"),
    REVISION_FIELD("_rev"),
    ID_FIELD("_id");

    private final String dbName;
    private final String[] fieldPath;

    CouchDbField(String dbName) {
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
