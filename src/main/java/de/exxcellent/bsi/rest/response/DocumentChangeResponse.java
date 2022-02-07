package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.ChangeType;

import java.time.LocalDate;

public class DocumentChangeResponse extends ChangeResponse {

    private final ChangeType changeType;
    private final String diffAsJsonPatch;

    public DocumentChangeResponse(long advisoryId, String advisoryVersion, String userId, LocalDate createdAt
            , ChangeType changeType, String diffAsJsonPatch) {
        super(advisoryId, advisoryVersion, userId, createdAt);
        this.changeType = changeType;
        this.diffAsJsonPatch = diffAsJsonPatch;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getDiffAsJsonPatch() {
        return diffAsJsonPatch;
    }
}
