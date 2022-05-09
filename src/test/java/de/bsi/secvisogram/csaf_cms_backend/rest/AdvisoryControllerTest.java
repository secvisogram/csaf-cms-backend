package de.bsi.secvisogram.csaf_cms_backend.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.CouchDbService;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdvisoryController.class)
public class AdvisoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CouchDbService couchDbService;

    @Autowired
    AdvisoryController advisoryController;

    private static final String csafJsonString = """
            {
                "document": {
                    "category": "CSAF_BASE"
                }
            }
            """;
    private static JsonNode csafJsonNode;

    @BeforeAll
    private static void setup() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        csafJsonNode = objectMapper.readTree(csafJsonString);
    }


    @Test
    public void contextLoads() {
        Assertions.assertNotNull(advisoryController);
    }


    @Test
    void listCsafDocumentsEmptyTest() throws Exception {

        List<AdvisoryInformationResponse> emptyAdvisoryInfos = Collections.emptyList();

        when(couchDbService.readAllCsafDocuments()).thenReturn(emptyAdvisoryInfos);

        this.mockMvc.perform(get("/api/2.0/advisories/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

    }

    @Test
    void listCsafDocumentsOneItemTest() throws Exception {

        List<AdvisoryInformationResponse> advisoryInfos = List.of(
                new AdvisoryInformationResponse("advisoryId", WorkflowState.Draft)
        );

        when(couchDbService.readAllCsafDocuments()).thenReturn(advisoryInfos);

        this.mockMvc.perform(get("/api/2.0/advisories/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("[{\"advisoryId\":  \"advisoryId\", \"workflowState\":  \"Draft\"}]")
                );

    }

    @Test
    void readCsafDocumentNotPresentTest() throws Exception {

        String advisoryId = "notExisting";

        this.mockMvc.perform(get("/api/2.0/advisories/" + advisoryId))
                .andExpect(status().isNotFound());

    }

    @Test
    void readCsafDocumentTest() {

    }

    @Test
    void createCsafDocumentTest() {

    }

    @Test
    void changeCsafDocumentTest() {

    }

    @Test
    void createNewCsafDocumentVersionTest() {

    }

    @Test
    void deleteCsafDocumentTest() {

    }

    @Test
    void listAllTemplatesTest() {

    }

    @Test
    void readTemplateTest() {

    }

    @Test
    void exportAdvisoryTest() {

    }

    @Test
    void setWorkflowStateToDraftTest() {

    }

    @Test
    void setWorkflowStateToReviewTest() {

    }

    @Test
    void setWorkflowStateToApproveTest() {

    }

    @Test
    void setWorkflowStateToRfPublicationTest() {

    }

    @Test
    void setWorkflowStateToPublishTest() {

    }

    @Test
    void listCommentsTest() {

    }

    @Test
    void createCommentTest() {

    }

    @Test
    void addAnswerTest() {

    }

    @Test
    void changeCommentTest() {

    }

    @Test
    void changeAnswerTest() {

    }

}
