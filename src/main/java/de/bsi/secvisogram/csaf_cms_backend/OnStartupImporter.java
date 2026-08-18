package de.bsi.secvisogram.csaf_cms_backend;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Imports CSAF advisories from a directory on the filesystem, used by {@link PostConstructActions}
 * to import advisories found in the "import" directory on startup.
 */
@Component
public class OnStartupImporter {

    private static final Logger LOG = LoggerFactory.getLogger(OnStartupImporter.class);

    @Autowired
    private AdvisoryService advisoryService;

    void importAdvisories(Path importDirectory) {
        File dir = importDirectory.toFile();
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