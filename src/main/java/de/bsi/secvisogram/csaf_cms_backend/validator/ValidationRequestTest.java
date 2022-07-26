package de.bsi.secvisogram.csaf_cms_backend.validator;

/**
 * Tests part of the request to the csaf validation service
 */
public class ValidationRequestTest {

    private String type;
    private String name;

    public ValidationRequestTest(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public ValidationRequestTest setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public ValidationRequestTest setName(String name) {
        this.name = name;
        return this;
    }
}
