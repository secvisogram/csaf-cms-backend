package de.bsi.secvisogram.csaf_cms_backend.validator;

/**
 * Response of to the CSAF validation service
 *
 */
public class ValidatorResponse {

    private boolean isValid;
    private ValidatorResponseTest[] tests;

    public boolean isValid() {
        return isValid;
    }

    public ValidatorResponseTest[] getTests() {
        return tests.clone();
    }

    public ValidatorResponse setValid(boolean valid) {
        isValid = valid;
        return this;
    }

    public ValidatorResponse setTests(ValidatorResponseTest[] tests) {
        this.tests = tests.clone();
        return this;
    }
}
