package de.bsi.secvisogram.csaf_cms_backend.rest.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError")
public class ApiError {

    private final String message;

    public ApiError(String message) {
        this.message = message;
    }

    @Schema(description = "A human-readable description of the error.", example = "Advisory is not in state final or interim")
    public String getMessage() {
        return message;
    }
}
