package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.ChangeType;

public class DocumentChangeResponse extends ChangeResponse {

    ChangeType changeType;
    String diffAsJsonPatch;

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getDiffAsJsonPatch() {
        return diffAsJsonPatch;
    }
}
