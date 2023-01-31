package de.bsi.secvisogram.csaf_cms_backend;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import java.io.File;
import java.io.IOException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

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

    @PostConstruct
    private void postConstruct() {
        checkConfiguration();
        importAdvisories("import");
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
        }
    }

    private void importAdvisories(String importDirectory) {
        File dir = new File(importDirectory);
        if (dir.exists()) {
            LOG.info("Importing files from directory {}.", importDirectory);
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                ObjectMapper mapper = new ObjectMapper();
                for (File child : directoryListing) {
                    String advisoryPath = child.getPath();
                    LOG.warn("Importing advisory from {}.", advisoryPath);
                    if (child.isFile()) {
                        try {
                            JsonNode csafJson = mapper.readTree(child);
                            advisoryService.importAdvisoryForSystem(csafJson);
                        } catch (JsonParseException e) {
                            LOG.error("Error parsing JSON from file {}.", advisoryPath);
                            LOG.error(e.getMessage());
                        } catch (IOException e) {
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
