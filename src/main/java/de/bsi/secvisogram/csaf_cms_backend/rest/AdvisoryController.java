package de.bsi.secvisogram.csaf_cms_backend.rest;


import com.fasterxml.jackson.databind.JsonNode;
import de.bsi.secvisogram.csaf_cms_backend.SecvisogramApplication;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.DatabaseException;
import de.bsi.secvisogram.csaf_cms_backend.couchdb.IdNotFoundException;
import de.bsi.secvisogram.csaf_cms_backend.model.DocumentTrackingStatus;
import de.bsi.secvisogram.csaf_cms_backend.model.ExportFormat;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.model.template.DocumentTemplateDescription;
import de.bsi.secvisogram.csaf_cms_backend.model.template.DocumentTemplateService;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.Comment;
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
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
        description = "API for for Creating, Retrieving, Updating and Deleting of CSAF documents," +
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
    @Operation(
            summary = "Get all authorized advisories.",
            description = "All CSAF documents for which the logged in user is authorized are returned." +
                          " This depends on the user's role and the state of the CSAF document.",
            tags = {"Advisory"}
    )
    public ResponseEntity<List<AdvisoryInformationResponse>> listCsafDocuments(
            @RequestParam(required = false)
            @Parameter(
                    in = ParameterIn.QUERY,
                    name = "expression",
                    description = "The filter expression in JSON format.",
                    schema = @Schema(
                            type = "string",
                            format = "json",
                            description = "An optional expression to filter documents by."
                    )
            ) String expression
    ) {

        LOG.info("findAdvisories {} ", sanitize(expression));
        return ResponseEntity.ok(advisoryService.getAdvisoryInformations());
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

        LOG.info("readCsafDocument");
        checkValidUuid(advisoryId);
        try {
            return ResponseEntity.ok(advisoryService.getAdvisory(advisoryId));
        } catch (IdNotFoundException idNfEx) {
            LOG.info("Advisory with given ID not found");
            return ResponseEntity.notFound().build();
        } catch (DatabaseException e) {
            LOG.info("Error reading Advisory");
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new CSAF document
     *
     * @param newCsafJson content of the new CSAF document
     * @return response with id and revision of the newly created advisory
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a new Advisory.",
            description = "Create a new CSAF document with added node IDs in the system.",
            tags = {"Advisory"},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "An advisory in CSAF JSON format including node IDs.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    title = "Common Security Advisory Framework schema extended with node IDs.",
                                    description = "See the base schema at http://docs.oasis-open.org/csaf/csaf/v2.0/csd02/schemas/csaf_json_schema.json."
                            ),
                            examples = {@ExampleObject(
                                    name = "A CSAF document in JSON format including additional node IDs.",
                                    value = "{$nodeId: \"nodeId123\", document: { $nodeId: \"nodeId567\", category: \"CSAF Base\",... }, vulnerabilities: {...}}"
                            )}
                    )
            )
    )
    public ResponseEntity<EntityCreateResponse> createCsafDocument(@RequestBody String newCsafJson) {

        LOG.info("createCsafDocument");
        try {
            IdAndRevision idRev = advisoryService.addAdvisory(newCsafJson);
            URI advisoryLocation = URI.create("advisories/" + idRev.getId());
            EntityCreateResponse createResponse = new EntityCreateResponse(idRev.getId(), idRev.getRevision());
            return ResponseEntity.created(advisoryLocation).body(createResponse);
        } catch (IOException jpEx) {
            return ResponseEntity.badRequest().build();
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
    @Operation(
            summary = "Change advisory.",
            description = "Change a CSAF document in the system. On saving a document its content (version) may change " +
                          " Thus, after changing a document, it must be reloaded on the client side.",
            tags = {"Advisory"}
    )
    public ResponseEntity<EntityUpdateResponse> changeCsafDocument(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change."
            ) String advisoryId,
            @RequestParam
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "An advisory in CSAF JSON format including node IDs.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    title = "Common Security Advisory Framework schema extended with node IDs.",
                                    description = "See the base schema at http://docs.oasis-open.org/csaf/csaf/v2.0/csd02/schemas/csaf_json_schema.json."
                            ),
                            examples = {@ExampleObject(
                                    name = "A CSAF document in JSON format including node IDs.",
                                    value = "{$nodeId: \"nodeId123\", document: { $nodeId: \"nodeId567\", category: \"CSAF Base\",... }, vulnerabilities: {...}}"
                            )}
                    )
            ) @RequestBody String changedCsafJson
    ) throws IOException {

        LOG.info("changeCsafDocument");
        checkValidUuid(advisoryId);
        try {
            String newRevision = advisoryService.updateAdvisory(advisoryId, revision, changedCsafJson);
            return ResponseEntity.ok(new EntityUpdateResponse(newRevision));
        } catch (IdNotFoundException idNfEx) {
            LOG.info("Advisory with given ID not found");
            return ResponseEntity.notFound().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Increase version of a CSAF document
     *
     * @param advisoryId ID of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return response with the new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/csaf/document/tracking/version")
    @Operation(
            summary = "Increase version of an advisory.",
            description = "Increase the version of a CSAF document.",
            tags = {"Advisory"}
    )
    public EntityUpdateResponse createNewCsafDocumentVersion(
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

        // only for debugging, remove when implemented
        LOG.info("createNewCsafDocumentVersion {} {}", sanitize(advisoryId), sanitize(revision));

        return new EntityUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
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

        LOG.info("deleteCsafDocument");
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

        LOG.info("listAllTemplates");

        try {
            var response =  Arrays.stream(this.templateService.getAllTemplates())
                    .map(template -> new AdvisoryTemplateInfoResponse(template.getId(), template.getDescription()))
                    .collect(Collectors.toList());
            return  ResponseEntity.ok(response);
        } catch (IOException ex) {
            LOG.error("Error loading templates", ex);
            return ResponseEntity.internalServerError().build();
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
            description = "Get the content of the templates with the given templateId.",
            tags = {"Advisory"}
    )
    public ResponseEntity<JsonNode> readTemplate(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the template to read."
            ) String templateId
    ) {

        // only for debugging, remove when implemented
        LOG.info("readTemplate {}", sanitize(templateId));
        try {
            Optional<DocumentTemplateDescription> template = this.templateService.getTemplateForId(templateId);
            if (template.isPresent()) {
                return  ResponseEntity.ok(template.get().getFileAsJsonNode());
            } else {
                return  ResponseEntity.notFound().build();
            }
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
    public String exportAdvisory(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to export."
            ) String advisoryId,
            @RequestParam(required = false)
            @Parameter(
                    description = "The format in which the document shall be exported."
            ) ExportFormat format
    ) {

        // only for debugging, remove when implemented
        LOG.info("exportAdvisory to format: {} {}", sanitize(format), sanitize(advisoryId));
        checkValidUuid(advisoryId);
        return "";
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
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change the workflow state of."
            ) String advisoryId,
            @RequestParam
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision
    ) throws IOException {

        LOG.info("setWorkflowStateToDraft {} {}", sanitize(advisoryId), sanitize(revision));
        checkValidUuid(advisoryId);
        try {
            advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.Draft);
            return ResponseEntity.ok().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
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
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change the workflow state of."
            ) String advisoryId,
            @RequestParam
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision
    ) throws IOException {

        // only for debugging, remove when implemented
        LOG.info("setWorkflowStateToReview {} {}", sanitize(advisoryId), sanitize(revision));
        try {
            advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.Review);
            return ResponseEntity.ok().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Change workflow state of a CSAF document to Approve
     *
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/workflowstate/Approve")
    @Operation(
            summary = "Change workflow state of an advisory to Approve.",
            description = "Change the workflow state of the advisory with the given id to Approve.",
            tags = {"Advisory"}
    )
    public ResponseEntity<String> setWorkflowStateToApprove(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change the workflow state of."
            ) String advisoryId,
            @RequestParam
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision
    ) throws IOException {

        // only for debugging, remove when implemented
        LOG.info("setWorkflowStateToApprove {} {}", sanitize(advisoryId), sanitize(revision));
        checkValidUuid(advisoryId);
        try {
            advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.Approved);
            return ResponseEntity.ok().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        }
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
            description = "Change the workflow state of the advisory with the given id to RfPublication" +
                          " (Request for Publication).",
            tags = {"Advisory"}
    )
    public ResponseEntity<String> setWorkflowStateToRfPublication(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change the workflow state of."
            ) String advisoryId,
            @RequestParam @Parameter(description = "The optimistic locking revision.") String revision,
            @RequestParam(required = false)
            @Parameter(
                    description = "Proposed Time at which the publication should take place.")
                    String proposedTime
    ) throws IOException {

        // only for debugging, remove when implemented
        LOG.info("setWorkflowStateToPublish {} {} {}", sanitize(advisoryId), sanitize(revision), sanitize(proposedTime));
        checkValidUuid(advisoryId);
        try {
            advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.RfPublication);
            return ResponseEntity.ok().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Change workflow state of a CSAF document to Publish
     *
     * @param advisoryId             advisoryId id of the CSAF document to change
     * @param revision               optimistic locking revision
     * @param proposedTime           optimistic locking revision
     * @param documentTrackingStatus the new Document Tracking Status of the CSAF Document
     * @return new optimistic locking revision
     */
    @Operation(
            summary = "Change workflow state of an advisory to Publish.",
            description = "Change the workflow state of the advisory with the given id to Publish.",
            tags = {"Advisory"}
    )
    @PatchMapping("/{advisoryId}/workflowstate/Publish")
    public ResponseEntity<String> setWorkflowStateToPublish(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change the workflow state of."
            ) String advisoryId,
            @RequestParam @Parameter(description = "Optimistic locking revision.") String revision,
            @RequestParam(required = false)
            @Parameter(description = "Proposed Time at which the publication should take place.") String proposedTime,
            @RequestParam
            @Parameter(
                    description = "The new Document Tracking Status of the CSAF Document." +
                                  " Only interim and final are allowed."
            ) DocumentTrackingStatus documentTrackingStatus
    ) throws IOException {

        // only for debugging, remove when implemented
        LOG.info("setWorkflowStateToPublish {} {} {} {}",
                sanitize(advisoryId), sanitize(revision), sanitize(proposedTime), sanitize(documentTrackingStatus));
        checkValidUuid(advisoryId);
        try {
            advisoryService.changeAdvisoryWorkflowState(advisoryId, revision, WorkflowState.Published);
            return ResponseEntity.ok().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get a list of all comments and answers of an CSAF document
     *
     * @param advisoryId id of the CSAF document to get comment ids for
     * @return list of comment ids
     */
    @Operation(
            summary = "Show comments and answers of an advisory.",
            description = "Show all comments and answers of the advisory with the given advisoryId.",
            tags = {"Advisory"}
    )
    @GetMapping("/{advisoryId}/comments")
    public ResponseEntity<List<CommentInformationResponse>> listComments(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to get the comments of."
            ) String advisoryId
    ) throws IOException {

        return ResponseEntity.ok(advisoryService.getComments(advisoryId));
    }

    /**
     * Create a new comment in the system, belonging to the advisory with given ID
     *
     * @param advisoryId      the ID of the advisory to add the comment to
     * @param newComment      the comment to add as JSON string
     */
    @PostMapping("/{advisoryId}/comments")
    @Operation(
            summary = "Create a new comment in the system.",
            description = "Creates a new comment associated with the advisory with the given ID." +
                          " The comments are generated independently of the CSAF document.",
            tags = {"Advisory"}
    )
    public ResponseEntity<EntityCreateResponse> createComment(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to add the comments to."
            ) String advisoryId,
            @RequestBody Comment newComment) {

        checkValidUuid(advisoryId);
        try {
            IdAndRevision idRev = advisoryService.addComment(advisoryId, newComment);
            URI advisoryLocation = URI.create("advisories/" + advisoryId + "/comments/" + idRev.getId());
            EntityCreateResponse createResponse = new EntityCreateResponse(idRev.getId(), idRev.getRevision());
            return ResponseEntity.created(advisoryLocation).body(createResponse);
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (DatabaseException dbEx) {
            LOG.error("Error creating comment");
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Add an answer to a comment of an advisory
     *
     * @param advisoryId ID of the CSAF document to add the answer to
     * @param commentId  ID of the comment to add the answer to
     * @param answerText new text content of the answer
     */
    @Operation(
            summary = "Add an answer to an advisory comment.",
            description = "Add a answer to the comment with the given ID.",
            tags = {"Advisory"}
    )
    @PostMapping("/{advisoryId}/comments/{commentId}/answer")
    public EntityCreateResponse addAnswer(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory the answered comment belongs to."
            ) String advisoryId,
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the comment to add the answer to."
            ) long commentId,
            @RequestBody String answerText
    ) {

        // only for debugging, remove when implemented
        LOG.info("addAnswer {} {} {}", sanitize(advisoryId), sanitize(commentId), sanitize(answerText));
        return new EntityCreateResponse(UUID.randomUUID().toString(), "2-efaa5db9409b2d4300535c70aaf6a66b");
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
            tags = {"Advisory"}
    )
    public ResponseEntity<EntityUpdateResponse> changeComment(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory a comment of."
            ) String advisoryId,
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the comment to change."
            ) String commentId,
            @RequestParam @Parameter(description = "Optimistic locking revision.") String revision,
            @RequestBody String newCommentText
    ) {

        checkValidUuid(advisoryId);
        checkValidUuid(commentId);
        try {
            String newRevision = advisoryService.updateComment(commentId, revision, newCommentText);
            return ResponseEntity.ok(new EntityUpdateResponse(newRevision));
        } catch (IdNotFoundException idNfEx) {
            LOG.info("Comment with given ID not found");
            return ResponseEntity.notFound().build();
        } catch (DatabaseException dbEx) {
            return ResponseEntity.badRequest().build();
        } catch (IOException ioEx) {
            return ResponseEntity.internalServerError().build();
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
    @Operation(
            summary = "Change answer text to an advisory comment.",
            description = "Change the text of an answer to a comment.",
            tags = {"Advisory"}
    )
    @PatchMapping("/{advisoryId}/comments/{commentId}/answer/{answerId}")
    public EntityUpdateResponse changeAnswer(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory the answered comment belongs to."
            ) String advisoryId,
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the comment to change an answer of."
            ) long commentId,
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the answer to change."
            ) long answerId,
            @RequestParam
            @Parameter(description = "Optimistic locking revision of the answer.") String revision,
            @RequestBody String newAnswerText
    ) {

        // only for debugging, remove when implemented
        LOG.info("changeAnswer {} {} {} {} {}", sanitize(advisoryId), sanitize(commentId),
                sanitize(answerId), sanitize(revision), sanitize(newAnswerText));
        return new EntityUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Sanitize request parameter
     */
    private String sanitize(Object value) {
        return (value != null) ? value.toString().replaceAll("[\r\n]", "") : "";
    }

    /**
     * Check whether the given id is a valid uuid
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
