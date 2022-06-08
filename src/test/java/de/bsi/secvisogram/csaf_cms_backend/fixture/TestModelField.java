package de.bsi.secvisogram.csaf_cms_backend.fixture;

import de.bsi.secvisogram.csaf_cms_backend.couchdb.DbField;

public enum TestModelField implements DbField {

    FIRST_STRING("firstString"),
    SECOND_STRING("secondString"),
    DECIMAL_VALUE("decimalValue"),

    BOOLEAN_VALUE("booleanValue"),

    FIRST_LEVEL("firstLevel"),
    ARRAY_VALUES("arrayValues");

    private final String dbName;
    private final String[] fieldPath;

    TestModelField(String dbName) {
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
