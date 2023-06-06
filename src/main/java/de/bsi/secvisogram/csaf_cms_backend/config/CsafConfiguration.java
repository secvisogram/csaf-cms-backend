package de.bsi.secvisogram.csaf_cms_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "csaf")
public class CsafConfiguration {

    private CsafSummaryConfiguration summary;
    private CsafVersioningConfiguration versioning;
    private CsafAutoPublishConfiguration autoPublish;
    
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

    public CsafAutoPublishConfiguration getAutoPublish() {
      return autoPublish;
    }

    public void setAutoPublish(CsafAutoPublishConfiguration autoPublish) {
      this.autoPublish = autoPublish;
    }
}
