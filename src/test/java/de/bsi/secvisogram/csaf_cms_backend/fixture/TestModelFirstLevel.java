package de.bsi.secvisogram.csaf_cms_backend.fixture;

/**
 * Model to test CouchDB access
 * subfields level 1 from root
 */
public class TestModelFirstLevel {

    public static final String SECOND_LEVEL = "secondLevel";

    private String level1Value;

    private final TestModelSecondLevel secondLevel = new TestModelSecondLevel();



    public String getLevel1Value() {
        return level1Value;
    }

    public TestModelFirstLevel setLevel1Value(String level1Value) {
        this.level1Value = level1Value;
        return this;
    }

    public TestModelSecondLevel getSecondLevel() {
        return secondLevel;
    }
}
