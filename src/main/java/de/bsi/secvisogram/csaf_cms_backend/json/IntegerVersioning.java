package de.bsi.secvisogram.csaf_cms_backend.json;

import de.bsi.secvisogram.csaf_cms_backend.service.PatchType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "Class has no internal state")
public class IntegerVersioning implements Versioning {

    private static final Logger LOG = LoggerFactory.getLogger(IntegerVersioning.class);

    private static final IntegerVersioning DEFAULT_INSTANCE = new IntegerVersioning();

    public static IntegerVersioning getDefault() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public VersioningType getVersioningType() {
        return VersioningType.Integer;
    }

    @Override
    public String getInitialVersion() {
        return "0";
    }

    @Override
    public String getZeroVersion() {
        return "0";
    }

    @Override
    public String getNextApprovedVersion(String  oldVersion) {

        return ("0").equals(oldVersion) ? "1" : oldVersion;
    }

    @Override
    public String getNextDraftVersion(String currentVersionString) {
        return currentVersionString;
    }

    @Override
    public String removeVersionSuffix(String  oldVersion) {

        return oldVersion;
    }

    @Override
    public String getNextVersion(PatchType changeType, String currentVersionString, String lastMajor) {
        return currentVersionString;
    }

    @Override
    /**
     * Get next version string
     * @param currentVersionString
     * @return current version + 1 or "1" if currentVersionString is not a number. 
     */
    public String getNewDocumentVersion(String currentVersionString) {

        int oldVersion = 0;
        try {
            oldVersion = Integer.parseInt(currentVersionString);
        } catch (NumberFormatException ex) {
        }
        return "" + (oldVersion + 1);
    }
}
