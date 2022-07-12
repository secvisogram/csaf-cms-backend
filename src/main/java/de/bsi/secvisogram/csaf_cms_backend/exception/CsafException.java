package de.bsi.secvisogram.csaf_cms_backend.exception;

import org.springframework.http.HttpStatus;

public class CsafException extends Exception {

    private final CsafExceptionKey exceptionKey;
    private final HttpStatus recommendedHttpState;

    public CsafException(String message, CsafExceptionKey exceptionKey) {
        super(message);
        this.exceptionKey = exceptionKey;
        this.recommendedHttpState = HttpStatus.NOT_FOUND;
    }

    public CsafException(Throwable cause, CsafExceptionKey exceptionKey, HttpStatus httpState) {
        super(cause);
        this.exceptionKey = exceptionKey;
        this.recommendedHttpState = httpState;
    }

    public CsafException(String message, Throwable cause, CsafExceptionKey exceptionKey, HttpStatus httpState) {
        super(message, cause);
        this.exceptionKey = exceptionKey;
        this.recommendedHttpState = httpState;
    }

    public HttpStatus getRecommendedHttpState() {
        return recommendedHttpState;
    }

    public CsafExceptionKey getExceptionKey() {
        return exceptionKey;
    }
}
