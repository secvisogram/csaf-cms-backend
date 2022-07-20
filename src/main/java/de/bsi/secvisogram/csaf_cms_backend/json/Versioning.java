package de.bsi.secvisogram.csaf_cms_backend.json;

import de.bsi.secvisogram.csaf_cms_backend.service.PatchType;

public interface Versioning {

    public static Versioning getStrategy(String versioningStrategy) {

        VersioningType type = VersioningType.valueOf(versioningStrategy);
        if (type == null || type == VersioningType.Semantic) {
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
     * @return The new version
     */
    String getNextApprovedVersion(String currentVersionString);

    /**
     * Remove the version suffix from the advisory version
     * @param currentVersionString The advisory version to remove the suffix
     * @return Teh new version
     */
    String removeVersionSuffix(String currentVersionString);

    String getNextVersion(PatchType changeType, String currentVersionString, int lastMajor);

    public String getNewDocumentVersion(String currentVersionString);
}
