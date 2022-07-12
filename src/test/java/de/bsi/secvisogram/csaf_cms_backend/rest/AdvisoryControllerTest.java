package de.bsi.secvisogram.csaf_cms_backend.rest;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.IdNotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.template.DocumentTemplateDescription;
import de.bsi.secvisogram.csaf_cms_backend.model.template.DocumentTemplateService;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import de.bsi.secvisogram.csaf_cms_backend.service.IdAndRevision;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdvisoryController.class)
public class AdvisoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdvisoryService advisoryService;

    @MockBean
    private DocumentTemplateService templateService;

    @Autowired
    AdvisoryController advisoryController;

    private static final ObjectMapper jacksonMapper = new ObjectMapper();

    private static final String advisoryRoute = "/api/2.0/advisories/";

    private static final String csafJsonString = "{" +
                                                 "    \"document\": {" +
                                                 "        \"category\": \"CSAF_BASE\"" +
                                                 "    }" +
                                                 "}";
    private static final String advisoryId = UUID.randomUUID().toString();
    private static final String fullAdvisoryJsonString = String.format("{" +
                                                                       "    \"owner\": \"Musterfrau\"," +
                                                                       "    \"type\": \"Advisory\"," +
                                                                       "    \"workflowState\": \"Draft\"," +
                                                                       "    \"csaf\": %s," +
                                                                       "    \"_rev\": \"revision\"," +
                                                                       "    \"_id\": \"%s\"" +
                                                                       "}", csafJsonString, advisoryId);

    private static final String revision = "2-efaa5db9409b2d4300535c70aaf6a66b";


    @Test
    public void contextLoads() {
        Assertions.assertNotNull(advisoryController);
    }


    @Test
    void listCsafDocumentsTest_empty() throws Exception {

        when(advisoryService.getAdvisoryInformations()).thenReturn(Collections.emptyList());

        this.mockMvc.perform(get(advisoryRoute))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

    }

    @Test
    void listCsafDocumentsTest_oneItem() throws Exception {

        AdvisoryInformationResponse info = new AdvisoryInformationResponse(advisoryId, WorkflowState.Draft);
        when(advisoryService.getAdvisoryInformations()).thenReturn(List.of(info));

        this.mockMvc.perform(get(advisoryRoute))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(String.format("[{\"advisoryId\": \"%s\"}]", advisoryId))
                );

    }


    @Test
    void readCsafDocumentTest_notExisting() throws Exception {

        UUID advisoryId = UUID.randomUUID();
        when(advisoryService.getAdvisory(advisoryId.toString())).thenThrow(IdNotFoundException.class);


        this.mockMvc.perform(get(advisoryRoute + advisoryId))
                .andDo(print())
                .andExpect(status().isNotFound());

    }

    @Test
    void readCsafDocumentTest() throws Exception {

        JsonNode node = jacksonMapper.readTree(csafJsonString);
        final AdvisoryResponse advisoryResponse = new AdvisoryResponse(advisoryId, WorkflowState.Draft, node);

        when(advisoryService.getAdvisory(advisoryId)).thenReturn(advisoryResponse);

        this.mockMvc.perform(get(advisoryRoute + advisoryId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("{\"advisoryId\":  \"%s\", \"workflowState\": Draft}", advisoryId)));
    }

    @Test
    void createCsafDocumentTest_invalidJson() throws Exception {

        String invalidJson = "not a valid JSON string";

        when(advisoryService.addAdvisory(invalidJson)).thenThrow(JsonProcessingException.class);

        this.mockMvc.perform(
                        post(advisoryRoute).content(invalidJson).contentType(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCsafDocumentTest() throws Exception {

        IdAndRevision idRev = new IdAndRevision(advisoryId, revision);
        when(advisoryService.addAdvisory(csafJsonString)).thenReturn(idRev);

        this.mockMvc.perform(
                        post(advisoryRoute).with(csrf()).content(csafJsonString).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().json(String.format("{\"id\": \"%s\", \"revision\": \"%s\"}", advisoryId, revision)));
    }


    @Test
    void changeCsafDocumentTest_notExisting() throws Exception {

        doThrow(IdNotFoundException.class).when(advisoryService).updateAdvisory(advisoryId, revision, fullAdvisoryJsonString);

        this.mockMvc.perform(patch(advisoryRoute + advisoryId).with(csrf())
                        .content(fullAdvisoryJsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void changeCsafDocumentTest_invalidRevision() throws Exception {

        String invalidRevision = "invalid";
        doThrow(DatabaseException.class).when(advisoryService).updateAdvisory(advisoryId, invalidRevision, fullAdvisoryJsonString);

        this.mockMvc.perform(patch(advisoryRoute + advisoryId).with(csrf())
                        .content(fullAdvisoryJsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("revision", invalidRevision))
                .andDo(print())
                .andExpect(status().isBadRequest());

    }

    @Test
    void changeCsafDocumentTest_invalidId() throws Exception {

        String invalidId = "not an UUID";

        this.mockMvc.perform(patch(advisoryRoute + invalidId).with(csrf())
                        .content(fullAdvisoryJsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeCsafDocumentTest() throws Exception {

        String newRevision = "2-efaa5db9409b2d4300535c70aaf5ff62";
        when(advisoryService.updateAdvisory(advisoryId, revision, fullAdvisoryJsonString)).thenReturn(newRevision);

        this.mockMvc.perform(patch(advisoryRoute + advisoryId).with(csrf())
                        .content(fullAdvisoryJsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("{\"revision\": \"%s\"}", newRevision)));
    }

    @Test
    void deleteCsafDocumentTest_notExisting() throws Exception {

        UUID advisoryId = UUID.randomUUID();
        doThrow(IdNotFoundException.class).when(advisoryService).deleteAdvisory(advisoryId.toString(), revision);

        this.mockMvc.perform(delete(advisoryRoute + advisoryId).param("revision", revision).with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCsafDocumentTest_invalid() throws Exception {

        String invalidId = "invalid ID";

        this.mockMvc.perform(delete(advisoryRoute + invalidId).with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCsafDocumentTest_invalidRevision() throws Exception {

        UUID advisoryId = UUID.randomUUID();
        String invalidRevision = "invalid";
        doThrow(DatabaseException.class).when(advisoryService).deleteAdvisory(advisoryId.toString(), invalidRevision);

        this.mockMvc.perform(delete(advisoryRoute + advisoryId).with(csrf())
                        .param("revision", invalidRevision))
                .andDo(print())
                .andExpect(status().isBadRequest());

    }

    @Test
    void deleteCsafDocumentTest() throws Exception {

        this.mockMvc.perform(delete(advisoryRoute + advisoryId).param("revision", revision).with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void listAllTemplatesTest() throws Exception {

        DocumentTemplateDescription[] templateDescs = {new DocumentTemplateDescription("T1", "Template1", "File1")};
        when(this.templateService.getAllTemplates()).thenReturn(templateDescs);

        this.mockMvc.perform(get(advisoryRoute + "/templates"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("""
                                [{"templateId":"T1","templateDescription":"Template1"}]
                                """
                ));
    }

    @Test
    void listAllTemplatesTest_internalServerError() throws Exception {

        when(this.templateService.getAllTemplates()).thenThrow(new IOException());

        this.mockMvc.perform(get(advisoryRoute + "/templates"))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    void readTemplateTest() throws Exception {

        final String templateId = "T1";
        Optional<DocumentTemplateDescription> templateDesc = Optional.of(new DocumentTemplateDescription(templateId, "Template1", "File1") {
            @Override
            public JsonNode getFileAsJsonNode() throws IOException {
                return jacksonMapper.readTree(csafJsonString);
            }
        });
        when(this.templateService.getTemplateForId(templateId)).thenReturn(templateDesc);

        this.mockMvc.perform(get(advisoryRoute + "/templates/" + templateId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(csafJsonString));
    }

    @Test
    void readTemplateTest_internalServerError() throws Exception {

        final String templateId = "T1";
        Optional<DocumentTemplateDescription> templateDesc = Optional.of(new DocumentTemplateDescription(templateId, "Template1", "File1") {
            @Override
            public JsonNode getFileAsJsonNode() throws IOException {
                throw new IOException("Server Error Test");
            }
        });
        when(this.templateService.getTemplateForId(templateId)).thenReturn(templateDesc);

        this.mockMvc.perform(get(advisoryRoute + "/templates/" + templateId))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    void readTemplateTest_NotFound() throws Exception {

        final String templateId = "T1";
        Optional<DocumentTemplateDescription> templateDesc = Optional.empty();
        when(this.templateService.getTemplateForId(templateId)).thenReturn(templateDesc);

        this.mockMvc.perform(get(advisoryRoute + "/templates/" + templateId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void changeWorkflowStateTest() {
        // to be filled
        // include tests for 422 response code when trying to perform invalid change
        // allowed are:
        // Draft -> Review
        // Approved -> Published
        // Review -> Draft, Approved
    }

    @Test
    void exportAdvisoryTest_HTML() throws Exception {

        UUID advisoryId = UUID.randomUUID();
        when(advisoryService.exportAdvisory(advisoryId.toString(), ExportFormat.HTML)).thenReturn("<html></html>");

        this.mockMvc.perform(
                        get(advisoryRoute + advisoryId.toString() + "/csaf").with(csrf()).content(csafJsonString).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("<html></html>"));
    }


    @Test
    void createNewCsafDocumentVersionTest() {
        // to be filled
    }

}
