package de.bsi.secvisogram.csaf_cms_backend;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private static final String PROCESSED_DIRECTORY_NAME = "processed";
    private static final String FAILED_DIRECTORY_NAME = "failed";
    private static final String ERROR_LOG_SUFFIX = ".err";

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
                    if (child.isFile()) {
                        LOG.info("Importing advisory from {}.", advisoryPath);
                        try {
                            JsonNode csafJson = mapper.readTree(child);
                            advisoryService.importAdvisoryForSystem(csafJson);
                            moveToProcessedDirectory(child, importDirectory);
                        } catch (StreamReadException e) {
                            LOG.error("Error parsing JSON from file {}.", advisoryPath);
                            LOG.error(e.getMessage());
                            moveToFailedSubdirectory(child, importDirectory, e);
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
                                moveToFailedSubdirectory(child, importDirectory, e);
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
            LOG.info("Importing finished.");
        } else {
            LOG.warn("No directory {} found, nothing to import.", importDirectory.toAbsolutePath());
        }
    }

    private void moveToProcessedDirectory(File file, Path importDirectory) {
        Path targetDirectory = importDirectory.resolve(PROCESSED_DIRECTORY_NAME);
        try {
            Files.createDirectories(targetDirectory);
            Files.move(file.toPath(), uniqueNamedTarget(targetDirectory, file.getName()));
        } catch (IOException e) {
            LOG.error("Could not move file {} to {}: {}", file.getPath(), targetDirectory, e.getMessage());
        }
    }

    private void moveToFailedSubdirectory(File file, Path importDirectory, Exception cause) {
        Path targetDirectory = importDirectory.resolve(FAILED_DIRECTORY_NAME);
        try {
            Files.createDirectories(targetDirectory);
            Path target = uniqueNamedTarget(targetDirectory, file.getName());
            Files.move(file.toPath(), target);
            writeErrorLog(target, cause);
        } catch (IOException e) {
            LOG.error("Could not move file {} to {}: {}", file.getPath(), targetDirectory, e.getMessage());
        }
    }

    private void writeErrorLog(Path movedFile, Exception cause) {
        Path errorLogFile = movedFile.resolveSibling(movedFile.getFileName() + ERROR_LOG_SUFFIX);
        try {
            Files.writeString(errorLogFile, cause.getMessage());
        } catch (IOException e) {
            LOG.error("Could not write error log file {}: {}", errorLogFile, e.getMessage());
        }
    }

    // append numeric suffixes to name if necessary in order to avoid using the name of an existing file.
    private Path uniqueNamedTarget(Path targetDirectory, String fileName) {
        Path target = targetDirectory.resolve(fileName);
        for (int suffix = 1; Files.exists(target); suffix++) {
            target = targetDirectory.resolve(fileName + "." + suffix);
        }
        return target;
    }

}