package de.bsi.secvisogram.csaf_cms_backend.rest;


import com.fasterxml.jackson.databind.JsonNode;
import de.bsi.secvisogram.csaf_cms_backend.SecvisogramApplication;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.IdNotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.template.DocumentTemplateService;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateAdvisoryRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateCommentRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.*;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import de.bsi.secvisogram.csaf_cms_backend.service.IdAndRevision;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * API for Creating, Retrieving, Updating and Deleting CSAF Documents,
 * including their Versions, Audit Trails, Comments and Workflow States.
 */
@RestController
@RequestMapping(SecvisogramApplication.BASE_ROUTE + "advisories")
@Tag(
        name = "Advisory",
        description = "API for Creating, Retrieving, Updating and Deleting of CSAF documents," +
                      " including their Versions, Comments and Workflow States."
)
public class AdvisoryController {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryController.class);

    @Autowired
    private AdvisoryService advisoryService;

    @Autowired
    private DocumentTemplateService templateService;

    /**
     * Read all advisories, optionally filtered by a search expression
     *
     * @param expression optional search expression as json string
     * @return response with list of advisories satisfying the search criteria
     */
    @GetMapping("")
    @Operation(summary = "Get all authorized advisories.", tags = {"Advisory"},
            description = "All CSAF documents for which the logged in user is authorized are returned." +
                          " This depends on the user's role and the state of the CSAF document.")
    public ResponseEntity<List<AdvisoryInformationResponse>> listCsafDocuments(
            @RequestParam(required = false)
            @Parameter(in = ParameterIn.QUERY, name = "expression",
                    description = """
                            The filter expression in JSON format. Example to find documents with title equal 'title1': { "type" : "Operator",
                              "selector" : [ "csaf", "document", "title" ],
                              "operatorType" : "Equal",
                              "value" : "title1",
                              "valueType" : "Text"
                            }.
                             Possible operatorType's: 'Equal', 'NotEqual', 'Greater', 'GreaterOrEqual', 'Less', 'LessOrEqual', 'ContainsIgnoreCase'.
                             Possible valueType's: 'Text', 'Decimal', 'Boolean'. You can search for all attributes in 'csaf/document""",
                    schema = @Schema(type = "string", format = "json",
                            description = "An optional expression in JSON to filter documents by.")
            )
            String expression
    ) {

        LOG.debug("findAdvisories");
        try {
            return ResponseEntity.ok(advisoryService.getAdvisoryInformations(expression));
        } catch (IOException e) {
            LOG.info("Error reading Advisory");
            return ResponseEntity.internalServerError().build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }
    }


    /**
     * Get a single advisory
     *
     * @param advisoryId ID of the CSAF document that should be read
     * @return response with the requested CSAF document
     */
    @GetMapping("/{advisoryId}")
    @Operation(
            summary = "Get a single Advisory.",
            description = "Get the advisory CSAF document and some additional data for the given advisoryId.",
            tags = {"Advisory"}
    )
    public ResponseEntity<AdvisoryResponse> readCsafDocument(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to read."
            ) String advisoryId
    ) {

        LOG.debug("readCsafDocument");
        checkValidUuid(advisoryId);
        try {
            return ResponseEntity.ok(advisoryService.getAdvisory(advisoryId));
        } catch (IdNotFoundException idNfEx) {
            LOG.info("Advisory with given ID not found");
            return ResponseEntity.notFound().build();
        } catch (DatabaseException e) {
            LOG.info("Error reading Advisory");
            return ResponseEntity.internalServerError().build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }
    }

    /**
     * Create a new CSAF document
     *
     * @param newCsafJson content of the new CSAF document
     * @return response with id and revision of the newly created advisory
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new Advisory.", tags = {"Advisory"},
            description = "Create a new CSAF document with added node IDs in the system. It possible to add an summary " +
                          "and a legacy version information for the revision history.")
    public ResponseEntity<EntityCreateResponse> createCsafDocument(@RequestBody CreateAdvisoryRequest newCsafJson) {

        LOG.debug("createCsafDocument");
        try {
            IdAndRevision idRev = advisoryService.addAdvisory(newCsafJson);
            URI advisoryLocation = URI.create("advisories/" + idRev.getId());
            EntityCreateResponse createResponse = new EntityCreateResponse(idRev.getId(), idRev.getRevision());
            return ResponseEntity.created(advisoryLocation).body(createResponse);
        } catch (IOException jpEx) {
            return ResponseEntity.badRequest().build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }
    }

    /**
     * Create a new CSAF document
     *
     * @param newCsafJson content of the new CSAF document
     * @return response with id and revision of the newly created advisory
     */
    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Import a new Advisory.", tags = {"Advisory"},
            description = "Import a new CSAF document into the system.")
    public ResponseEntity<EntityCreateResponse> importCsafDocument(@RequestBody JsonNode newCsafJson) {

        try {
            LOG.debug("importCsafDocument");
            IdAndRevision idRev = advisoryService.importAdvisory(newCsafJson);
            URI advisoryLocation = URI.create("advisories/" + idRev.getId());
            EntityCreateResponse createResponse = new EntityCreateResponse(idRev.getId(), idRev.getRevision());
            return ResponseEntity.created(advisoryLocation).body(createResponse);
        } catch (IOException jpEx) {
            return ResponseEntity.badRequest().build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }
    }

    /**
     * Change a CSAF document
     *
     * @param advisoryId ID of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return response with the new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}")
    @Operation(summary = "Change advisory.", tags = {"Advisory"},
            description = "Change a CSAF document in the system. On saving a document its content (version) may change " +
                          " Thus, after changing a document, it must be reloaded on the client side.")
    public ResponseEntity<EntityUpdateResponse> changeCsafDocument(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to change.")
            String advisoryId,
            @RequestParam
            @Parameter(description = "The optimistic locking revision.")
            String revision,
            @RequestBody
            CreateAdvisoryRequest changedCsafJson
    ) throws IOException {

        LOG.debug("changeCsafDocument");
        checkValidUuid(advisoryId);
        try {
            String newRevision = advisoryService.updateAdvisory(advisoryId, revision, changedCsafJson);
            return ResponseEntity.ok(new EntityUpdateResponse(newRevision));
        } catch (IdNotFoundException idNfEx) {
            LOG.info("Advisory with given ID not found");
            return ResponseEntity.notFound().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }

    }

    /**
     * Increase version of a CSAF document
     *
     * @param advisoryId ID of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return response with the new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/createNewVersion")
    @Operation(
            summary = "Increase version of an advisory.",
            description = "Increase the version of a CSAF document. This can only be done in workflow state Published",
            tags = {"Advisory"}
    )
    public ResponseEntity<String> createNewCsafDocumentVersion(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to change.")
            String advisoryId,
            @RequestParam
            @Parameter(description = "The optimistic locking revision.")
            String revision
    ) {

        LOG.debug("createNewCsafDocumentVersion");
        checkValidUuid(advisoryId);

        try {
            String newRevision = advisoryService.createNewCsafDocumentVersion(advisoryId, revision);
            return ResponseEntity.ok(newRevision);
        } catch (IOException | DatabaseException ex) {
            return ResponseEntity.badRequest().build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }
    }


    /**
     * Delete a CSAF document
     *
     * @param advisoryId advisoryId id of the CSAF document to delete
     * @param revision   optimistic locking revision
     */
    @DeleteMapping("/{advisoryId}")
    @Operation(
            summary = "Delete an advisory.",
            description = "Delete a CSAF document from the system. All older versions of the document, corresponding" +
                          " comments and audit-trails are also deleted.",
            tags = {"Advisory"}
    )
    public ResponseEntity<Void> deleteCsafDocument(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change."
            ) String advisoryId,
            @RequestParam
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision
    ) {

        LOG.debug("deleteCsafDocument");
        checkValidUuid(advisoryId);
        try {
            advisoryService.deleteAdvisory(advisoryId, revision);
            return ResponseEntity.ok().build();
        } catch (IdNotFoundException idNfEx) {
            LOG.info("Advisory with given ID not found");
            return ResponseEntity.notFound().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        } catch (IOException ioEx) {
            return ResponseEntity.internalServerError().build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }
    }

    /**
     * Get a list of all templates in the system
     *
     * @return list of all templates
     */
    @GetMapping("/templates")
    @Operation(
            summary = "Get all authorized templates.",
            description = "Get all available templates in the system.",
            tags = {"Advisory"}
    )
    public ResponseEntity<List<AdvisoryTemplateInfoResponse>> listAllTemplates() {

        LOG.debug("listAllTemplates");

        try {
            var response = Arrays.stream(this.templateService.getAllTemplates())
                    .map(template -> new AdvisoryTemplateInfoResponse(template.getId(), template.getDescription()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            LOG.error("Error loading templates", ex);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * Get the content of a template
     *
     * @param templateId ID of the template that should be read
     * @return the requested template
     */
    @GetMapping("/templates/{templateId}")
    @Operation(
            summary = "Get template content.",
            description = "Get the content of the template with the given templateId.",
            tags = {"Advisory"}
    )
    public ResponseEntity<JsonNode> readTemplate(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the template to read."
            ) String templateId
    ) {

        LOG.debug("readTemplate");
        try {
            Optional<JsonNode> templateJson = this.templateService.getTemplate(templateId);
            return templateJson.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IOException ex) {
            LOG.error(String.format("Error loading template with id: %s", sanitize(templateId)), ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export a CSAF document
     *
     * @param advisoryId ID of the CSAF document that should be exported
     * @param format     optional format of the result, defaults to JSON
     * @return the converted advisory
     */
    @GetMapping(
            value = "/{advisoryId}/csaf",
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.TEXT_HTML_VALUE,
                    MediaType.TEXT_MARKDOWN_VALUE,
                    MediaType.APPLICATION_PDF_VALUE
            })
    @Operation(
            summary = "Export a CSAF document.",
            description = "Export advisory csaf in different formats, possible formats are: PDF, Markdown, HTML, JSON.",
            tags = {"Advisory"}
    )
    public ResponseEntity<InputStreamResource> exportAdvisory(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to export.")
            String advisoryId,
            @RequestParam(required = false)
            @Parameter(description = "The format in which the document shall be exported.")
            ExportFormat format
    ) {
        LOG.debug("exportAdvisory");
        checkValidUuid(advisoryId);
        Path filePath = null;
        try {
            // export to local temporary file
            filePath = advisoryService.exportAdvisory(advisoryId, format);

            // return the export file through a stream (should be okay even with big files)
            final InputStream inputStream = Files.newInputStream(filePath);
            final InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
            return ResponseEntity.ok()
                    .contentLength(Files.size(filePath))
                    .contentType(determineExportResponseContentType(format))
                    .body(inputStreamResource);
        } catch (IOException e) {
            LOG.error("Error happened when creating the export: ", e);
            return ResponseEntity.internalServerError().build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        } finally {
            if (filePath != null) {
                boolean result = filePath.toFile().delete();
                if (!result) {
                    LOG.error("Could not delete temporary file {} after exporting.", filePath);
                }
            }
        }
    }

    static MediaType determineExportResponseContentType(@Nullable final ExportFormat format) {
        if (format == ExportFormat.PDF) {
            return MediaType.APPLICATION_PDF;
        } else if (format == ExportFormat.Markdown) {
            return MediaType.TEXT_MARKDOWN;
        } else if (format == ExportFormat.HTML) {
            return MediaType.TEXT_HTML;
        } else if (format == ExportFormat.JSON) {
            return MediaType.APPLICATION_JSON;
        } else {
            return MediaType.APPLICATION_JSON;
        }
    }

    /**
     * Change workflow state of a CSAF document to Draft
     *
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/workflowstate/Draft")
    @Operation(
            summary = "Change workflow state of an advisory to Draft.",
            description = "Change the workflow state of the advisory with the given id to Draft.",
            tags = {"Advisory"}
    )
    public ResponseEntity<String> setWorkflowStateToDraft(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to change the workflow state of.")
            String advisoryId,
            @RequestParam
            @Parameter(description = "The optimistic locking revision.")
            String revision
    ) throws IOException {

        LOG.debug("setWorkflowStateToDraft");
        checkValidUuid(advisoryId);
        return changeWorkflowState(advisoryId, revision, WorkflowState.Draft);
    }

    private ResponseEntity<String> changeWorkflowState(String advisoryId, String revision, WorkflowState state) throws IOException {

        return this.changeWorkflowState(advisoryId, revision, state, null, null);
    }

    private ResponseEntity<String> changeWorkflowState(String advisoryId, String revision, WorkflowState state,
                                                       String proposedTime, DocumentTrackingStatus documentTrackingStatus) throws IOException {
        try {
            advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, state, proposedTime, documentTrackingStatus);
            return ResponseEntity.ok().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }
    }

    /**
     * Change workflow state of a CSAF document to Review
     *
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/workflowstate/Review")
    @Operation(
            summary = "Change workflow state of an advisory to Draft.",
            description = "Change the workflow state of the advisory with the given id to Review.",
            tags = {"Advisory"}
    )
    public ResponseEntity<String> setWorkflowStateToReview(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to change the workflow state of.")
            String advisoryId,
            @RequestParam
            @Parameter(description = "The optimistic locking revision.")
            String revision
    ) throws IOException {

        LOG.debug("setWorkflowStateToReview");
        checkValidUuid(advisoryId);
        return changeWorkflowState(advisoryId, revision, WorkflowState.Review);
    }

    /**
     * Change workflow state of a CSAF document to Approve
     *
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/workflowstate/Approved")
    @Operation(
            summary = "Change workflow state of an advisory to Approved.",
            description = "Change the workflow state of the advisory with the given id to Approve.",
            tags = {"Advisory"}
    )
    public ResponseEntity<String> setWorkflowStateToApproved(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to change the workflow state of.")
            String advisoryId,
            @RequestParam
            @Parameter(description = "The optimistic locking revision.")
            String revision
    ) throws IOException {

        LOG.debug("setWorkflowStateToApproved");
        checkValidUuid(advisoryId);
        return changeWorkflowState(advisoryId, revision, WorkflowState.Approved);
    }

    /**
     * Change workflow state of a CSAF document to RfPublication
     *
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/workflowstate/RfPublication")
    @Operation(
            summary = "Change workflow state of an advisory to RfPublication.",
            description = "Change the workflow state of the advisory with the given id to Request for Publication" +
                          " (Request for Publication).",
            tags = {"Advisory"}
    )
    public ResponseEntity<String> setWorkflowStateToRfPublication(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to change the workflow state of.")
            String advisoryId,
            @RequestParam @Parameter(description = "The optimistic locking revision.") String revision,
            @RequestParam(required = false)
            @Parameter(description = "Proposed Time at which the publication should take place as ISO-8601 UTC string.")
            String proposedTime
    ) throws IOException {

        LOG.debug("setWorkflowStateToRfPublication");
        checkValidUuid(advisoryId);
        return changeWorkflowState(advisoryId, revision, WorkflowState.RfPublication, proposedTime, null);
    }

    /**
     * Change workflow state of a CSAF document to Published
     *
     * @param advisoryId             advisoryId id of the CSAF document to change
     * @param revision               optimistic locking revision
     * @param proposedTime           optimistic locking revision
     * @param documentTrackingStatus the new Document Tracking Status of the CSAF Document
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change workflow state of an advisory to Published.", tags = {"Advisory"},
            description = "Change the workflow state of the advisory with the given id to Published.")
    @PatchMapping("/{advisoryId}/workflowstate/Published")
    public ResponseEntity<String> setWorkflowStateToPublished(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to change the workflow state of.")
            String advisoryId,
            @RequestParam @Parameter(description = "Optimistic locking revision.")
            String revision,
            @RequestParam(required = false)
            @Parameter(description = "Proposed Time at which the publication should take place as ISO-8601 UTC string.")
            String proposedTime,
            @RequestParam
            @Parameter(description = "The new Document Tracking Status of the CSAF Document." +
                                     " Only Interim and Final are allowed.")
            DocumentTrackingStatus documentTrackingStatus
    ) throws IOException {

        LOG.debug("setWorkflowStateToPublish");
        checkValidUuid(advisoryId);
        return changeWorkflowState(advisoryId, revision, WorkflowState.Published, proposedTime, documentTrackingStatus);
    }

    /**
     * Get a list of all comments of a CSAF document
     *
     * @param advisoryId id of the CSAF document to get comment ids for
     * @return list of comment ids
     */
    @Operation(summary = "Show comments of an advisory.", tags = {"Advisory"},
            description = "Show all comments of the advisory with the given advisoryId.")
    @GetMapping("/{advisoryId}/comments")
    public ResponseEntity<List<CommentInformationResponse>> listComments(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to get the comments of.")
            String advisoryId
    ) throws IOException {
        checkValidUuid(advisoryId);
        LOG.debug("listComments");
        try {
            return ResponseEntity.ok(advisoryService.getComments(advisoryId));
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Create a new comment in the system, belonging to the advisory with given ID
     *
     * @param advisoryId the ID of the advisory to add the comment to
     * @param newComment the comment to add as JSON string
     */
    @PostMapping("/{advisoryId}/comments")
    @Operation(
            summary = "Create a new comment in the system.", tags = {"Advisory"},
            description = "Creates a new comment associated with the advisory with the given ID." +
                          " The comments are generated independently of the CSAF document and may link" +
                          " to a specific node of the CSAF document by its $nodeId",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "A comment in JSON format.", required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(title = "Comment schema", description = "Comment schema with some metadata."),
                            examples = {@ExampleObject(
                                    name = "A comment with text, CSAF Node Id and fieldName",
                                    value = "{commentText: \"This is a comment\", csafNodeId: \"dd9683d8-be4b-4d09-a864-1a04092a071f\", fieldName: \"category\"}"
                            )}
                    )
            )
    )
    public ResponseEntity<EntityCreateResponse> createComment(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to add the comments to.")
            String advisoryId,
            @RequestBody CreateCommentRequest newComment) {

        LOG.debug("createComment");
        checkValidUuid(advisoryId);
        try {
            IdAndRevision idRev = advisoryService.addComment(advisoryId, newComment);
            URI commentLocation = URI.create("advisories/" + advisoryId + "/comments/" + idRev.getId());
            EntityCreateResponse createResponse = new EntityCreateResponse(idRev.getId(), idRev.getRevision());
            return ResponseEntity.created(commentLocation).body(createResponse);
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (DatabaseException dbEx) {
            LOG.error("Error creating comment");
            return ResponseEntity.internalServerError().build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Get a list of all answers of a comment
     *
     * @param advisoryId ID of the advisory, the comment for which to get answers for belongs to
     * @param commentId  ID of the comment to get answers for
     * @return list of answers
     */
    @Operation(
            summary = "Show answers of a comment.", tags = {"Advisory"},
            description = "Show all answers of the comment with the given commentId.")
    @GetMapping("/{advisoryId}/comments/{commentId}/answers")
    public ResponseEntity<List<AnswerInformationResponse>> listAnswers(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory to the comment belongs to.")
            String advisoryId,
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the comment to get answers of.")
            String commentId
    ) throws IOException {

        LOG.debug("listAnswers");
        checkValidUuid(advisoryId);
        checkValidUuid(commentId);
        try {
            return ResponseEntity.ok(advisoryService.getAnswers(advisoryId, commentId));
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Add an answer to a comment of an advisory
     *
     * @param advisoryId        ID of the CSAF document to add the answer to
     * @param commentId         ID of the comment to add the answer to
     * @param answerCommentText the answer text
     */
    @Operation(summary = "Add an answer to an advisory comment.", tags = {"Advisory"},
            description = "Add a answer to the comment with the given ID.")
    @PostMapping("/{advisoryId}/comments/{commentId}/answers")
    public ResponseEntity<EntityCreateResponse> addAnswer(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory the answered comment belongs to.")
            String advisoryId,
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the comment to add the answer to.")
            String commentId,
            @RequestBody String answerCommentText
    ) {

        LOG.debug("addAnswer");
        checkValidUuid(advisoryId);
        checkValidUuid(commentId);
        try {
            IdAndRevision idRev = advisoryService.addAnswer(advisoryId, commentId, answerCommentText);
            URI answerLocation = URI.create("advisories/" + advisoryId + "/comments/" + commentId + "/answers/" + idRev.getId());
            EntityCreateResponse createResponse = new EntityCreateResponse(idRev.getId(), idRev.getRevision());
            return ResponseEntity.created(answerLocation).body(createResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (DatabaseException dbEx) {
            LOG.error("Error creating answer");
            return ResponseEntity.internalServerError().build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Change the comment of an advisory
     *
     * @param advisoryId     ID of the CSAF document to add the answer
     * @param commentId      ID of the comment to change the answer
     * @param revision       optimistic locking revision
     * @param newCommentText new text content of the comment
     * @return new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/comments/{commentId}")
    @Operation(
            summary = "Change the text of a comment.",
            description = "Change the text of the comment with the given ID.",
            tags = {"Advisory"},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "A new comment text.", required = true,
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", format = "plain"),
                            examples = {@ExampleObject(name = "A comment text", value = "This is a new text for a comment.")}
                    )
            )
    )
    public ResponseEntity<EntityUpdateResponse> changeComment(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory a comment of.")
            String advisoryId,
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the comment to change.")
            String commentId,
            @RequestParam @Parameter(description = "Optimistic locking revision.")
            String revision,
            @RequestBody String newCommentText) {

        LOG.debug("changeComment");
        checkValidUuid(advisoryId);
        checkValidUuid(commentId);
        try {
            String newRevision = advisoryService.updateComment(advisoryId, commentId, revision, newCommentText);
            return ResponseEntity.ok(new EntityUpdateResponse(newRevision));
        } catch (IdNotFoundException idNfEx) {
            LOG.info("Comment with given ID not found");
            return ResponseEntity.notFound().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        } catch (IOException ioEx) {
            return ResponseEntity.internalServerError().build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }
    }

    /**
     * Change answer of a comment to an advisory
     *
     * @param advisoryId    ID of the CSAF document the answered comment belongs to
     * @param commentId     ID of the comment to change an answer of
     * @param answerId      ID of the answer to change
     * @param revision      optimistic locking revision
     * @param newAnswerText new text content of the answer
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change answer text to an advisory comment.", tags = {"Advisory"},
            description = "Change the text of an answer to a comment.")
    @PatchMapping("/{advisoryId}/comments/{commentId}/answers/{answerId}")
    public ResponseEntity<EntityUpdateResponse> changeAnswer(
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the advisory the answered comment belongs to.")
            String advisoryId,
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the comment to change an answer of.")
            String commentId,
            @PathVariable
            @Parameter(in = ParameterIn.PATH, description = "The ID of the answer to change.")
            String answerId,
            @RequestParam
            @Parameter(description = "Optimistic locking revision of the answer.")
            String revision,
            @RequestBody String newAnswerText) {

        LOG.debug("changeAnswer");
        checkValidUuid(advisoryId);
        checkValidUuid(commentId);
        checkValidUuid(answerId);
        try {
            String newRevision = advisoryService.updateComment(advisoryId, answerId, revision, newAnswerText);
            return ResponseEntity.ok(new EntityUpdateResponse(newRevision));
        } catch (IdNotFoundException idNfEx) {
            LOG.info("Comment with given ID not found");
            return ResponseEntity.notFound().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        } catch (IOException ioEx) {
            return ResponseEntity.internalServerError().build();
        } catch (AccessDeniedException adEx) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).build();
        }
    }

    /**
     * Sanitize request parameter
     */
    public static String sanitize(Object value) {
        return (value != null) ? value.toString().replaceAll("[\r\n]", "") : "";
    }

    /**
     * Check whether the given id is a valid uuid
     *
     * @param uuidString the string to check
     */
    private static void checkValidUuid(String uuidString) {
        try {
            UUID.fromString(uuidString);
        } catch (IllegalArgumentException iaEx) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid UUID!", iaEx);
        }
    }

}
