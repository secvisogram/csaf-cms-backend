package de.bsi.secvisogram.csaf_cms_backend.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest()
public class PandocServiceTest {

    @Autowired
    private PandocService pandocService;

    @Test
    public void convertTest() throws IOException, CsafException {

        try (MockedConstruction<ProcessBuilder> mocked = Mockito.mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process processMock = mock(Process.class);
                    when(mock.start()).thenReturn(processMock);
                    when(processMock.waitFor(anyLong(), any())).thenReturn(Boolean.TRUE);
                    when(processMock.getErrorStream()).thenReturn(new ByteArrayInputStream("Error".getBytes(UTF_8)));
                    when(processMock.exitValue()).thenReturn(0);
                })) {
            final Path input = Files.createTempFile("input", ".md");
            final Path output = Files.createTempFile("output", ".md");
            pandocService.isReady();
            pandocService.convert(input, output);
        }
    }

    @Test
    public void convertTest_exception() throws IOException {

        int exitValue = 100;
        try (MockedConstruction<ProcessBuilder> mocked = Mockito.mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    Process processMock = mock(Process.class);
                    when(mock.start()).thenReturn(processMock);
                    when(processMock.waitFor(anyLong(), any())).thenReturn(Boolean.TRUE);
                    when(processMock.getErrorStream()).thenReturn(new ByteArrayInputStream("Error".getBytes(UTF_8)));
                    when(processMock.exitValue()).thenReturn(exitValue);
                })) {

            final Path input = Files.createTempFile("input", ".md");
            final Path output = Files.createTempFile("output", ".md");
            IOException ioException = assertThrows(IOException.class, () -> this.pandocService.convert(input, output));
            assertThat(ioException.getMessage(), startsWith("The cli tool returned with exit code " + exitValue));
        }
    }

}
