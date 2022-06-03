package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public enum CommentAuditTrailField implements DbField {

    COMMENT_ID("commentId");

    private final String dbName;
    private final String[] fieldPath;

    CommentAuditTrailField(String dbName) {
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
