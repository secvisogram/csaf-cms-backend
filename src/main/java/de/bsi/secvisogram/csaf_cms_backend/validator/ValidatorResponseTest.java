package de.bsi.secvisogram.csaf_cms_backend.validator;

/**
 * Test part of the response of to the CSAF validation service
 */
public class ValidatorResponseTest {

    private ValidatorResponseEntry[] errors;
    private ValidatorResponseEntry[] infos;
    private ValidatorResponseEntry[] warnings;
    private boolean isValid;
    private String name;

    public ValidatorResponseEntry[] getErrors() {
        return errors;
    }

    public ValidatorResponseEntry[] getInfos() {
        return infos;
    }

    public ValidatorResponseEntry[] getWarnings() {
        return warnings;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getName() {
        return name;
    }
}
