package de.bsi.secvisogram.csaf_cms_backend.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for converting between different document formats using the
 * <a href="https://weasyprint.org/">weasyprint</a> cli tool.
 * <p>
 * Requires to have the weasyprint command in the current system path installed
 * (availability can be checked by using the {@link WeasyprintService#isReady()} method).
 */
@Service
public class WeasyprintService {
    private static final Logger LOG = LoggerFactory.getLogger(WeasyprintService.class);

    /**
     * Convert the input file into a new file format given by the extension of the output file.
     *
     * @param input  the input file; only existing files are valid
     * @param output the output file; the extension defines in what format the file will be converted
     * @throws IOException on any error regarding the weasyprint cli tool
     */
    public void convert(
            @Nonnull final Path input,
            @Nonnull final Path output)
            throws IOException, InterruptedException {
        final String inputFilePath = input.toAbsolutePath().toString();
        final String outputFilePath = output.toAbsolutePath().toString();
        callWeasyprint(inputFilePath, outputFilePath);
    }

    /**
     * Check if the weasyprint cli is ready to be used in the current system environment.
     * Either returns true or throws an error with further description what went wrong.
     *
     * @return true if weasyprint is ready to be used in the current system environment
     * @throws IOException if weasyprint can not be successfully called
     */
    public boolean isReady()
            throws IOException, InterruptedException {
        return callWeasyprint("--version");
    }

    /**
     * Call the weasyprint cli tool with the given args.
     *
     * @param args the arguments to call the weasyprint cli tool with
     * @return true if the call was successful, false otherwise
     * @throws IOException on any error with the call of the weasyprint cli tool
     */
    private boolean callWeasyprint(
            final String... args)
            throws IOException, InterruptedException {
        // prepare the command to call weasyprint with
        final List<String> command = new ArrayList<>(args.length + 1);
        command.add("weasyprint");
        command.addAll(Arrays.asList(args));

        // call weasyprint cli (timeout after 1 minute!)
        LOG.debug("Calling weasyprint cli with following command: " + String.join(" ", command));
        final Process weasyprint = new ProcessBuilder(command).start();
        weasyprint.waitFor(1, TimeUnit.MINUTES);
        if (weasyprint.exitValue() == 0) {
            return true;
        } else {
            String errorMessageBuilder = "weasyprint cli returned with exit code " + weasyprint.exitValue() + " " +
                    "and output: " + processInputStreamToString(weasyprint.getErrorStream());
            throw new IOException(errorMessageBuilder);
        }
    }

    private static String processInputStreamToString(@Nonnull final InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
