package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Counter Object in the CouchDB for the tracking id
 * For every ObjectId there should only exist one counter object in the db
 */
public class TrackingIdCounter {

    public static final String TMP_OBJECT_ID = "TMP_TRACKING_ID_COUNTER";
    public static final String FINAL_OBJECT_ID = "FINAL_TRACKING_ID_COUNTER";

    @JsonProperty("_id")
    private final String id;
    @JsonProperty("_rev")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String rev;
    @JsonProperty("type")
    private final String type;
    @JsonProperty("count")
    private long count;

    public static TrackingIdCounter createInitialCounter(String id) {
        return new TrackingIdCounter(id);
    }

    private TrackingIdCounter(String id) {
        this.id = id;
        this.rev = null;
        this.type = ObjectType.Counter.name();
    }

    private TrackingIdCounter() {
        this.id = null;
        this.rev = null;
        this.type = ObjectType.Counter.name();
    }

    public String getId()  {
        return id;
    }

    public String getRev()  {
        return rev;
    }

    public long getCount() {
        return count;
    }

    public void increaseCount() {
        this.count = this.count + 1;
    }

    public String getType() {
        return type;
    }
}
