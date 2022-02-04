package de.exxcellent.bsi.rest.response;

import de.exxcellent.bsi.model.ChangeType;

public class DocumentChangeResponse extends AuditTrailEntryResponse {

    ChangeType changeType;
    String diffAsJsonPatch;
}
