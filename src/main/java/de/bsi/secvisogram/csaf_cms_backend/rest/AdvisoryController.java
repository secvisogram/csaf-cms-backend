package de.bsi.secvisogram.csaf_cms_backend.rest;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.SecvisogramApplication;
import de.bsi.secvisogram.csaf_cms_backend.coudb.CouchDbService;
import de.bsi.secvisogram.csaf_cms_backend.coudb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryJsonService;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Read all searched advisories
     * @param expression serach expression as json string
     * @return filtered advisories
     */
    @GetMapping("/")
    @Operation( summary = "Get all authorized advisories"
            , description = "All CSAF documents for which the logged-in user is authorized are returned." +
            " This depends on the user's role and the state of the CSAF document."
            , tags = { "Advisory" })
    public List<AdvisoryInformationResponse> listCsafDocuments(@RequestParam(required = false)
                        @Parameter(in = ParameterIn.QUERY, name = "expression",
                                description = "The filter expression in JSON format",
                                schema = @Schema(
                                        type = "string",
                                        format = "json",
                                        description = "filter expression")) String expression) {

        LOG.info("findAdvisories {} ", expression);
        return this.couchdDbService.readAllCsafDocuments();
    }


    /**
     * Get Single Advisory
     * @param advisoryId Id of the CSAF-Dokumente, that should be read
     * @return dokument
     */
    @GetMapping("/{advisoryId}/")
    @Operation(summary = "Get a single Advisory", description = "Get the advisory CSAF document and some additional data for the given advisoryId.", tags = { "Advisory" })
    public AdvisoryResponse readCsafDocument(
            @Parameter(description = "Id of the advisory to read") @PathVariable String advisoryId) throws IOException {

        LOG.info("readCsafDocument");
        JsonNode document = this.couchdDbService.readCsafDokument(advisoryId);
        return jsonService.covertCoudbCsafToAdvisory(document, advisoryId);
    }

    /**
     * Create new CSAF-document
     * @param newCsafJson content of the new CSAF document
     */
    @PostMapping(name="/", consumes = "application/json")
    @Operation(summary = "Create a new Advisory in the system.", description = "Create a new CSAF-document in the system.", tags = { "Advisory" }
            ,requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Advisory in CSAF JSON Format with comments.", required = true
                ,content = @Content(mediaType="application/json"
                                    ,schema = @Schema(title = "Common Security Advisory Framework with Comments", description="http://docs.oasis-open.org/csaf/csaf/v2.0/csd02/schemas/csaf_json_schema.json")
                                    , examples = {@ExampleObject(name="CSAF with comments"
                                                , value = "document: { $comment: [23454], category: generic_csaf,...")})))
    public AdvisoryCreateResponse createCsafDocument(
            @RequestBody String newCsafJson) throws IOException {

        LOG.info("createCsafDocument");
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
    @Operation(summary = "Change advisory", description = "Change a CSAF-document in the system. On saving a document its" +
            "content may change, e.g. the document version. Thus after changing a document , it must be reloaded on the client side.", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/")
    public AdvisoryUpdateResponse changeCsafDocument(
            @Parameter(description = "Id of the advisory to change") @PathVariable String advisoryId
            , @Parameter(description = "Optimistic locking revision") @RequestParam String revision
            , @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Advisory in CSAF JSON Format with comments.", required = true)
           @RequestBody String changedCsafJson) throws IOException {

        LOG.info("changeCsafDocument");
        final InputStream csafStream = new ByteArrayInputStream(changedCsafJson.getBytes(StandardCharsets.UTF_8));
        final String owner = "Musterman";
        ObjectNode objectNode = jsonService.convertCsafToJson(csafStream, owner, WorkflowState.Draft);
        final String newRevision = couchdDbService.updateCsafDocument(advisoryId, revision, objectNode);

        return new AdvisoryUpdateResponse(newRevision);
    }

    /**
     * Increase version of the CSAF document
     * @param advisoryId id of the CSAF document to change
     * @param revision optimistic locking revision
     * @return new optimistic locking revision
     */
    @Operation(summary = "Increase version of the CSAF document", description = "Increase the version of the the CSAF document", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/csaf/document/tracking/version")
    public AdvisoryUpdateResponse createNewCsafDocumentVersion(
            @Parameter(description = "Id of the advisory to change") @PathVariable String advisoryId
            , @Parameter(description = "Optimistic locking revision") @RequestParam String revision) {

        LOG.info("createNewCsafDocumentVersion {} {}", advisoryId, revision);

        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }


    /**
     * Delete  CSAF document
     * @param advisoryId advisoryId id of the CSAF document to delete
     * @param revision optimistic locking revision
     */
    @Operation(summary = "Delete advisory."
            , description = "Delete advisory from the system. All older versions, comments and audit-trails are also deleted.", tags = { "Advisory" })
    @DeleteMapping("/{advisoryId}/")
    public void deleteCsafDocument(
                @Parameter(description = "Id of the advisory to delete") @PathVariable String advisoryId
            ,   @Parameter(description = "Optimistic locking revision") @RequestParam String revision  ) throws DatabaseException {

        LOG.info("deleteCsafDocument");
        this.couchdDbService.deleteCsafDokument(advisoryId, revision);
    }

    /**
     * Get list of all templates in the system
     * @return list of all templates
     */
    @GetMapping("/templates")
    @Operation(summary = "Get all authorized templates", description = "Get all available templates in the system", tags = { "Advisory" })
    public List<AdvisoryTemplateInformationResponse> listAllTemplates() {

        LOG.info("listAllTemplates");
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
    public AdvisoryTemplateResponse readTemplate(
            @Parameter(description = "Id of the template to read") @PathVariable long templateId) {

        LOG.info("readTemplate {}", templateId);
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

        LOG.info("exportAdvisory to format: {} {}", advisoryId, format);
        return "";
    }

    /**
     * Change workflow state of a CSAF document to Draft
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision optimistic locking revision
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change workflow state of an advisory to Draft",
               description = "Change the workflow state of the advisory with the given id to Draft", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/workflowstate/Draft")
    public AdvisoryUpdateResponse setWorkflowStateToDraft(
              @Parameter(description = "Id of the advisory to change the workflow state")  @PathVariable String advisoryId
            , @Parameter(description = "Optimistic locking revision") @RequestParam String revision) {

        LOG.info("setWorkflowStateToDraft {} {}", advisoryId, revision);
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Change workflow state of a CSAF document to Review
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision optimistic locking revision
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change workflow state of an advisory to Review",
            description = "Change the workflow state of the advisory with the given id to Review", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/workflowstate/Review")
    public AdvisoryUpdateResponse setWorkflowStateToReview(
            @Parameter(description = "Id of the advisory to change the workflow state")  @PathVariable String advisoryId
            , @Parameter(description = "Optimistic locking revision") @RequestParam String revision) {

        LOG.info("setWorkflowStateToReview {} {}", advisoryId, revision);
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Change workflow state of a CSAF document to Approve
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision optimistic locking revision
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change workflow state of an advisory to Approve",
            description = "Change the workflow state of the advisory with the given id to Approve", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/workflowstate/Approve")
    public AdvisoryUpdateResponse setWorkflowStateToApprove(
            @Parameter(description = "Id of the advisory to change the workflow state")  @PathVariable String advisoryId
            , @Parameter(description = "Optimistic locking revision") @RequestParam String revision) {

        LOG.info("setWorkflowStateToApprove {} {}", advisoryId, revision);
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Change workflow state of a CSAF document to RfPublication
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision optimistic locking revision
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change workflow state of an advisory to RfPublication",
            description = "Change the workflow state of the advisory with the given id to RfPublication (Request for Publication)", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/workflowstate/RfPublication")
    public AdvisoryUpdateResponse setWorkflowStateToRfPublication(
            @Parameter(description = "Id of the advisory to change the workflow state")  @PathVariable String advisoryId
            , @Parameter(description = "Optimistic locking revision") @RequestParam String revision
            , @Parameter(description = "Proposed Time at which the publication should take place") @RequestParam(required=false) String proposedTime) {

        LOG.info("setWorkflowStateToPublish {} {} {}",advisoryId, revision, proposedTime );
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Change workflow state of a CSAF document to Publish
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision optimistic locking revision
     * @param proposedTime optimistic locking revision
     * @param documentTrackingStatus the new Document Tracking Status of the CSAF Document, only
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change workflow state of an advisory to Publish",
            description = "Change the workflow state of the advisory with the given id to Publish", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/workflowstate/Publish")
    public AdvisoryUpdateResponse setWorkflowStateToPublish(
            @Parameter(description = "Id of the advisory to change the workflow state")  @PathVariable String advisoryId
            , @Parameter(description = "Optimistic locking revision") @RequestParam String revision
            , @Parameter(description = "Proposed Time at which the publication should take place") @RequestParam(required=false) String proposedTime
            , @Parameter(description = "The new Document Tracking Status of the CSAF Document. Only interim and final are allowed") @RequestParam DocumentTrackingStatus documentTrackingStatus) {

        LOG.info("setWorkflowStateToPublish {} {} {} {}",advisoryId, revision, proposedTime, documentTrackingStatus );
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Get a list of all comments and answers of an CSAF document
     * @param advisoryId  id of the CSAF document to add the answer
     * @return list of comments and their metadata
     */
    @Operation( summary = "Show comments and answers of an advisory",
                description = "Show all comments and answers of the advisory with the given advisoryId", tags = { "Advisory" })
    @GetMapping("/{advisoryId}/comments/")
    public List<AdvisoryCommentResponse> listComments(
            @Parameter(description = "Id of the advisory to get the comments") @PathVariable String advisoryId) {

        LOG.info("listComments {}", advisoryId);
        return Collections.emptyList();
    }

    /**
     * Add comment to an advisory
     * @param advisoryId id of the CSAF document to add the comment
     * @param commentText text content of the comment
     *
     */
    @Operation(summary = "Add comment to an advisory",
               description = "Add a comment to the advisory with the given id.", tags = { "Advisory" })
    @PostMapping("/{advisoryId}/comments")
    public AdvisoryCreateResponse createComment(
            @Parameter(description = "AdvisoryId of the advisory to add the comments") @PathVariable String advisoryId
            , @RequestBody AdvisoryCreateCommentRequest commentText) {

        LOG.info("createComment {} {}", advisoryId, commentText);
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
    public AdvisoryCreateResponse addAnswer(
            @Parameter(description = "Id of the advisory to add the answer") @PathVariable long advisoryId
            , @Parameter(description = "Id of the comment to add the answer") @PathVariable long commentId
            , @RequestBody String answerText) {

        LOG.info("addAnswer {} {} {}", advisoryId, commentId, answerText);
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
    @Operation(summary = "Change comment text of an advisory", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/comments/{commentId}")
    public AdvisoryUpdateResponse changeComment(
            @Parameter(description = "Id of the advisory to change the comment") @PathVariable long advisoryId
            , @Parameter(description = "Id of the comment to change") @PathVariable long commentId
            , @Parameter(description = "Optimistic locking revision of the comment")  @RequestParam String revision
            , @RequestBody String newCommentText) {

        LOG.info("changeComment {} {} {} {}", advisoryId, commentId,revision, newCommentText);
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
    @Operation(summary = "Change answer text to an advisory comment", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/comments/{commentId}/answer/{answerId}")
    public AdvisoryUpdateResponse changeAnswer(
                @Parameter(description = "Id of the advisory to change the answer") @PathVariable long advisoryId
                , @Parameter(description = "Id of the comment the answer belongs to") @PathVariable long commentId
                , @Parameter(description = "Id of the answer to change") @PathVariable long answerId
                , @Parameter(description = "Optimistic locking revision of the answer")  @RequestParam String revision
                , @RequestBody String newAnswerText) {

        LOG.info("changeAnswer {} {} {} {} {}", advisoryId, commentId, answerId, revision, newAnswerText );
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

}
