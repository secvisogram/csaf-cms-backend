package de.exxcellent.bsi.rest.response;

import java.time.LocalDate;

public class ChangeResponse {

    private long advisoryId;
    private String advisoryVersion;
    private String userId;
    private LocalDate createdAt;

    public long getAdvisoryId() {
        return advisoryId;
    }

    public String getAdvisoryVersion() {
        return advisoryVersion;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

}
