package de.bsi.secvisogram.csaf_cms_backend.rest;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.IdNotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.template.DocumentTemplateDescription;
import de.bsi.secvisogram.csaf_cms_backend.model.template.DocumentTemplateService;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateCommentRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AnswerInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.CommentInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import de.bsi.secvisogram.csaf_cms_backend.service.IdAndRevision;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdvisoryController.class)
@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "False positives on multiline format strings")
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

    private static final String advisoryRoute = "/api/v1/advisories/";

    private static final String csafJsonString = """
            {
                "document": {
                    "category": "CSAF_BASE"
                }
            }
            """;
    private static final String advisoryId = UUID.randomUUID().toString();
    private static final String fullAdvisoryJsonString = String.format(
            """
                            "owner": "Musterfrau",
                            "type": "Advisory",
                            "workflowState": "Draft",
                            "csaf": %s,
                            "_rev": "revision",
                            "_id": "%s"
                    """, csafJsonString, advisoryId);

    private static final String revision = "2-efaa5db9409b2d4300535c70aaf6a66b";

    private static final String commentRoute = advisoryRoute + advisoryId + "/comments/";
    private static final String commentId = UUID.randomUUID().toString();
    private static final String commentText = "This is a comment.";

    private static final String answerRoute = commentRoute + commentId + "/answers/";
    private static final String answerId = UUID.randomUUID().toString();
    private static final String answerText = "This is an answer";


    @Test
    public void contextLoads() {
        Assertions.assertNotNull(advisoryController);
    }


    @Test
    void listCsafDocumentsTest_empty() throws Exception {

        when(advisoryService.getAdvisoryInformations(null)).thenReturn(Collections.emptyList());

        this.mockMvc.perform(get(advisoryRoute))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

    }

    @Test
    void listCsafDocumentsTest_oneItem() throws Exception {

        AdvisoryInformationResponse info = new AdvisoryInformationResponse(advisoryId, WorkflowState.Draft);
        when(advisoryService.getAdvisoryInformations(null)).thenReturn(List.of(info));

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
    void readCsafDocumentTest_unauthorized() throws Exception {

        UUID advisoryId = UUID.randomUUID();
        when(advisoryService.getAdvisory(advisoryId.toString())).thenThrow(AccessDeniedException.class);

        this.mockMvc.perform(get(advisoryRoute + advisoryId))
                .andDo(print())
                .andExpect(status().isUnauthorized());

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
    void createCsafDocumentTest_invalidCsaf() throws Exception {

        when(advisoryService.addAdvisory(any())).thenThrow(JsonProcessingException.class);
        String invalidCsaf = "{}";
        ObjectWriter writer =  new ObjectMapper().writer(new DefaultPrettyPrinter());
        this.mockMvc.perform(
                        post(advisoryRoute).content(writer.writeValueAsString(csafToRequest(invalidCsaf)))
                                .contentType(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCsafDocumentTest_invalidJson() throws Exception {

        when(advisoryService.addAdvisory(any())).thenThrow(JsonProcessingException.class);
        String invalidJson = "invalid JSON";
        this.mockMvc.perform(
                        post(advisoryRoute).content(invalidJson)
                                .contentType(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCsafDocumentTest() throws Exception {

        IdAndRevision idRev = new IdAndRevision(advisoryId, revision);
        when(advisoryService.addAdvisory(any())).thenReturn(idRev);

        ObjectWriter writer =  new ObjectMapper().writer(new DefaultPrettyPrinter());
        this.mockMvc.perform(
                        post(advisoryRoute).with(csrf())
                                .content(writer.writeValueAsString(csafToRequest(fullAdvisoryJsonString)))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().json(String.format("{\"id\": \"%s\", \"revision\": \"%s\"}", advisoryId, revision)));
    }

    @Test
    void createCsafDocumentTest_unauthorized() throws Exception {

        doThrow(AccessDeniedException.class).when(advisoryService).addAdvisory(any());

        this.mockMvc.perform(
                        post(advisoryRoute).with(csrf()).content(csafJsonString).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeCsafDocumentTest_notExisting() throws Exception {

        doThrow(IdNotFoundException.class).when(advisoryService).updateAdvisory(eq(advisoryId), eq(revision), any());

        ObjectWriter writer =  new ObjectMapper().writer(new DefaultPrettyPrinter());
        this.mockMvc.perform(patch(advisoryRoute + advisoryId).with(csrf())
                        .content(writer.writeValueAsString(csafToRequest(fullAdvisoryJsonString)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void changeCsafDocumentTest_unauthorized() throws Exception {

        doThrow(AccessDeniedException.class).when(advisoryService).updateAdvisory(eq(advisoryId), eq(revision), any());

        ObjectWriter writer =  new ObjectMapper().writer(new DefaultPrettyPrinter());
        this.mockMvc.perform(patch(advisoryRoute + advisoryId).with(csrf())
                        .content(writer.writeValueAsString(csafToRequest(fullAdvisoryJsonString)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
    @Test
    void changeCsafDocumentTest_invalidRevision() throws Exception {

        String invalidRevision = "invalid";
        doThrow(DatabaseException.class).when(advisoryService).updateAdvisory(advisoryId, invalidRevision, csafToRequest(fullAdvisoryJsonString));

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
        when(advisoryService.updateAdvisory(eq(advisoryId), eq(revision), any())).thenReturn(newRevision);

        ObjectWriter writer =  new ObjectMapper().writer(new DefaultPrettyPrinter());
        this.mockMvc.perform(patch(advisoryRoute + advisoryId).with(csrf())
                        .content(writer.writeValueAsString(csafToRequest(fullAdvisoryJsonString)))
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
    void deleteCsafDocumentTest_unauthorized() throws Exception {

        doThrow(AccessDeniedException.class).when(advisoryService).deleteAdvisory(advisoryId, revision);

        this.mockMvc.perform(delete(advisoryRoute + advisoryId).param("revision", revision).with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listAllTemplatesTest() throws Exception {

        when(this.templateService.getAllTemplates())
                .thenReturn(new DocumentTemplateDescription[]{new DocumentTemplateDescription("T1", "Template1", "File1")});

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
                .andExpect(status().isOk());
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
    void changeWorkflowStateDraftTest() throws Exception {
        String newRevision = "2-efaa5db9409b2d4300535c70aaf5ff62";
        when(advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.Draft,
                null, null)).thenReturn(newRevision);

        this.mockMvc.perform(patch(advisoryRoute + advisoryId + "/workflowstate/Draft").with(csrf())
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void changeWorkflowStateReviewTest() throws Exception {
        String newRevision = "2-efaa5db9409b2d4300535c70aaf5ff62";
        when(advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.Review,
                null, null)).thenReturn(newRevision);

        this.mockMvc.perform(patch(advisoryRoute + advisoryId + "/workflowstate/Review").with(csrf())
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void changeWorkflowStateApprovedTest() throws Exception {
        String newRevision = "2-efaa5db9409b2d4300535c70aaf5ff62";
        when(advisoryService.changeAdvisoryWorkflowState(advisoryId, revision,
                WorkflowState.Approved, null, null)).thenReturn(newRevision);

        this.mockMvc.perform(patch(advisoryRoute + advisoryId + "/workflowstate/Approved").with(csrf())
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void changeWorkflowStateRfPublicationTest() throws Exception {
        String newRevision = "2-efaa5db9409b2d4300535c70aaf5ff62";
        when(advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.RfPublication,
                "2022-07-15T05:50:21Z", DocumentTrackingStatus.Interim)).thenReturn(newRevision);

        this.mockMvc.perform(patch(advisoryRoute + advisoryId + "/workflowstate/RfPublication").with(csrf())
                        .param("revision", revision)
                        .param("proposedTime", "2022-07-15T05:50:21Z"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void changeWorkflowStatePublishedTest() throws Exception {
        String newRevision = "2-efaa5db9409b2d4300535c70aaf5ff62";
        when(advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.Published,
                "2022-07-15T05:50:21Z", null)).thenReturn(newRevision);

        this.mockMvc.perform(patch(advisoryRoute + advisoryId + "/workflowstate/Published").with(csrf())
                        .param("revision", revision)
                        .param("proposedTime", "2022-07-15T05:50:21Z")
                        .param("documentTrackingStatus", "Interim"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void changeWorkflowStatePublishedTest_unauthorized() throws Exception {
        when(advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.Published, "2022-07-15T05:50:21Z", DocumentTrackingStatus.Interim))
                .thenThrow(new CsafException("access denied", CsafExceptionKey.NoPermissionForAdvisory, HttpStatus.UNAUTHORIZED));

        this.mockMvc.perform(patch(advisoryRoute + advisoryId + "/workflowstate/Published").with(csrf())
                        .param("revision", revision)
                        .param("proposedTime", "2022-07-15T05:50:21Z")
                        .param("documentTrackingStatus", "Interim"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createNewCsafDocumentVersionTest() throws Exception {

        String newRevision = "2-efaa5db9409b2d4300535c70aaf5ff62";
        when(advisoryService.createNewCsafDocumentVersion(advisoryId, revision))
                .thenReturn(newRevision);

        this.mockMvc.perform(patch(advisoryRoute + advisoryId + "/createNewVersion").with(csrf())
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void createNewCsafDocumentVersionTest_unauthorized() throws Exception {

        when(advisoryService.createNewCsafDocumentVersion(advisoryId, revision))
                .thenThrow(new CsafException("access denied", CsafExceptionKey.NoPermissionForAdvisory, HttpStatus.UNAUTHORIZED));

        this.mockMvc.perform(patch(advisoryRoute + advisoryId + "/createNewVersion").with(csrf())
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportAdvisoryTest() {
        // to be filled
    }

    @Test
    void listCommentsTest_empty() throws Exception {

        this.mockMvc.perform(get(commentRoute))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listCommentsTest_oneItem() throws Exception {

        String owner = "Musterfrau";

        CommentInformationResponse info = new CommentInformationResponse(commentId, advisoryId, "nodeId123", owner);
        when(advisoryService.getComments(advisoryId)).thenReturn(List.of(info));


        String expected = String.format(
                """
                        [{
                            "commentId": "%s",
                            "advisoryId": "%s",
                            "csafNodeId": "nodeId123",
                            "owner": "%s"
                        }]
                        """, commentId, advisoryId, owner
        );

        this.mockMvc.perform(get(commentRoute))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(expected));
    }

    @Test
    void listCommentsTest_unauthorized() throws Exception {

        when(advisoryService.getComments(advisoryId)).thenThrow(AccessDeniedException.class);

        this.mockMvc.perform(get(commentRoute))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCommentTest_invalidJson() throws Exception {

        String invalidJson = "not a valid JSON string";

        this.mockMvc.perform(
                        post(commentRoute).with(csrf()).content(invalidJson).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCommentTest_missingCsafNodeId() throws Exception {


        String commentJson = """
                {
                    "content": "invalid as comment"
                }
                """;

        this.mockMvc.perform(
                        post(commentRoute).with(csrf()).content(commentJson).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCommentTest_unauthorized() throws Exception {

        when(advisoryService.addComment(eq(advisoryId), any(CreateCommentRequest.class)))
                .thenThrow(AccessDeniedException.class);

        String commentJson = """
                {
                    "commentText": "This is a comment.",
                    "csafNodeId": "some node ID we pretend exists."
                }
                """;
        this.mockMvc.perform(
                        post(commentRoute).with(csrf()).content(commentJson).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }


    @Test
    void createCommentTest() throws Exception {

        IdAndRevision idRev = new IdAndRevision(UUID.randomUUID().toString(), "rev-123-abc");
        String commentJson = """
                {
                    "commentText": "This is a comment.",
                    "csafNodeId": "some node ID we pretend exists."
                }
                """;

        when(advisoryService.addComment(eq(advisoryId), any(CreateCommentRequest.class))).thenReturn(idRev);

        String expected = String.format(
                """
                        {
                            "id": "%s",
                            "revision": "%s"
                        }
                        """, idRev.getId(), idRev.getRevision());

        this.mockMvc.perform(
                        post(commentRoute).with(csrf()).content(commentJson).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().json(expected));

    }


    @Test
    void changeCommentTest_notExisting() throws Exception {

        doThrow(IdNotFoundException.class).when(advisoryService).updateComment(advisoryId, commentId, revision, commentText);

        this.mockMvc.perform(patch(commentRoute + commentId).with(csrf())
                        .content(commentText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void changeCommentTest_invalidRevision() throws Exception {

        String invalidRevision = "invalid";
        doThrow(DatabaseException.class).when(advisoryService).updateComment(advisoryId, commentId, invalidRevision, commentText);

        this.mockMvc.perform(patch(commentRoute + commentId).with(csrf())
                        .content(commentText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", invalidRevision))
                .andDo(print())
                .andExpect(status().isBadRequest());

    }

    @Test
    void changeCommentTest_invalidId() throws Exception {

        String invalidId = "not an UUID";

        this.mockMvc.perform(patch(commentRoute + invalidId).with(csrf())
                        .content(commentText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeCommentTest() throws Exception {

        String newRevision = "2-efaa5db9409b2d4300535c70aaf5ff62";
        when(advisoryService.updateComment(advisoryId, commentId, revision, commentText)).thenReturn(newRevision);

        this.mockMvc.perform(patch(commentRoute + commentId).with(csrf())
                        .content(commentText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("{\"revision\": \"%s\"}", newRevision)));
    }

    @Test
    void changeCommentTest_unauthorized() throws Exception {

        when(advisoryService.updateComment(advisoryId, commentId, revision, commentText)).thenThrow(AccessDeniedException.class);

        this.mockMvc.perform(patch(commentRoute + commentId).with(csrf())
                        .content(commentText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listAnswersTest_empty() throws Exception {

        this.mockMvc.perform(get(answerRoute))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listAnswersTest_oneItem() throws Exception {

        String owner = "Musterfrau";

        AnswerInformationResponse info = new AnswerInformationResponse(answerId, commentId, owner);
        when(advisoryService.getAnswers(advisoryId, commentId)).thenReturn(List.of(info));


        String expected = String.format(
                """
                        [{
                            "answerId": "%s",
                            "answerTo": "%s",
                            "owner": "%s"
                        }]
                        """, answerId, commentId, owner
        );

        this.mockMvc.perform(get(answerRoute))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(expected));
    }

    @Test
    void listAnswersTest_unauthorized() throws Exception {

        when(advisoryService.getAnswers(advisoryId, commentId)).thenThrow(AccessDeniedException.class);

        this.mockMvc.perform(get(answerRoute))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addAnswerTest_advisoryNotFound() throws Exception {

        String invalidJson = "not a valid JSON string";

        when(advisoryService.addAnswer(advisoryId, commentId, invalidJson)).thenThrow(new CsafException("Advisory not found", CsafExceptionKey.AdvisoryNotFound));

        this.mockMvc.perform(
                        post(answerRoute).content(invalidJson).contentType(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void addAnswerTest_unauthorized() throws Exception {

        String answerText = "This is an answer.";

        when(advisoryService.addAnswer(advisoryId, commentId, answerText)).thenThrow(AccessDeniedException.class);

        this.mockMvc.perform(post(answerRoute).with(csrf())
                        .content(answerText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }


    @Test
    void addAnswerTest() throws Exception {

        IdAndRevision idRev = new IdAndRevision(UUID.randomUUID().toString(), "rev-123-abc");
        String answerJson = """
                {
                    "commentText": "This is an answer.",
                }
                """;

        when(advisoryService.addAnswer(advisoryId, commentId, answerJson)).thenReturn(idRev);

        String expected = String.format(
                """
                        {
                            "id": "%s",
                            "revision": "%s"
                        }
                        """, idRev.getId(), idRev.getRevision());

        this.mockMvc.perform(
                        post(answerRoute).with(csrf()).content(answerJson).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().json(expected));

    }


    @Test
    void changeAnswerTest_notExisting() throws Exception {

        doThrow(IdNotFoundException.class).when(advisoryService).updateComment(advisoryId, answerId, revision, answerText);

        this.mockMvc.perform(patch(answerRoute + answerId).with(csrf())
                        .content(answerText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void changeAnswerTest_unauthorized() throws Exception {

        doThrow(AccessDeniedException.class).when(advisoryService).updateComment(advisoryId, answerId, revision, answerText);

        this.mockMvc.perform(patch(answerRoute + answerId).with(csrf())
                        .content(answerText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }


    @Test
    void changeAnswerTest_invalidRevision() throws Exception {

        String invalidRevision = "invalid";
        doThrow(DatabaseException.class).when(advisoryService).updateComment(advisoryId, answerId, invalidRevision, answerText);

        this.mockMvc.perform(patch(answerRoute + answerId).with(csrf())
                        .content(answerText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", invalidRevision))
                .andDo(print())
                .andExpect(status().isBadRequest());

    }

    @Test
    void changeAnswerTest_invalidId() throws Exception {

        String invalidId = "not an UUID";

        this.mockMvc.perform(patch(answerRoute + invalidId).with(csrf())
                        .content(answerText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeAnswerTest() throws Exception {

        String newRevision = "2-efaa5db9409b2d4300535c70aaf5ff62";
        when(advisoryService.updateComment(advisoryId, answerId, revision, answerText)).thenReturn(newRevision);

        this.mockMvc.perform(patch(answerRoute + answerId).with(csrf())
                        .content(answerText)
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("revision", revision))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("{\"revision\": \"%s\"}", newRevision)));
    }
}
