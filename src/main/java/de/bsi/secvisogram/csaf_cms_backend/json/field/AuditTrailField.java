package de.bsi.secvisogram.csaf_cms_backend.json.field;

public enum AuditTrailField implements DbField {

    CREATED_AT("createdAt"),
    USER("user"),
    CHANGE_TYPE("changeType");

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
