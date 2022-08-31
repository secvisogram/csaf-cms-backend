package de.bsi.secvisogram.csaf_cms_backend.service;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import de.bsi.secvisogram.csaf_cms_backend.CouchDBExtension;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafRoles;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test for the Advisory service. The required CouchDB container is started in the CouchDBExtension.
 */
@SpringBootTest(properties = "csaf.document.templates.companyLogoPath=")
@ExtendWith(CouchDBExtension.class)
@DirtiesContext
@ContextConfiguration
@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "False positives on multiline format strings")
public class AdvisoryServiceExportNoLogoTest {

    @Autowired
    private AdvisoryService advisoryService;

    @MockBean
    private PandocService pandocService;

    @MockBean
    private WeasyprintService weasyprintService;


    private static final String csafJson = """
            {
                "document": {
                    "category": "CSAF_BASE"
                }
            }""";


    @Test
    public void contextLoads() {
        Assertions.assertNotNull(advisoryService);
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void exportAdvisoryTest() throws IOException, CsafException {

        when(this.pandocService.isReady()).thenReturn(Boolean.TRUE);
        when(this.weasyprintService.isReady()).thenReturn(Boolean.TRUE);
        doNothing().when(this.pandocService).convert(any(), any());
        doNothing().when(this.weasyprintService).convert(any(), any());

        IdAndRevision idRev = advisoryService.addAdvisory(csafToRequest(csafJson));
        Path jsonExport = advisoryService.exportAdvisory(idRev.getId(), ExportFormat.JSON);
        Assertions.assertNotNull(jsonExport);
        Path pdfExport = advisoryService.exportAdvisory(idRev.getId(), ExportFormat.PDF);
        Assertions.assertNotNull(pdfExport);
        Path htmlExport = advisoryService.exportAdvisory(idRev.getId(), ExportFormat.HTML);
        Assertions.assertNotNull(htmlExport);
        Path mdExport = advisoryService.exportAdvisory(idRev.getId(), ExportFormat.Markdown);
        Assertions.assertNotNull(mdExport);
    }

    @Test
    @WithMockUser(username = "editor", authorities = {CsafRoles.ROLE_REGISTERED, CsafRoles.ROLE_AUTHOR})
    public void exportAdvisoryTest_IdNotFound() throws IOException, CsafException {

        advisoryService.addAdvisory(csafToRequest(csafJson));
        assertThrows(CsafException.class, () -> advisoryService.exportAdvisory("wrong Id", ExportFormat.JSON));
    }

}
