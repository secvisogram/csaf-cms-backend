package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public enum AdvisorySearchField implements DbField {

    DOCUMENT("csaf", "document"),
    DOCUMENT_TITLE("csaf", "document", "title"),
    DOCUMENT_TRACKING_ID("csaf", "document", "tracking", "id"),

    DOCUMENT_TRACKING_VERSION("csaf", "document", "tracking", "version"),

    DOCUMENT_TRACKING_STATUS("csaf", "document", "tracking", "status"),

    DOCUMENT_TRACKING_GENERATOR_ENGINE_NAME("csaf", "document", "tracking", "generator", "engine", "name"),

    DOCUMENT_TRACKING_GENERATOR_ENGINE_VERSION("csaf", "document", "tracking", "generator", "engine", "version"),

    DOCUMENT_TRACKING_CURRENT_RELEASE_DATE("csaf", "document", "tracking", "current_release_date"),

    DOCUMENT_TRACKING_INITIAL_RELEASE_DATE("csaf", "document", "tracking", "initial_release_date");


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
