package de.bsi.secvisogram.csaf_cms_backend.validator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request to the csaf validation service
 *
 */
public class ValidationRequest {

    private ValidationRequestTest[] tests;
    /** CSAF document */
    private JsonNode document;

    public ValidationRequest(JsonNode document, ValidationRequestTest... tests) {
        this.tests = tests;
        this.document = document;
    }

    public ValidationRequestTest[] getTests() {
        return tests.clone();
    }

    public JsonNode getDocument() {
        return document;
    }
}
