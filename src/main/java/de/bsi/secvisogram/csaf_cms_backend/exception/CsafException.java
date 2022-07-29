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

    public CsafException(String message, CsafExceptionKey exceptionKey, HttpStatus recommendedHttpState) {
        super(message);
        this.exceptionKey = exceptionKey;
        this.recommendedHttpState = recommendedHttpState;
    }

    public CsafException(Throwable cause, CsafExceptionKey exceptionKey, HttpStatus httpState) {
        super(cause);
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
