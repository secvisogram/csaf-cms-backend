package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryAuditTrailField;

public class AdvisoryAuditTrailWrapper extends AuditTrailWrapper {

    public AdvisoryAuditTrailWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }


    public String getAdvisoryId() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.ADVISORY_ID.getDbName()).asText();
    }

    public String getDocVersion() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.DOC_VERSION.getDbName()).asText();
    }

    public String getOldDocVersion() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.OLD_DOC_VERSION.getDbName()).asText();
    }

    public AdvisoryAuditTrailWrapper setAdvisoryId(String newValue) {

        this.getAuditTrailNode().put(AdvisoryAuditTrailField.ADVISORY_ID.getDbName(), newValue);
        return this;
    }

    public AdvisoryAuditTrailWrapper setDocVersion(String newValue) {

        this.getAuditTrailNode().put(AdvisoryAuditTrailField.DOC_VERSION.getDbName(), newValue);
        return this;
    }

    public AdvisoryAuditTrailWrapper setOldDocVersion(String newValue) {

        this.getAuditTrailNode().put(AdvisoryAuditTrailField.OLD_DOC_VERSION.getDbName(), newValue);
        return this;
    }


}
