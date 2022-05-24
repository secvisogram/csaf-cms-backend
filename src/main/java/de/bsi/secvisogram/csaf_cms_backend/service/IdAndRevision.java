package de.bsi.secvisogram.csaf_cms_backend.service;

import java.util.UUID;

public class IdAndRevision {

    private final UUID id;
    private final String revision;

    public IdAndRevision(UUID id, String revision) {
        this.id = id;
        this.revision = revision;
    }

    public UUID getId() {
        return id;
    }

    public String getRevision() {
        return revision;
    }
}
