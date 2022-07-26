package de.bsi.secvisogram.csaf_cms_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "csaf")
public class CsafConfiguration {

    private String summarypublication;
    private String summaryapprove;
    private String summarydraft;

    public String getSummarypublication() {
        return summarypublication;
    }

    public String getSummaryapprove() {
        return summaryapprove;
    }

    public String getSummarydraft() {
        return summarydraft;
    }
}
