package de.bsi.secvisogram.csaf_cms_backend;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import jakarta.annotation.PostConstruct;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
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

    @Autowired
    private AdvisoryService advisoryService;

    @PostConstruct
    private void postConstruct() {
        checkConfiguration();
        onStartupImporter.importAdvisories(Path.of("import"));
    }

    private static final String CONFIG_LOG_SEPARATOR = "----------------------------------------------------------------------";

    private void checkConfiguration() {
        LOG.info(CONFIG_LOG_SEPARATOR);
        LOG.info("Configuration:");

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
        LOG.info("csaf.trackingid.assignment.phase is configured to {}.", advisoryService.getTrackingIdAssignmentPhase());

        LOG.info(CONFIG_LOG_SEPARATOR);
    }

}