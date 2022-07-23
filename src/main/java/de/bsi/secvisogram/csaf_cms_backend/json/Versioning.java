package de.bsi.secvisogram.csaf_cms_backend.json;

import de.bsi.secvisogram.csaf_cms_backend.service.PatchType;

public interface Versioning {

    public static Versioning getStrategy(String versioningStrategy) {

        VersioningType type = VersioningType.valueOf(versioningStrategy);
        if (type == VersioningType.Semantic) {
            return SemanticVersioning.getDefault();
        } else {
            return IntegerVersioning.getDefault();
        }
    }
    VersioningType getVersioningType();

    String getInitialVersion();

    /**
     * Get next version for the workflow change to approve
     * @param currentVersionString The advisory to update the version
     * @return the new version
     */
    String getNextApprovedVersion(String currentVersionString);

    /**
     * Remove the version suffix from the advisory version
     * @param currentVersionString The advisory version to remove the suffix
     * @return the new version
     */
    String removeVersionSuffix(String currentVersionString);

    /**
     * Get a new version when saving a advisory
     * @param changeType the type of change
     * @param currentVersionString
     * @param lastReleaseVersion
     * @return the new version
     */
    String getNextVersion(PatchType changeType, String currentVersionString, String lastReleaseVersion);

    public String getNewDocumentVersion(String currentVersionString);
}
