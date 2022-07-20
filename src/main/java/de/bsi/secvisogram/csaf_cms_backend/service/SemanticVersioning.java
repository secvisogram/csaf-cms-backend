package de.bsi.secvisogram.csaf_cms_backend.service;

import com.vdurmont.semver4j.Semver;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.VersioningType;

/**
 * Implements semantic versioning
 */
public class SemanticVersioning implements Versioning {

    private static final String INITIAL_VERSION = "0.0.1";

    private static final SemanticVersioning DEFAULT_INSTANCE = new SemanticVersioning();

    public static SemanticVersioning getDefault() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public VersioningType getVersioningType() {
        return VersioningType.Semantic;
    }


    @Override
    public String getInitialVersion() {
        return INITIAL_VERSION;
    }

    /**
     * Get next version for the workflow change to approve
     * @param advisoryNode The advisory to update the version
     * @return The new version
     */
    @Override
    public String getNextApprovedVersion(AdvisoryWrapper advisoryNode) {

        String currentVersionString = advisoryNode.getDocumentTrackingVersion();
        return getNextApprovedVersion(currentVersionString);
    }

    String getNextApprovedVersion(String currentVersionString) {
        Semver oldVersion = new Semver(currentVersionString);
        Semver newVersion = oldVersion;
        if (oldVersion.getMajor() < 1) {
            newVersion = oldVersion.nextMajor();
        }

        String newSuffix = increaseSuffixMajorVersion(newVersion);
        return newVersion.withSuffix(newSuffix).toString();
    }

    /**
     * Remove teh version suffix from the advisory version
     * @param advisoryNode The advisory to remove the suffix
     * @return Teh new version
     */
    @Override
    public String removeVersionSuffix(AdvisoryWrapper advisoryNode) {

        String currentVersionString = advisoryNode.getDocumentTrackingVersion();
        return removeVersionSuffix(currentVersionString);
    }

    String removeVersionSuffix(String currentVersionString) {

        Semver oldVersion = new Semver(currentVersionString);
        return oldVersion.withClearedSuffixAndBuild().toString();
    }

    @Override
    public String getNewDocumentVersion(AdvisoryWrapper advisoryNode) {

        return this.getNewDocumentVersion(advisoryNode.getDocumentTrackingVersion());
    }

    public String getNewDocumentVersion(String currentVersionString) {

        Semver oldVersion = new Semver(currentVersionString);
        return oldVersion.nextPatch().toString();
    }

    @Override
    public String getNextVersion(AdvisoryWrapper oldAdvisoryNode, PatchType changeType, int lastMajor) {

        String currentVersionString = oldAdvisoryNode.getDocumentTrackingVersion();
        return getNextVersion(changeType, currentVersionString, lastMajor);
    }

    String getNextVersion(PatchType changeType, String currentVersionString, int lastMajor) {

        Semver oldVersion = new Semver(currentVersionString);
        Semver result;
        if (oldVersion.getMajor() == 0) {
            if (changeType == PatchType.MAJOR) {
                result = oldVersion.nextMinor();
            } else {
                result = oldVersion.nextPatch();
            }
        } else if (oldVersion.getMajor() == 1 && oldVersion.getMajor() == 0 && oldVersion.getPatch() == 0) {
            String newSuffix = increaseSuffixMinorVersion(oldVersion);
            result = oldVersion.withSuffix(newSuffix);
        } else {
            String newSuffix = increaseSuffixMinorVersion(oldVersion);
            Semver nextVersion = oldVersion;
            if (changeType == PatchType.MAJOR && (oldVersion.getMajor() == lastMajor)) {
                nextVersion = oldVersion.nextMajor();
            } else if (changeType == PatchType.MINOR && oldVersion.getMajor() == lastMajor &&
                    oldVersion.getMinor() == 0) {
                nextVersion = oldVersion.nextMinor();
            } else if (changeType == PatchType.PATCH && oldVersion.getMajor() == lastMajor &&
                    oldVersion.getMinor() == 0 && oldVersion.getPatch() == 0) {
                nextVersion = oldVersion.nextPatch();
            }
            result = nextVersion.withSuffix(newSuffix);
        }
        return result.toString();
    }

    private String increaseSuffixMinorVersion(Semver currentVersion) {

        String newSuffix = "1.0";
        if (currentVersion.getSuffixTokens().length > 1) {
            String token = currentVersion.getSuffixTokens()[1];
            int tokenValue = Integer.parseInt(token) + 1;
            newSuffix = currentVersion.getSuffixTokens()[0] + "." + tokenValue;
        }
        return newSuffix;
    }

    private String increaseSuffixMajorVersion(Semver currentVersion) {

        String newSuffix = "1.0";
        if (currentVersion.getSuffixTokens().length > 1) {
            String token = currentVersion.getSuffixTokens()[0];
            int newMajor = Integer.parseInt(token) + 1;
            newSuffix = newMajor + "." + 0;
        }
        return newSuffix;
    }
}
