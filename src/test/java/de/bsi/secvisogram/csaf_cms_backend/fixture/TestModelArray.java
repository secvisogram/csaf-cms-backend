package de.bsi.secvisogram.csaf_cms_backend.fixture;

/**
 * Model to test CouchDB access
 * object in an array in root
 */
public class TestModelArray {

    public static final String ENTRY_VALUE = "entryValue";

    private String entryValue;

    public String getEntryValue() {
        return entryValue;
    }

    public TestModelArray setEntryValue(String entryValue) {
        this.entryValue = entryValue;
        return this;
    }
}
