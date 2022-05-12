package de.bsi.secvisogram.csaf_cms_backend.fixture;

import java.util.Arrays;
import java.util.List;

public class TestModelRoot {

    public static final List<String> ROOT_PRIMITIVE_FIELDS = Arrays.asList("firstString", "secondString"
            , "decimalValue", "booleanValue");
    private String firstString;
    private String secondString;

    private Double decimalValue;

    private Boolean booleanValue;

    private final TestModelFirstLevel firstlevel = new TestModelFirstLevel();


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

    public TestModelFirstLevel getFirstlevel() {
        return firstlevel;
    }

    public void setLevelValues(String firstLevelValue, String secondLevelValue) {
        this.firstlevel.setLevel1Value(firstLevelValue);
        this.firstlevel.getSecondLevel().setLevel2Value(secondLevelValue);
    }
}
