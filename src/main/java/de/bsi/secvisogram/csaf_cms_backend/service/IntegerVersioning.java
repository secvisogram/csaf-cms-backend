package de.bsi.secvisogram.csaf_cms_backend.service;

import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.VersioningType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public String getNextApprovedVersion(AdvisoryWrapper advisoryNode) {

        String oldVersion = advisoryNode.getDocumentTrackingVersion();
        return this.getNextApprovedVersion(oldVersion);
    }

    String getNextApprovedVersion(String  oldVersion) {

        return oldVersion.equals("0") ? "1" : oldVersion;
    }

    @Override
    public String removeVersionSuffix(AdvisoryWrapper advisoryNode) {

        return removeVersionSuffix(advisoryNode.getDocumentTrackingVersion());
    }

    String removeVersionSuffix(String  oldVersion) {

        return oldVersion;
    }

    @Override
    public String getNextVersion(AdvisoryWrapper advisoryNode, PatchType changeType, int lastMajor) {

        return getNextVersion(advisoryNode.getDocumentTrackingVersion());
    }

    String getNextVersion(String currentVersionString) {
        return currentVersionString;
    }

    @Override
    public String getNewDocumentVersion(AdvisoryWrapper advisoryNode) {

        int oldVersion = 0;
        try {
            oldVersion = Integer.parseInt(advisoryNode.getDocumentTrackingVersion());
        } catch (NumberFormatException ex) {
            LOG.error("Invalid Versioning Format", ex);
        }
        return "" + (oldVersion + 1);
    }
}
