package de.bsi.secvisogram.csaf_cms_backend.fixture;

/**
 * Model to test CouchDB access
 * subfields level 2 from root
 */
public class TestModelSecondLevel {

    public static final String LEVEL_2_VALUE = "level2Value";

    private String level2Value;


    public String getLevel2Value() {
        return level2Value;
    }

    public TestModelSecondLevel setLevel2Value(String level2Value) {
        this.level2Value = level2Value;
        return this;
    }
}
