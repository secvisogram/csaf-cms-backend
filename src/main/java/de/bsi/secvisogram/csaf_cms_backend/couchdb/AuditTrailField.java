package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public enum AuditTrailField implements DbField {

    ADVISORY_ID("advisoryId"),
    DOC_VERSION("docVersion"),
    CREATED_AT("createdAt"),
    DIFF("diff"),
    OLD_DOC_VERSION("oldDocVersion"),
    USER("user"),

    CHANGE_TYPE("changeType"),

    OLD_WORKFLOW_STATE("oldState"),

    NEW_WORKFLOW_STATE("newState")
    ;

    private final String dbName;
    private final String[] fieldPath;

    AuditTrailField(String dbName) {
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
