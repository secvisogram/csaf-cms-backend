package de.bsi.secvisogram.csaf_cms_backend;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;

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
    private AdvisoryService advisoryService;

    @Autowired
    private CsafConfiguration configuration;

    @PostConstruct
    private void postConstruct() {
        checkConfiguration();
        importAdvisories("import");
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

    private void importAdvisories(String importDirectory) {
        File dir = new File(importDirectory);
        if (dir.exists()) {
            LOG.info("Importing files from directory {}.", importDirectory);
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                ObjectMapper mapper = new JsonMapper();
                for (File child : directoryListing) {
                    String advisoryPath = child.getPath();
                    LOG.warn("Importing advisory from {}.", advisoryPath);
                    if (child.isFile()) {
                        try {
                            JsonNode csafJson = mapper.readTree(child);
                            advisoryService.importAdvisoryForSystem(csafJson);
                        } catch (StreamReadException e) {
                            LOG.error("Error parsing JSON from file {}.", advisoryPath);
                            LOG.error(e.getMessage());
                        } catch (JacksonException | IOException e) {
                            LOG.error("Error reading file {}.", advisoryPath);
                            LOG.error(e.getMessage());
                        } catch (CsafException e) {
                            if (e.getRecommendedHttpState() == HttpStatus.SERVICE_UNAVAILABLE) {
                                LOG.error(
                                        "Could not reach Validation server and check validity - not importing file {}.",
                                        advisoryPath
                                );
                            } else {
                                LOG.error("CSAF Error importing file {}.", advisoryPath);
                            }
                            LOG.error(e.getMessage());
                        }
                    } else {
                        LOG.warn("Not a file: {}, skipping.", advisoryPath);
                    }
                }
            } else {
                LOG.warn("Error accessing directory {}.", importDirectory);
            }
        } else {
            LOG.info("No directory {} found, nothing to import.", importDirectory);
        }
    }

}
