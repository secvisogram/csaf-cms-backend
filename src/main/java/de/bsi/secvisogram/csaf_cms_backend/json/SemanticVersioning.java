package de.bsi.secvisogram.csaf_cms_backend.json;

import com.vdurmont.semver4j.Semver;
import de.bsi.secvisogram.csaf_cms_backend.service.PatchType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Implements semantic versioning
 */
@SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "Class has no internal state")
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

    public String getNextApprovedVersion(String currentVersionString) {
        Semver oldVersion = new Semver(currentVersionString);
        Semver newVersion = oldVersion;
        if (oldVersion.getMajor() < 1) {
            newVersion = oldVersion.nextMajor();
        }

        String newSuffix = increaseSuffixMajorVersion(newVersion);
        return newVersion.withSuffix(newSuffix).toString();
    }


    @Override
    public String removeVersionSuffix(String currentVersionString) {

        Semver oldVersion = new Semver(currentVersionString);
        return oldVersion.withClearedSuffixAndBuild().toString();
    }

    @Override
    public String getNewDocumentVersion(String currentVersionString) {

        Semver oldVersion = new Semver(currentVersionString);
        return oldVersion.nextPatch().toString();
    }

    @Override
    public String getNextVersion(PatchType changeType, String currentVersionString, int lastMajor) {

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

    @SuppressFBWarnings(value = "CLI_CONSTANT_LIST_INDEX", justification = "Suffix index is fix")
    private String increaseSuffixMinorVersion(Semver currentVersion) {

        String newSuffix = "1.0";
        if (currentVersion.getSuffixTokens().length > 1) {
            String token = currentVersion.getSuffixTokens()[1];
            int tokenValue = Integer.parseInt(token) + 1;
            newSuffix = currentVersion.getSuffixTokens()[0] + "." + tokenValue;
        }
        return newSuffix;
    }

    @SuppressFBWarnings(value = "CLI_CONSTANT_LIST_INDEX", justification = "Suffix index is fix")
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
