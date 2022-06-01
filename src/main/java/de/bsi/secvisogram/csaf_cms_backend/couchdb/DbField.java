package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.cloud.cloudant.v1.model.Document;

public interface DbField {

    String getDbName();
    String[] getFieldPath();

    default Object val(Document couchDbDoc) {
        return couchDbDoc.get(this.getDbName());
    }

    default String stringVal(Document couchDbDoc) {
        return couchDbDoc.get(this.getDbName()).toString();
    }

    default String stringVal(JsonNode couchDbDoc) {
        return couchDbDoc.get(this.getDbName()).asText();
    }

}
