package de.bsi.secvisogram.csaf_cms_backend.service;


import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.springframework.stereotype.Service;

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
