package de.bsi.secvisogram.csaf_cms_backend.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CsafSummaryConfiguration {

    private String publication;

    public String getPublication() {
        return publication;
    }

    public CsafSummaryConfiguration setPublication(String publication) {
        this.publication = publication;
        return this;
    }
}
