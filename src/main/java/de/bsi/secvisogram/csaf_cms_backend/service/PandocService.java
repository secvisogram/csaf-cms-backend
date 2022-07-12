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
 * <a href="https://pandoc.org/">pandoc</a> cli tool.
 * <p>
 * Requires to have the pandoc command in the current system path installed
 * (availability can be checked by using the {@link PandocService#isReady()} method).
 */
@Service
public class PandocService {
    private static final Logger LOG = LoggerFactory.getLogger(PandocService.class);

    /**
     * Convert the input file into a new file format given by the extension of the output file.
     *
     * @param input the input file; only existing files are valid
     * @param output the output file; the extension defines in what format the file will be converted
     * @throws IOException on any error regarding the pandoc cli tool
     */
    public void convert(
            @Nonnull final Path input,
            @Nonnull final Path output)
            throws IOException, InterruptedException {
        final String inputFilePath = input.toAbsolutePath().toString();
        final String outputFilePath = output.toAbsolutePath().toString();
        callPandoc(inputFilePath, "-o", outputFilePath);
    }

    /**
     * Check if the pandoc cli is ready to be used in the current system environment.
     * Either returns true or throws an error with further description what went wrong.
     *
     * @return true if pandoc is ready to be used in the current system environment
     * @throws IOException if pandoc can not be successfully called
     */
    public boolean isReady()
            throws IOException, InterruptedException {
        return callPandoc("-v");
    }

    /**
     * Call the pandoc cli tool with the given args.
     *
     * @param args the arguments to call the pandoc cli tool with
     * @return true if the call was successful, false otherwise
     * @throws IOException on any error with the call of the pandoc cli tool
     */
    private boolean callPandoc(
            final String... args)
            throws IOException, InterruptedException {
        // prepare the command to call pandoc with
        final List<String> command = new ArrayList<>(args.length + 1);
        command.add("pandoc");
        command.addAll(Arrays.asList(args));

        // call pandoc cli (timeout after 1 minute!)
        LOG.debug("Calling pandoc cli with following command: " + String.join(" ", command));
        final Process pandoc = new ProcessBuilder(command).start();
        pandoc.waitFor(1, TimeUnit.MINUTES);
        if (pandoc.exitValue() == 0) {
            return true;
        } else {
            final StringBuilder errorMessageBuilder = new StringBuilder()
                    .append("pandoc cli returned with exit code ").append(pandoc.exitValue()).append(" ")
                    .append("and output: ").append(processInputStreamToString(pandoc.getErrorStream()));
            throw new IOException(errorMessageBuilder.toString());
        }
    }

    private static String processInputStreamToString(@Nonnull final InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
