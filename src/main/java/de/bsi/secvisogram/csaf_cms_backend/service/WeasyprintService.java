package de.bsi.secvisogram.csaf_cms_backend.service;


import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
 * Service for converting HTML into PDFs using the <a href="https://weasyprint.org/">weasyprint</a> cli tool.
 * Requires to have the weasyprint command in the current system path installed
 * (availability can be checked by using the {@link WeasyprintService#isReady()} method).
 */
@Service
public class WeasyprintService extends AbstractCliToolService {
    private static final String BASE_COMMAND = "weasyprint";

    public WeasyprintService() {
        super(BASE_COMMAND, null);
    }

    public boolean isReady()
            throws IOException, CsafException {
        return call("--version");
    }

    /**
     * Convert the input HTML file into a PDF using weasyprint.
     *
     * @param input  the input file; only existing files are valid
     * @param output the output file; the output will always be a PDF file
     *               (although with wrong extension if you specify another one)
     * @throws IOException on any error regarding the weasyprint cli tool
     */
    public void convert(
            @Nonnull final Path input,
            @Nonnull final Path output)
            throws IOException, CsafException {
        final String inputFilePath = input.toAbsolutePath().toString();
        final String outputFilePath = output.toAbsolutePath().toString();
        call(inputFilePath, outputFilePath);
    }
}
