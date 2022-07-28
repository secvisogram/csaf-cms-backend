package de.bsi.secvisogram.csaf_cms_backend.service;


import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
public class PandocService extends AbstractCliToolService {
    private static final String BASE_COMMAND = "pandoc";

    public PandocService() {
        super(BASE_COMMAND, null);
    }

    public boolean isReady()
            throws IOException, CsafException {
        return call("-v");
    }

    /**
     * Convert the input file into a new file format given by the extension of the output file.
     *
     * @param input  the input file; only existing files are valid
     * @param output the output file; the extension defines in what format the file will be converted
     * @throws IOException on any error regarding the pandoc cli tool
     * @throws CsafException for other errors (see details in the exception for information of what happened)
     */
    public void convert(
            @Nonnull final Path input,
            @Nonnull final Path output)
            throws IOException, CsafException {
        final String inputFilePath = input.toAbsolutePath().toString();
        final String outputFilePath = output.toAbsolutePath().toString();
        call(inputFilePath, "-o", outputFilePath);
    }
}
