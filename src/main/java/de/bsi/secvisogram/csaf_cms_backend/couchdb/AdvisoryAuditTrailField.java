package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public enum AdvisoryAuditTrailField implements DbField {

    ADVISORY_ID("advisoryId"),
    OLD_DOC_VERSION("oldDocVersion"),
    DOC_VERSION("docVersion"),
    DIFF("diff"),

    OLD_WORKFLOW_STATE("oldState"),
    NEW_WORKFLOW_STATE("newState");

    private final String dbName;
    private final String[] fieldPath;

    AdvisoryAuditTrailField(String dbName) {
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
