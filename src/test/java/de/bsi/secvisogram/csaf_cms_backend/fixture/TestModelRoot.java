package de.bsi.secvisogram.csaf_cms_backend.fixture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Model to test CouchDB access
 * Root object with primitive attributes fields, array field and subfields
 */
public class TestModelRoot {


    public static final String FIRST_STRING = "firstString";
    public static final String SECOND_STRING = "secondString";
    public static final String DECIMAL_VALUE = "decimalValue";
    public static final String BOOLEAN_VALUE = "booleanValue";

    public static final String FIRST_LEVEL = "firstLevel";

    public static final String ARRAY_VALUES = "arrayValues";

    public static final String[] ARRAY_FIELD_SELECTOR = {ARRAY_VALUES};

    public static final List<String> ROOT_PRIMITIVE_FIELDS = Arrays.asList(FIRST_STRING, SECOND_STRING,
            DECIMAL_VALUE, BOOLEAN_VALUE);
    private String firstString;

    private String secondString;

    private Double decimalValue;

    private Boolean booleanValue;

    private final TestModelFirstLevel firstLevel = new TestModelFirstLevel();


    private final List<TestModelArray> arrayValues = new ArrayList<>();

    public String getFirstString() {
        return firstString;
    }

    public TestModelRoot setFirstString(String firstString) {
        this.firstString = firstString;
        return this;
    }

    public String getSecondString() {
        return secondString;
    }

    public TestModelRoot setSecondString(String secondString) {
        this.secondString = secondString;
        return this;
    }

    public Double getDecimalValue() {
        return decimalValue;
    }

    public TestModelRoot setDecimalValue(Double decimalValue) {
        this.decimalValue = decimalValue;
        return this;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public TestModelRoot setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
        return this;
    }

    public TestModelFirstLevel getFirstLevel() {
        return firstLevel;
    }

    public TestModelRoot setLevelValues(String firstLevelValue, String secondLevelValue) {
        this.firstLevel.setLevel1Value(firstLevelValue);
        this.firstLevel.getSecondLevel().setLevel2Value(secondLevelValue);
        return this;
    }

    public List<TestModelArray> getArrayValues() {
        return Collections.unmodifiableList(arrayValues);
    }

    public TestModelRoot addListValues(String ... listValues) {
        Arrays.stream(listValues)
                .forEach(value -> this.arrayValues.add(new TestModelArray().setEntryValue(value)));
        return this;
    }
}
