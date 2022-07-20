package de.bsi.secvisogram.csaf_cms_backend.service;

import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import de.bsi.secvisogram.csaf_cms_backend.json.VersioningType;

public interface Versioning {

    static Versioning getStrategy(String versioningStrategy) {

        VersioningType type = VersioningType.valueOf(versioningStrategy);
        if (type == null || type == VersioningType.Semantic) {
            return SemanticVersioning.getDefault();
        } else {
            return IntegerVersioning.getDefault();
        }
    }

    VersioningType getVersioningType();

    String getInitialVersion();
    String getNextApprovedVersion(AdvisoryWrapper advisoryNode);

    String removeVersionSuffix(AdvisoryWrapper advisoryNode);

    String getNextVersion(AdvisoryWrapper oldAdvisoryNode, PatchType changeType, int lastMajor);

    String getNewDocumentVersion(AdvisoryWrapper existingAdvisoryNode);
}
