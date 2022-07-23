package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public enum AdvisoryField implements DbField {

    WORKFLOW_STATE("workflowState"),
    OWNER("owner"),
    CSAF("csaf"),
    VERSIONING_TYPE("versioningType"),
    LAST_VERSION("lastMajorVersion");

    private final String dbName;
    private final String[] fieldPath;

    AdvisoryField(String dbName) {
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
