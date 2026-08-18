package de.bsi.secvisogram.csaf_cms_backend;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Actions to do after startup of the application
 */
@Component
public class PostConstructActions {

    private static final Logger LOG = LoggerFactory.getLogger(PostConstructActions.class);

    @Value("${csaf.references.baseurl}")
    private String referencesBaseUrl;

    @Value("${csaf.trackingid.company}")
    private String trackingidCompany;

    @Autowired
    private OnStartupImporter onStartupImporter;

    @Autowired
    private CsafConfiguration configuration;

    @PostConstruct
    private void postConstruct() {
        checkConfiguration();
        onStartupImporter.importAdvisories(Path.of("import"));
    }

    private void checkConfiguration() {
        if (this.referencesBaseUrl == null || this.referencesBaseUrl.isBlank()) {
            LOG.warn("csaf.references.baseurl is not configured");
        } else {
            if (!this.referencesBaseUrl.startsWith("https://")) {
                LOG.warn("csaf.references.baseurl should start with https://");
            }
        }
        if (this.trackingidCompany == null || this.trackingidCompany.isBlank()) {
            LOG.warn("csaf.trackingid.company is not configured");
        } else {
            LOG.info("csaf.trackingid.company is configured to {}", this.trackingidCompany);
        }

        LOG.info("Is Allowed to Approved Own Documents:  {}.", configuration.getWorkflow().isAllowOwnDocumentsApproved());
        LOG.info("Creates an Html Reference on Publish:  {}.", configuration.getWorkflow().isCreateHtmlReference());
    }

}