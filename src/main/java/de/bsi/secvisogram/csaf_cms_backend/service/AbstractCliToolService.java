package de.bsi.secvisogram.csaf_cms_backend.service;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Abstract base class for cli tools.
 */
public abstract class AbstractCliToolService {
    private static final Logger LOG = LoggerFactory.getLogger(PandocService.class);
    private Integer processTimeoutSeconds = 60;
    private final String baseCommand;

    /**
     * Create a new cli tool service with the given baseCommand and a process timeout.
     * The timeout defaults to 60 seconds.
     * @param baseCommand the base command; e.g. "pandoc"
     * @param processTimeoutSeconds the duration to wait for the command to finish before killing it
     *                              and throwing an error
     */
    public AbstractCliToolService(@Nonnull final String baseCommand,
                                  @Nullable final Integer processTimeoutSeconds) {
        this.baseCommand = baseCommand;
        if (processTimeoutSeconds != null) {
            this.processTimeoutSeconds = processTimeoutSeconds;
        }
    }

    /**
     * Check if cli tool is ready to be used in the current system environment.
     * Either returns true or throws an error with further description what went wrong.
     *
     * @return true if the cli tool is ready to be used in the current system environment
     * @throws IOException if the cli tool can not be successfully called
     * @throws CsafException for other errors (see details in the exception for information of what happened)
     */
    public abstract boolean isReady()
        throws IOException, CsafException;

    /**
     * Call the cli tool with the given args.
     *
     * @param args the arguments to call the cli tool with
     * @return true if the call was successful, false otherwise
     * @throws IOException on any error with the call of the cli tool
     * @throws CsafException for other errors (see details in the exception for information of what happened)
     */
    public boolean call(final String... args)
        throws IOException, CsafException {
        // prepare the command to call the cli tool with
        final List<String> command = new ArrayList<>(args.length + 1);
        command.add(this.baseCommand);
        command.addAll(Arrays.asList(args));

        try {
            LOG.debug("Calling the following command: " + String.join(" ", command));
            final Process process = new ProcessBuilder(command).start();
            process.waitFor(this.processTimeoutSeconds, TimeUnit.SECONDS);
            if (process.exitValue() == 0) {
                return true;
            } else {
                String errorMessage = "The cli tool returned with exit code " + process.exitValue() + " " +
                        "and output: " + processInputStreamToString(process.getErrorStream());
                throw new IOException(errorMessage);
            }
        } catch (InterruptedException e) {
            throw new CsafException(
                    "The call of the cli tool took too longer than " + this.processTimeoutSeconds + " seconds to process",
                    CsafExceptionKey.ExportTimeout,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private static String processInputStreamToString(@Nonnull final InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
