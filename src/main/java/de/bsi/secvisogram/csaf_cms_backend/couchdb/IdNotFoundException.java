package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public class IdNotFoundException extends DatabaseException {

    public IdNotFoundException(String message) {
        super(message);
    }

    public IdNotFoundException(Throwable cause) {
        super(cause);
    }

    public IdNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
