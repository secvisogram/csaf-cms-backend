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
        return tests;
    }
}
