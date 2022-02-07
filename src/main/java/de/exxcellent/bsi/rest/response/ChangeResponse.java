package de.exxcellent.bsi.rest.response;

import java.time.LocalDate;

public class ChangeResponse {

    private final long advisoryId;
    private final String advisoryVersion;
    private final String userId;
    private final LocalDate createdAt;

    public ChangeResponse(long advisoryId, String advisoryVersion, String userId, LocalDate createdAt) {
        this.advisoryId = advisoryId;
        this.advisoryVersion = advisoryVersion;
        this.userId = userId;
        this.createdAt = createdAt;
    }

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
