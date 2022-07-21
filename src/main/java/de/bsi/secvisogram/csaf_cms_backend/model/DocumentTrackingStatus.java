package de.bsi.secvisogram.csaf_cms_backend.model;

/**
 * CSAF document tracking status
 */
public enum DocumentTrackingStatus {

    Draft("draft"),
    Final("final"),
    Interim("interim");

    private final String csafValue;

    DocumentTrackingStatus(String csafValue) {
        this.csafValue = csafValue;
    }

    public String getCsafValue() {
        return csafValue;
    }
}
