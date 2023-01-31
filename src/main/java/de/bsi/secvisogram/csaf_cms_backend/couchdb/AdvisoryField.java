package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public enum AdvisoryField implements DbField {

    WORKFLOW_STATE("workflowState"),
    OWNER("owner"),
    CSAF("csaf"),
    /** semantic or integer */
    VERSIONING_TYPE("versioningType"),
    LAST_VERSION("lastMajorVersion"),
    /** reference form AdvisoryVersion to source advisory */
    ADVISORY_REFERENCE("advisoryReference"),
    /** A temporary tracking ID is assigned to the CSAF document during the creation process.
     * The final ID is assigned during publishing
     * It must be traceable which TEMP ID became which final ID.
     * Therefore, the temp id is stored in the metadata after publishing.*/
    TMP_TRACKING_ID("tmpTrackingId");

    private final String dbName;
    private final String[] fieldPath;

    AdvisoryField(String dbName) {
        this.dbName = dbName;
        this.fieldPath = new String[] {dbName};
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    @Override
    public String[] getFieldPath() {
        return this.fieldPath.clone();
    }
}
