package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public enum AdvisorySearchField implements DbField {

    DOCUMENT_TITLE("csaf", "document", "title"),
    DOCUMENT_TRACKING_ID("csaf", "document", "tracking", "id"),

    DOCUMENT_TRACKING_VERSION("csaf", "document", "tracking", "version");


    private final String dbName;
    private final String[] fieldPath;

    AdvisorySearchField(String... dbNamePath) {
        this.dbName = String.join(".", dbNamePath);
        this.fieldPath = dbNamePath;
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
