package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

/** A filter of the paged advisory list uses a field or value that is not backed by a column. */
public class UnsupportedFilterException extends RuntimeException {

    public UnsupportedFilterException(String message) {
        super(message);
    }
}
