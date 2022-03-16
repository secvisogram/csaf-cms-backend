package de.exxcellent.bsi.rest;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.exxcellent.bsi.SecvisogramApplication;
import de.exxcellent.bsi.coudb.CouchDbService;
import de.exxcellent.bsi.json.AdvisoryJsonService;
import de.exxcellent.bsi.model.ExportFormat;
import de.exxcellent.bsi.model.WorkflowState;
import de.exxcellent.bsi.rest.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * API for Creating, Retrieving, Updating and Deleting of CSAF Dokuments,
 * including their Versions, Audit Trails, Comments and Workflow States.
 */
@RestController
@RequestMapping(SecvisogramApplication.BASE_ROUTE +"advisories")
@Tag(name = "Advisory", description = "API for for Creating, Retrieving, Updating and Deleting of CSAF Dokuments, including their Versions, Comments and Workflow States.")
public class AdvisoryController {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryController.class);

    private final AdvisoryJsonService jsonService = new AdvisoryJsonService();

    @Autowired
    private CouchDbService couchdDbService;

    @Value("${couchdb.dbname}")
    private String dbname;

    /**
     * Read all searched advisories
     * @param expression serach expression as json string
     * @return filtered advisories
     */
    @GetMapping("/")
    @Operation(summary = "Get all authorized advisories", tags = { "Advisory" })
    public List<AdvisoryInformationResponse> findAdvisories(@RequestParam(required = false)
                        @Parameter(in = ParameterIn.QUERY, name = "expression",
                                description = "The filter expression in JSON format",
                                schema = @Schema(
                                        type = "string",
                                        format = "json",
                                        description = "filter expression")) String expression) {


        return this.couchdDbService.readAllCsafDocuments();
    }


    /**
     * Get Single Advisory
     * @param advisoryId Id of the CSAF-Dokumente, that should be read
     * @return dokument
     */
    @GetMapping("/{advisoryId}/")
    @Operation(summary = "Get a single Advisory", description = "Get the advisory CSAF document and some additional data for the given id.", tags = { "Advisory" })
    public AdvisoryResponse advisoryById(
            @Parameter(description = "Id of the advisory to read") @PathVariable String advisoryId) throws IOException {

        JsonNode document = this.couchdDbService.readCsafDokument(advisoryId);
        return jsonService.covertCoudbCsafToAdvisory(document, advisoryId);
    }

    /**
     * Create new CSAF-document
     * @param newCsafJson content of the new CSAF document
     */
    @PostMapping(name="/", consumes = "application/json")
    @Operation(summary = "Create a new Advisory in the system", description = "Create a new CSAF-document in the system", tags = { "Advisory" }
            ,requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Advisory in CSAF JSON Format with comments.", required = true))
    public AdvisoryCreateResponse createCsafDocument(
            @RequestBody String newCsafJson) throws IOException {

        final InputStream csafStream = new ByteArrayInputStream(newCsafJson.getBytes(StandardCharsets.UTF_8));
        final String owner = "Musterman";
        ObjectNode objectNode = jsonService.convertCsafToJson(csafStream, owner, WorkflowState.Draft);
        final UUID uuid= UUID.randomUUID();
        final String revision = couchdDbService.writeCsafDocument(uuid, objectNode);
        return new AdvisoryCreateResponse(uuid.toString(), revision);
    }

    /**
     * change CSAF-document
     * @param advisoryId id of the CSAF document to change
     * @param revision optimistic locking revision
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change advisory", description = "Change a CSAF-document in the system", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/")
    public AdvisoryCreateResponse changeCsafDocument(
            @Parameter(description = "Id of the advisory to change") @PathVariable String advisoryId
            , @Parameter(description = "Optimistic locking revision") @RequestParam String revision
            , @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Advisory in CSAF JSON Format with comments.", required = true)
           @RequestBody String changedCsafJson) throws IOException {

        final InputStream csafStream = new ByteArrayInputStream(changedCsafJson.getBytes(StandardCharsets.UTF_8));
        final String owner = "Musterman";
        ObjectNode objectNode = jsonService.convertCsafToJson(csafStream, owner, WorkflowState.Draft);
        final String newRevision = couchdDbService.updateCsafDocument(advisoryId, revision, objectNode);

        return new AdvisoryCreateResponse(advisoryId, newRevision);
    }

    /**
     * Delete  CSAF document
     * @param advisoryId advisoryId id of the CSAF document to delete
     * @param revision optimistic locking revision
     */
    @Operation(summary = "Delete advisory."
            , description = "Delete advisory from the system. All older versions, comments and audit-trails are also deleted.", tags = { "Advisory" })
    @DeleteMapping("/{advisoryId}/")
    public void deleteAdvisoryWithId(
                @Parameter(description = "Id of the advisory to read") @PathVariable String advisoryId
            ,   @Parameter(description = "Optimistic locking revision") @RequestParam String revision  ) {
        this.couchdDbService.deleteCsafDokument(advisoryId, revision);
    }

    /**
     * Get list of all templates in the system
     * @return list of all templates
     */
    @GetMapping("/templates")
    @Operation(summary = "Get all authorized templates", description = "Get all available templates in the system", tags = { "Advisory" })
    public List<AdvisoryTemplateInformationResponse> getAllTemplates() {

        return Arrays.asList(
                new AdvisoryTemplateInformationResponse(1L, "Template for security incident response"),
                new AdvisoryTemplateInformationResponse(2L, "Template for informational Advisory"),
                new AdvisoryTemplateInformationResponse(3L, "Template for security Advisory")
        );
    }

    /**
     * Get Content of a template
     * @param templateId Id of the CSAF-template, that should be read
     * @return dokument
     */
    @GetMapping("/templates/{templateId}")
    @Operation(summary = "Get template content", description = "Get the content of the templates with the given templateId", tags = { "Advisory" })
    public AdvisoryTemplateResponse templateById(
            @Parameter(description = "Id of the template to read") @PathVariable long templateId) {

        return null;
    }

    /**
     * Export CSAF document
     * @param advisoryId id of the CSAF-document, that should be exported
     * @param format optional - format of the result
     * @return the converted advisory
     */
    @GetMapping(value="/{advisoryId}/csaf",produces = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_HTML_VALUE
            , MediaType.TEXT_MARKDOWN_VALUE, MediaType.APPLICATION_PDF_VALUE})
    @Operation(summary = "Export advisory CSAF document ",
                description = "Export advisory csaf in different formats, possible formats are: PDF, Markdown, HTML, JSON",tags = { "Advisory" })
    public String exportAdvisory(
            @Parameter(description = "Id of the advisory to export") @PathVariable String advisoryId
            , @RequestParam(required = false) ExportFormat format) {

        return "";
    }

    /**
     * Change workflow state of a CSAF document
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision optimistic locking revision
     * @param newState new workflow state of the CSAF document
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change workflow state of an advisory",
               description = "Change the workflow state of the advisory with the given id", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/workflowstate/")
    public AdvisoryUpdateResponse changeWorkflowState(
              @Parameter(description = "Id of the advisory to change the workflow state")  @PathVariable String advisoryId
            , @Parameter(description = "Optimistic locking revision") @RequestParam String revision
            , @RequestBody WorkflowState newState) {

        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Get a list of all comments and answers of an CSAF document
     * @param advisoryId  id of the CSAF document to add the answer
     * @return list of comments and their metadata
     */
    @Operation( summary = "Show comments and answers of an advisory",
                description = "Show all comments and answers of the advisory with the given id", tags = { "Advisory" })
    @GetMapping("/{advisoryId}/comments/")
    public List<AdvisoryCommentResponse> comments(
            @Parameter(description = "Id of the advisory to get the comments") @PathVariable String advisoryId) {

        return Collections.emptyList();
    }

    /**
     * Add comment to an advisory
     * @param advisoryId id of the CSAF document to add the comment
     * @param commentText text content of the comment
     *
     */
    @Operation(summary = "Add comment to an advisory",
               description = "Add a comment to the advisory with the given id", tags = { "Advisory" })
    @PostMapping("/{advisoryId}/comments")
    public AdvisoryCreateResponse createComment(
            @Parameter(description = "Id of the advisory to add the comments") @PathVariable String advisoryId
            , @RequestBody AdvisoryCreateCommentRequest commentText) {

        return new AdvisoryCreateResponse(UUID.randomUUID().toString(), "2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Add answer to a comment of an advisory
     * @param advisoryId id of the CSAF document to add the answer
     * @param commentId id of the comment to add the answer
     * @param answerText new text content of the answer
     *
     */
    @Operation(summary = "Add answer to an advisory comment",
            description = "Add a answer to the comment with the given id", tags = { "Advisory" })
    @PostMapping("/{advisoryId}/comments/{commentId}/answer")
    public AdvisoryCreateResponse createAnswer(
            @Parameter(description = "Id of the advisory to add the answer") @PathVariable long advisoryId
            , @Parameter(description = "Id of the comment to add the answer") @PathVariable long commentId
            , @RequestBody String answerText) {

        return new AdvisoryCreateResponse(UUID.randomUUID().toString(), "2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Change comment of an advisory
     * @param advisoryId id of the CSAF document to add the answer
     * @param commentId of the comment to change the answer
     * @param revision optimistic locking revision
     * @param newCommentText new text content of the comment
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change comment of an advisory", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/comments/{commentId}")
    public AdvisoryUpdateResponse changeComment(
            @Parameter(description = "Id of the advisory to change the comment") @PathVariable long advisoryId
            , @Parameter(description = "Id of the comment to change") @PathVariable long commentId
            , @Parameter(description = "Optimistic locking revision of the comment")  @RequestParam String revision
            , @RequestBody String newCommentText) {

        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Change answer of a comment to an advisory
     * @param advisoryId id of the CSAF document to change the answer
     * @param commentId commentId of the comment
     * @param answerId id of the answer to change
     * @param revision optimistic locking revision
     * @param newAnswerText new text content of the answer
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change answer to an advisory comment", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/comments/{commentId}/answer/{answerId}")
    public AdvisoryUpdateResponse changeAnswer(
                @Parameter(description = "Id of the advisory to change the answer") @PathVariable long advisoryId
                , @Parameter(description = "Id of the comment the answer belongs to") @PathVariable long commentId
                , @Parameter(description = "Id of the answer to change") @PathVariable long answerId
                , @Parameter(description = "Optimistic locking revision of the answer")  @RequestParam String revision
                , @RequestBody String newAnswerText) {

        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

}
