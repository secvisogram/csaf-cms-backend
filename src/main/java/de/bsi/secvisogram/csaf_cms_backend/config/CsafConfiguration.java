package de.bsi.secvisogram.csaf_cms_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "csaf")
public class CsafConfiguration {

    private CsafSummaryConfiguration summary;
    private CsafVersioningConfiguration versioning;

    public CsafSummaryConfiguration getSummary() {
        return summary;
    }

    public CsafConfiguration setSummary(CsafSummaryConfiguration summary) {
        this.summary = summary;
        return this;
    }

    public CsafVersioningConfiguration getVersioning() {
        return versioning;
    }

    public CsafConfiguration setVersioning(CsafVersioningConfiguration versioning) {
        this.versioning = versioning;
        return this;
    }
}
