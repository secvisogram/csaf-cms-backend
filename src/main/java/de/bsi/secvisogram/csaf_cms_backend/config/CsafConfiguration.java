package de.bsi.secvisogram.csaf_cms_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "csaf")
public class CsafConfiguration {

    private CsafSummaryConfiguration summary;

    public CsafSummaryConfiguration getSummary() {
        return summary;
    }

    public CsafConfiguration setSummary(CsafSummaryConfiguration summary) {
        this.summary = summary;
        return this;
    }
}
