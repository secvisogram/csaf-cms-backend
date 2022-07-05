package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public enum CommentField implements DbField {

    TEXT("commentText"),
    OWNER("owner"),
    ADVISORY_ID("advisoryId"),
    CSAF_NODE_ID("csafNodeId"),
    FIELD_NAME("fieldName"),
    ANSWER_TO("answerTo");

    private final String dbName;
    private final String[] fieldPath;

    CommentField(String dbName) {
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
