package de.bsi.secvisogram.csaf_cms_backend;

import static java.util.Comparator.comparing;

import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.json.Versioning;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            LOG.info("Importing files from directory {}.", importDirectory.toAbsolutePath());
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (AdvisoryToImport advisory : readAdvisoriesToImport(directoryListing, importDirectory)) {
                    importAdvisory(advisory, importDirectory);
                }
            } else {
                LOG.warn("Error accessing directory {}.", importDirectory);
            }
            LOG.info("Importing finished.");
        } else {
            LOG.warn("No directory {} found, nothing to import.", importDirectory.toAbsolutePath());
        }
    }

    /**
     * Read and parse all advisories in the given directory listing. Several versions of the same advisory are returned
     * oldest version first, so that every version can be imported on top of the version it succeeds. Files that cannot
     * be parsed are moved to the failed subdirectory right away.
     *
     * @param directoryListing the files in the import directory
     * @param importDirectory  the import directory
     * @return the advisories to import, oldest version of an advisory first
     */
    private List<AdvisoryToImport> readAdvisoriesToImport(File[] directoryListing, Path importDirectory) {

        ObjectMapper mapper = new JsonMapper();
        List<AdvisoryToImport> advisories = new ArrayList<>();
        for (File child : directoryListing) {
            String advisoryPath = child.getPath();
            if (child.isFile()) {
                try {
                    advisories.add(new AdvisoryToImport(child, mapper.readTree(child)));
                } catch (StreamReadException e) {
                    LOG.error("Error parsing JSON from file {}.", advisoryPath);
                    LOG.error(e.getMessage());
                    moveToFailedSubdirectory(child, importDirectory, e);
                } catch (JacksonException e) {
                    LOG.error("Error reading file {}.", advisoryPath);
                    LOG.error(e.getMessage());
                }
            } else {
                LOG.warn("Not a file: {}, skipping.", advisoryPath);
            }
        }
        advisories.sort(comparing(AdvisoryToImport::trackingId)
                .thenComparing(AdvisoryToImport::version, OnStartupImporter::compareVersions));
        return advisories;
    }

    private void importAdvisory(AdvisoryToImport advisory, Path importDirectory) {

        String advisoryPath = advisory.file().getPath();
        LOG.info("Importing advisory from {}.", advisoryPath);
        try {
            advisoryService.importAdvisoryForSystem(advisory.csafJson());
            moveToProcessedDirectory(advisory.file(), importDirectory);
        } catch (IOException | DatabaseException e) {
            LOG.error("Error importing file {}.", advisoryPath);
            LOG.error(e.getMessage());
        } catch (CsafException e) {
            if (e.getRecommendedHttpState() == HttpStatus.SERVICE_UNAVAILABLE) {
                LOG.error(
                        "Could not reach Validation server and check validity - not importing file {}.",
                        advisoryPath
                );
            } else {
                LOG.error("CSAF Error importing file {}.", advisoryPath);
                moveToFailedSubdirectory(advisory.file(), importDirectory, e);
            }
            LOG.error(e.getMessage());
        } catch (RuntimeException e) {
            // Importing runs in @PostConstruct, so an uncaught unchecked exception would prevent startup of the
            // application.
            LOG.error("Unexpected error importing file {}.", advisoryPath, e);
            moveToFailedSubdirectory(advisory.file(), importDirectory, e);
        }
    }

    /**
     * Compare two document tracking versions so that several versions of one advisory can be ordered oldest version
     * first. Only ever called for files that already share a tracking ID.
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return a negative number, zero or a positive number if version1 is older than, the same as or newer than
     *         version2
     */
    private static int compareVersions(String version1, String version2) {
        try {
            return Versioning.detectStrategy(version1).compareVersions(version1, version2);
        } catch (RuntimeException versionsNotComparable) {
            return version1.compareTo(version2);
        }
    }

    /**
     * A parsed CSAF document from the import directory together with the file it was read from
     *
     * @param file     the file in the import directory
     * @param csafJson the parsed CSAF document
     */
    private record AdvisoryToImport(File file, JsonNode csafJson) {

        String trackingId() {
            return textAt("/document/tracking/id");
        }

        String version() {
            return textAt("/document/tracking/version");
        }

        private String textAt(String jsonPointer) {
            JsonNode node = csafJson.at(jsonPointer);
            return node.isMissingNode() ? "" : node.asString();
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