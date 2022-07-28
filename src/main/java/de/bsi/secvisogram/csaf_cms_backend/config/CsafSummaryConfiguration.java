package de.bsi.secvisogram.csaf_cms_backend.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CsafSummaryConfiguration {

    private String publication;
    private String approve;
    private String draft;

    public String getPublication() {
        return publication;
    }

    public CsafSummaryConfiguration setPublication(String publication) {
        this.publication = publication;
        return this;
    }

    public String getApprove() {
        return approve;
    }

    public CsafSummaryConfiguration setApprove(String approve) {
        this.approve = approve;
        return this;
    }

    public String getDraft() {
        return draft;
    }

    public CsafSummaryConfiguration setDraft(String draft) {
        this.draft = draft;
        return this;
    }
}
