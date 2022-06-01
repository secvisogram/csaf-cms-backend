package de.bsi.secvisogram.csaf_cms_backend.service;

public class IdAndRevision {

    private final String id;
    private final String revision;

    public IdAndRevision(String id, String revision) {
        this.id = id;
        this.revision = revision;
    }

    public String getId() {
        return id;
    }

    public String getRevision() {
        return revision;
    }
}
