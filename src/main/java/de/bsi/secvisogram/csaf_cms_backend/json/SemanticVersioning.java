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

    @Override
    public String getZeroVersion() {
        return "0.0.0";
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
    public String getNextDraftVersion(String currentVersionString) {

        Semver oldVersion = new Semver(currentVersionString);
        if (isInitialPublicReleaseOrEarlier(oldVersion)) {
            String newSuffix = increaseSuffixMinorVersion(oldVersion);
            return oldVersion.withSuffix(newSuffix).toString();
        } else {
            String newSuffix = increaseSuffixMajorVersion(oldVersion);
            return oldVersion.withSuffix(newSuffix).toString();
        }
    }

    public boolean isInitialPublicReleaseOrEarlier(Semver version) {

        return (version.getMajor() < 1)
                || ((version.getMajor() == 1) && (version.getMinor() == 0) && (version.getPatch() == 0));
    }

    public boolean isPrerelease(Semver version) {

        return (version.getMajor() < 1) || version.getSuffixTokens().length > 0;
    }


    @Override
    public String removeVersionSuffix(String currentVersionString) {

        Semver oldVersion = new Semver(currentVersionString);
        return oldVersion.withClearedSuffixAndBuild().toString();
    }

    @Override
    public String getNewDocumentVersion(String currentVersionString) {

        Semver oldVersion = new Semver(currentVersionString);
        return oldVersion.nextPatch().withSuffix("1.0").toString();
    }

    @Override
    public String getNextVersion(PatchType changeType, String currentVersionString, String lastVersionString) {

        Semver lastVersion = new Semver(lastVersionString);
        Semver oldVersion = new Semver(currentVersionString);
        Semver result;
        if (oldVersion.getMajor() == 0) {
            //prerelease
            if (changeType == PatchType.MAJOR) {
                result = oldVersion.nextMinor();
            } else {
                result = oldVersion.nextPatch();
            }
        } else if (oldVersion.getMajor() == 1 && oldVersion.getMinor() == 0 && oldVersion.getPatch() == 0) {
            // prerelease approved
            String newSuffix = increaseSuffixMinorVersion(oldVersion);
            result = oldVersion.withSuffix(newSuffix);
        } else {
            String newSuffix = increaseSuffixMinorVersion(oldVersion);
            Semver nextVersion = oldVersion;
            if (changeType == PatchType.MAJOR && (oldVersion.getMajor().equals(lastVersion.getMajor()))) {
                nextVersion = oldVersion.nextMajor();
            } else if (changeType == PatchType.MINOR && oldVersion.getMajor().equals(lastVersion.getMajor()) &&
                    oldVersion.getMinor().equals(lastVersion.getMinor())) {
                nextVersion = oldVersion.nextMinor();
            } else if (changeType == PatchType.PATCH && oldVersion.getMajor().equals(lastVersion.getMajor()) &&
                    oldVersion.getMinor().equals(lastVersion.getMinor()) && oldVersion.getPatch().equals(lastVersion.getPatch())) {
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
