package de.bsi.secvisogram.csaf_cms_backend.json;

import de.bsi.secvisogram.csaf_cms_backend.couchdb.AdvisoryAuditTrailField;
import tools.jackson.databind.node.ObjectNode;

public class AdvisoryAuditTrailWrapper extends AuditTrailWrapper {

    public AdvisoryAuditTrailWrapper(ObjectNode auditTrailNode) {
        super(auditTrailNode);
    }


    public String getAdvisoryId() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.ADVISORY_ID.getDbName()).asString();
    }

    public String getDocVersion() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.DOC_VERSION.getDbName()).asString();
    }

    public String getOldDocVersion() {

        return this.getAuditTrailNode().get(AdvisoryAuditTrailField.OLD_DOC_VERSION.getDbName()).asString();
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
