package de.bsi.secvisogram.csaf_cms_backend;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * Actions to do after startup of the application
 */
public class PostConstructActions {

    private static final Logger LOG = LoggerFactory.getLogger(SecvisogramApplication.class);

    @Value("${csaf.references.baseurl}")
    private String referencesBaseUrl;

    @Value("${csaf.trackingid.company}")
    private String trackingidCompany;

    @PostConstruct
    public void init() {
        if (this.referencesBaseUrl == null || this.referencesBaseUrl.isBlank()) {
            LOG.warn("csaf.references.baseurl is not configured");
            if (!this.referencesBaseUrl.startsWith("https://")) {
                LOG.warn("csaf.references.baseurl should start with https://");
            }
        }
        if (this.trackingidCompany == null || this.trackingidCompany.isBlank()) {
            LOG.warn("csaf.trackingid.company is not configured");
        }
    }

}
