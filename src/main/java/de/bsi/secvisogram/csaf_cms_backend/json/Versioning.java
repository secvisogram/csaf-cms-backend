package de.bsi.secvisogram.csaf_cms_backend.json;

import de.bsi.secvisogram.csaf_cms_backend.service.PatchType;

public interface Versioning {

    /**
     * Detect  Version Type (semantic or integer) from document tracking version
     * @param trackingVersion document tracking version
     * @return
     */
    public static Versioning detectStrategy(String trackingVersion) {

        VersioningType type;
        // semantic is  default, we assume semantic when the version string contains a dot
        if (trackingVersion == null || trackingVersion.isBlank() || containsChar(trackingVersion, '.')) {
            type = VersioningType.Semantic;
        } else {
            type = VersioningType.Integer;
        }

        if (type == VersioningType.Semantic) {
            return SemanticVersioning.getDefault();
        } else {
            return IntegerVersioning.getDefault();
        }
    }

    static boolean containsChar(String stringToTest, char charToDetect) {
        return stringToTest != null && stringToTest.indexOf(charToDetect) >= 0;
    }

    public static Versioning getStrategy(String versioningStrategy) {

        VersioningType type;
        try {
            type = (versioningStrategy != null) ? VersioningType.valueOf(versioningStrategy) : VersioningType.Semantic;
        } catch (IllegalArgumentException ex) {
            type = VersioningType.Semantic;
        }
        if (type == VersioningType.Semantic) {
            return SemanticVersioning.getDefault();
        } else {
            return IntegerVersioning.getDefault();
        }
    }
    VersioningType getVersioningType();

    String getInitialVersion();

    String getZeroVersion();

    /**
     * Get next version for the workflow change to approve
     * @param currentVersionString The advisory to update the version
     * @return the new version
     */
    String getNextApprovedVersion(String currentVersionString);

    /**
     * Get next version for the workflow change to draft
     * @param currentVersionString The advisory to update the version
     * @return the new version
     */
    String getNextDraftVersion(String currentVersionString);

    /**
     * Remove the version suffix from the advisory version
     * @param currentVersionString The advisory version to remove the suffix
     * @return the new version
     */
    String removeVersionSuffix(String currentVersionString);

    /**
     * Get a new version when saving a advisory
     * @param changeType the type of change
     * @param currentVersionString the version of the current advisory
     * @param lastReleaseVersion the version of the last published advisory version
     * @return the new version
     */
    String getNextVersion(PatchType changeType, String currentVersionString, String lastReleaseVersion);

    public String getNewDocumentVersion(String currentVersionString);
}
