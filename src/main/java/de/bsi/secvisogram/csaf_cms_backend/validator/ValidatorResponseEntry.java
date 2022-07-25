package de.bsi.secvisogram.csaf_cms_backend.validator;

/**
 * Entry in the test part response of to the CSAF validation service
 */
public class ValidatorResponseEntry {

    private String instancePath;
    private String message;

    public String getInstancePath() {
        return instancePath;
    }

    public String getMessage() {
        return message;
    }

    public ValidatorResponseEntry setInstancePath(String instancePath) {

        this.instancePath = instancePath;
        return this;
    }

    public ValidatorResponseEntry setMessage(String message) {

        this.message = message;
        return this;
    }
}
