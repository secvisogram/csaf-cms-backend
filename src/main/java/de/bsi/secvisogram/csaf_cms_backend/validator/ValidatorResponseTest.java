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
        return errors.clone();
    }

    public ValidatorResponseEntry[] getInfos() {
        return infos.clone();
    }

    public ValidatorResponseEntry[] getWarnings() {
        return warnings.clone();
    }

    public boolean isValid() {
        return isValid;
    }

    public String getName() {
        return name;
    }

    public ValidatorResponseTest setErrors(ValidatorResponseEntry[] errors) {
        this.errors = errors.clone();
        return this;
    }

    public ValidatorResponseTest setInfos(ValidatorResponseEntry[] infos) {
        this.infos = infos.clone();
        return this;
    }

    public ValidatorResponseTest setWarnings(ValidatorResponseEntry[] warnings) {
        this.warnings = warnings.clone();
        return this;
    }

    public ValidatorResponseTest setValid(boolean valid) {
        isValid = valid;
        return this;
    }

    public ValidatorResponseTest setName(String name) {
        this.name = name;
        return this;
    }
}
