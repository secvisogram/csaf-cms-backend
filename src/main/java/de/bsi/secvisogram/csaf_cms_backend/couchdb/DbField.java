package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import com.ibm.cloud.cloudant.v1.model.Document;
import tools.jackson.databind.JsonNode;

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
        return couchDbDoc.get(this.getDbName()).asString();
    }

}
