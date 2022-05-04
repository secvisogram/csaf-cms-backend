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

    private final AdvisoryJsonService jsonService = new AdvisoryJsonService();

    @Autowired
    private CouchDbService couchDbService;

    /**
     * Read all advisories, optionally filtered by a search expression
     *
     * @param expression optional search expression as json string
     * @return list of advisories satisfying the search criteria
     */
    @GetMapping("/")
    @Operation(
            summary = "Get all authorized advisories.",
            description = "All CSAF documents for which the logged in user is authorized are returned." +
                    " This depends on the user's role and the state of the CSAF document.",
            tags = {"Advisory"}
    )
    public List<AdvisoryInformationResponse> listCsafDocuments(
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
        return this.couchDbService.readAllCsafDocuments();
    }


    /**
     * Get a single advisory
     *
     * @param advisoryId ID of the CSAF document that should be read
     * @return the requested CSAF document
     */
    @GetMapping("/{advisoryId}/")
    @Operation(
            summary = "Get a single Advisory.",
            description = "Get the advisory CSAF document and some additional data for the given advisoryId.",
            tags = {"Advisory"}
    )
    public AdvisoryResponse readCsafDocument(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to read."
            ) String advisoryId
    ) throws IOException {

        LOG.info("readCsafDocument");
        JsonNode document = this.couchDbService.readCsafDocument(advisoryId);
        return jsonService.covertCoudbCsafToAdvisory(document, advisoryId);
    }

    /**
     * Create a new CSAF document
     *
     * @param newCsafJson content of the new CSAF document
     */
    @PostMapping(name = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a new Advisory.",
            description = "Create a new CSAF document with optional comments in the system.",
            tags = {"Advisory"},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "An advisory in CSAF JSON format including comments.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    title = "Common Security Advisory Framework schema extended with comments.",
                                    description = "See the base schema at http://docs.oasis-open.org/csaf/csaf/v2.0/csd02/schemas/csaf_json_schema.json."
                            ),
                            examples = {@ExampleObject(
                                    name = "A CSAF document in JSON format including comments.",
                                    value = "{document: { $comment: [23454], category: \"CSAF Base\",... }, vulnerabilities: {...}}"
                            )}
                    )
            )
    )
    public AdvisoryCreateResponse createCsafDocument(@RequestBody String newCsafJson) throws IOException {

        LOG.info("createCsafDocument");
        final InputStream csafStream = new ByteArrayInputStream(newCsafJson.getBytes(StandardCharsets.UTF_8));
        final String owner = "Musterman";
        ObjectNode objectNode = jsonService.convertCsafToJson(csafStream, owner, WorkflowState.Draft);
        final UUID uuid = UUID.randomUUID();
        final String revision = couchDbService.writeCsafDocument(uuid, objectNode);
        return new AdvisoryCreateResponse(uuid.toString(), revision);
    }

    /**
     * Change a CSAF document
     *
     * @param advisoryId ID of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/")
    @Operation(
            summary = "Change advisory.",
            description = "Change a CSAF document in the system. On saving a document its content (version) may change " +
                    " Thus, after changing a document, it must be reloaded on the client side.",
            tags = {"Advisory"}
    )
    public AdvisoryUpdateResponse changeCsafDocument(
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
                    description = "An advisory in CSAF JSON format including comments.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    title = "Common Security Advisory Framework schema extended with comments.",
                                    description = "See the base schema at http://docs.oasis-open.org/csaf/csaf/v2.0/csd02/schemas/csaf_json_schema.json."
                            ),
                            examples = {@ExampleObject(
                                    name = "A CSAF document in JSON format including comments.",
                                    value = "{document: { $comment: [23454], category: \"CSAF Base\",... }, vulnerabilities: {...}}"
                            )}
                    )
            ) @RequestBody String changedCsafJson
    ) throws IOException {

        LOG.info("changeCsafDocument");
        final InputStream csafStream = new ByteArrayInputStream(changedCsafJson.getBytes(StandardCharsets.UTF_8));
        final String owner = "Musterman";
        ObjectNode objectNode = jsonService.convertCsafToJson(csafStream, owner, WorkflowState.Draft);
        final String newRevision = couchDbService.updateCsafDocument(advisoryId, revision, objectNode);

        return new AdvisoryUpdateResponse(newRevision);
    }

    /**
     * Increase version of a CSAF document
     *
     * @param advisoryId ID of the CSAF document to change
     * @param revision   optimistic locking revision
     * @return new optimistic locking revision
     */
    @PatchMapping("/{advisoryId}/csaf/document/tracking/version")
    @Operation(
            summary = "Increase version of an advisory.",
            description = "Increase the version of a CSAF document.",
            tags = {"Advisory"}
    )
    public AdvisoryUpdateResponse createNewCsafDocumentVersion(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change."
            ) String advisoryId,
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision
    ) {

        // only for debugging, remove when implemented
        LOG.info("createNewCsafDocumentVersion {} {}", sanitize(advisoryId), sanitize(revision));

        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }


    /**
     * Delete a CSAF document
     *
     * @param advisoryId advisoryId id of the CSAF document to delete
     * @param revision   optimistic locking revision
     */
    @DeleteMapping("/{advisoryId}/")
    @Operation(
            summary = "Delete an advisory.",
            description = "Delete a CSAF document from the system. All older versions, comments and audit-trails are" +
                    " also deleted.",
            tags = {"Advisory"}
    )
    public void deleteCsafDocument(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change."
            ) String advisoryId,
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision
    ) throws DatabaseException {

        LOG.info("deleteCsafDocument");
        this.couchDbService.deleteCsafDocument(advisoryId, revision);
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
    public List<AdvisoryTemplateInformationResponse> listAllTemplates() {

        LOG.info("listAllTemplates");
        return Arrays.asList(
                new AdvisoryTemplateInformationResponse(1L, "Template for security incident response"),
                new AdvisoryTemplateInformationResponse(2L, "Template for informational Advisory"),
                new AdvisoryTemplateInformationResponse(3L, "Template for security Advisory")
        );
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
    public AdvisoryTemplateResponse readTemplate(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the template to read."
            ) long templateId
    ) {

        // only for debugging, remove when implemented
        LOG.info("readTemplate {}", sanitize(templateId));
        return null;
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
        LOG.info("exportAdvisory to format: {} {}", sanitize(advisoryId), sanitize(format));
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
    public AdvisoryUpdateResponse setWorkflowStateToDraft(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change the workflow state of."
            ) String advisoryId,
            @RequestParam
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision
    ) {

        // only for debugging, remove when implemented
        LOG.info("setWorkflowStateToDraft {} {}", sanitize(advisoryId), sanitize(revision));
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
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
    public AdvisoryUpdateResponse setWorkflowStateToReview(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change the workflow state of."
            ) String advisoryId,
            @RequestParam
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision
    ) {

        // only for debugging, remove when implemented
        LOG.info("setWorkflowStateToReview {} {}", sanitize(advisoryId), sanitize(revision));
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
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
    public AdvisoryUpdateResponse setWorkflowStateToApprove(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to change the workflow state of."
            ) String advisoryId,
            @RequestParam
            @Parameter(
                    description = "The optimistic locking revision."
            ) String revision
    ) {

        // only for debugging, remove when implemented
        LOG.info("setWorkflowStateToApprove {} {}", sanitize(advisoryId), sanitize(revision));
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
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
    public AdvisoryUpdateResponse setWorkflowStateToRfPublication(
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
    ) {

        // only for debugging, remove when implemented
        LOG.info("setWorkflowStateToPublish {} {} {}", sanitize(advisoryId), sanitize(revision), sanitize(proposedTime));
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Change workflow state of a CSAF document to Publish
     *
     * @param advisoryId             advisoryId id of the CSAF document to change
     * @param revision               optimistic locking revision
     * @param proposedTime           optimistic locking revision
     * @param documentTrackingStatus the new Document Tracking Status of the CSAF Document, only
     * @return new optimistic locking revision
     */
    @Operation(
            summary = "Change workflow state of an advisory to Publish.",
            description = "Change the workflow state of the advisory with the given id to Publish.",
            tags = {"Advisory"}
    )
    @PatchMapping("/{advisoryId}/workflowstate/Publish")
    public AdvisoryUpdateResponse setWorkflowStateToPublish(
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
    ) {

        // only for debugging, remove when implemented
        LOG.info("setWorkflowStateToPublish {} {} {} {}", sanitize(advisoryId), sanitize(revision), sanitize(proposedTime)
                , sanitize(documentTrackingStatus));
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Get a list of all comments and answers of an CSAF document
     *
     * @param advisoryId id of the CSAF document to add the answer
     * @return list of comments and their metadata
     */
    @Operation(
            summary = "Show comments and answers of an advisory.",
            description = "Show all comments and answers of the advisory with the given advisoryId.",
            tags = {"Advisory"}
    )
    @GetMapping("/{advisoryId}/comments/")
    public List<AdvisoryCommentResponse> listComments(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to get the comments of."
            ) String advisoryId
    ) {

        // only for debugging, remove when implemented
        LOG.info("listComments {}", sanitize(advisoryId));
        return Collections.emptyList();
    }

    /**
     * Add a comment to an advisory
     *
     * @param advisoryId  ID of the CSAF document to add the comment to
     * @param commentText text content of the comment
     */
    @PostMapping("/{advisoryId}/comments")
    @Operation(
            summary = "Add a comment to an advisory.",
            description = "Add a comment to the advisory with the given ID. The comments are generated independently" +
                    " of the CSAF document. The IDs of the comments must be added manually to the appropriate place in " +
                    "the CSAF document and then saved with the document.",
            tags = {"Advisory"}
    )
    public AdvisoryCreateResponse createComment(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory to add the comment to."
            ) String advisoryId,
            @RequestBody AdvisoryCreateCommentRequest commentText
    ) {

        // only for debugging, remove when implemented
        LOG.info("createComment {} {}", sanitize(advisoryId), sanitize(commentText));
        return new AdvisoryCreateResponse(UUID.randomUUID().toString(), "2-efaa5db9409b2d4300535c70aaf6a66b");
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
    public AdvisoryCreateResponse addAnswer(
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
        return new AdvisoryCreateResponse(UUID.randomUUID().toString(), "2-efaa5db9409b2d4300535c70aaf6a66b");
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
    public AdvisoryUpdateResponse changeComment(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory a comment of."
            ) String advisoryId,
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the comment to change."
            )  long commentId,
            @RequestParam @Parameter(description = "Optimistic locking revision.") String revision,
            @RequestBody String newCommentText
    ) {

        // only for debugging, remove when implemented
        LOG.info("changeComment {} {} {} {}", sanitize(advisoryId), sanitize(commentId), sanitize(revision)
                , sanitize(newCommentText));
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
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
    public AdvisoryUpdateResponse changeAnswer(
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the advisory the answered comment belongs to."
            ) String advisoryId,
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the comment to change an answer of."
            )  long commentId,
            @PathVariable
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The ID of the answer to change."
            )  long answerId,
            @RequestParam
            @Parameter(description = "Optimistic locking revision of the answer.") String revision,
            @RequestBody String newAnswerText
    ) {

        // only for debugging, remove when implemented
        LOG.info("changeAnswer {} {} {} {} {}", sanitize(advisoryId), sanitize(commentId)
                , sanitize(answerId), sanitize(revision), sanitize(newAnswerText));
        return new AdvisoryUpdateResponse("2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Sanitize request parameter
     */
    private String sanitize(Object value) {
        return (value != null) ? value.toString().replaceAll("[\r\n]", "") : "";
    }
}
