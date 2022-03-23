package de.exxcellent.bsi.rest;


import de.exxcellent.bsi.SecvisogramApplication;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * API for for Creating, Retrieving, Updating and Deleting of CSAF Dokuments,
 * including their Versions, Audit Trails, Comments and Workflow States.
 */
@RestController
@RequestMapping(SecvisogramApplication.BASE_ROUTE +"advisories")
@Tag(name = "Advisory", description = "API for for Creating, Retrieving, Updating and Deleting of CSAF Dokuments, including their Versions, Comments and Workflow States.")
public class AdvisoryController {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryController.class);


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


        return Arrays.asList(
           new AdvisoryInformationResponse(UUID.randomUUID().toString(), WorkflowState.Draft, "Example Company - 2019-YH3234"),
           new AdvisoryInformationResponse(UUID.randomUUID().toString(), WorkflowState.Approved, "RHBA-2019:0024"),
           new AdvisoryInformationResponse(UUID.randomUUID().toString(), WorkflowState.Review, "cisco-sa-20190513-secureboot")
        );
    }


    /**
     * Get Single Advisory
     * @param advisoryId Id of the CSAF-Dokumente, that should be read
     * @return dokument
     */
    @GetMapping("/{advisoryId}/")
    @Operation(summary = "Show advisory", tags = { "Advisory" })
    public AdvisoryResponse advisoryById(@PathVariable long advisoryId) {

        return null;
    }

    /**
     * Get list of all templates in the system
     * @return list of all templates
     */
    @GetMapping("/templates")
    @Operation(summary = "Get all authorized templates", tags = { "Advisory" })
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
    @Operation(summary = "Get template content", tags = { "Advisory" })
    public AdvisoryTemplateResponse templateById(@PathVariable long templateId) {

        return null;
    }

    /**
     * Export CSAF document
     * @param advisoryId id of the CSAF-document, that should be exported
     * @param format optional - format of the result
     * @return dokument
     */
    @GetMapping(value="/{advisoryId}/csaf",produces = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_HTML_VALUE
            , MediaType.TEXT_MARKDOWN_VALUE, MediaType.APPLICATION_PDF_VALUE})
    @Operation(summary = "Export advisory csaf in different formats, possible formats are: PDF, Markdown, HTML, JSON", tags = { "Advisory" })
    public String exportAdvisory(@PathVariable String advisoryId, @RequestParam(required = false) ExportFormat format) {

        return "";
    }

    /**
     * Create new CSAF-document
     * @param newCsafJson content of the new CSAF document
     */
    @PostMapping(name="/", consumes = "application/json")
    @Operation(summary = "Create a new Advisory in the system", tags = { "Advisory" })
    public AdvisoryCreateResponse createCsafDocument(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Advisory in CSAF JSON Format with comments.", required = true)
            @RequestBody String newCsafJson) {

        return new AdvisoryCreateResponse(UUID.randomUUID().toString(), "2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * change CSAF-document
     * @param advisoryId id of the CSAF document to change
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change advisory", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/")
    public String changeCsafDocument(@PathVariable long advisoryId,
           @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Advisory in CSAF JSON Format with comments.", required = true)
                                   @RequestBody String changedCsafJson) {
        return "2-efaa5db9409b2d4300535c70aaf6a66b";
    }


    /**
     * Delete  CSAF document
     * @param advisoryId advisoryId id of the CSAF document to delete
     */
    @Operation(summary = "Delete advisory. All older versions, comments and audit-trails are also deleted.", tags = { "Advisory" })
    @DeleteMapping("/{advisoryId}/")
    public void deleteAdvisoryWithId(@PathVariable long advisoryId) {

    }


    /**
     * Change workflow state of a CSAF document
     * @param advisoryId advisoryId id of the CSAF document to change
     * @param newState new workflow state of the CSAF document
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change workflow state of an advisory", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/workflowstate/")
    public String changeWorkflowState(@PathVariable long advisoryId, @RequestBody WorkflowState newState) {

        return "2-efaa5db9409b2d4300535c70aaf6a66b";
    }

    /**
     * Get a list of all comments and answers of an CSAF document
     * @param advisoryId  id of the CSAF document to add the answer
     * @return list of comments and their metadata
     */
    @Operation(summary = "Show comments and answers of an advisory", tags = { "Advisory" })
    @GetMapping("/{advisoryId}/comments/")
    public List<AdvisoryCommentResponse> comments(@PathVariable long advisoryId) {

        return Collections.emptyList();
    }

    /**
     * Add comment to an advisory
     * @param advisoryId id of the CSAF document to add the comment
     * @param commentText text content of the comment
     *
     */
    @Operation(summary = "Add comment to an advisory", tags = { "Advisory" })
    @PostMapping("/{advisoryId}/comments")
    public AdvisoryCreateResponse createComment(@PathVariable long advisoryId, @RequestBody AdvisoryCreateCommentRequest commentText) {

        return new AdvisoryCreateResponse(UUID.randomUUID().toString(), "2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Add answer to a comment of an advisory
     * @param advisoryId id of the CSAF document to add the answer
     * @param commentId id of the comment to add the answer
     * @param answerText new text content of the answer
     *
     */
    @Operation(summary = "Add answer to an advisory comment", tags = { "Advisory" })
    @PostMapping("/{advisoryId}/comments/{commentId}/answer")
    public AdvisoryCreateResponse createAnswer(@PathVariable long advisoryId, @PathVariable long commentId, @RequestBody String answerText) {

        return new AdvisoryCreateResponse(UUID.randomUUID().toString(), "2-efaa5db9409b2d4300535c70aaf6a66b");
    }

    /**
     * Change comment of an advisory
     * @param advisoryId id of the CSAF document to add the answer
     * @param commentId of the comment to change the answer
     * @param newCommentText new text content of the comment
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change comment of an advisory", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/comments/{commentId}")
    public String changeComment(@PathVariable long advisoryId, @PathVariable long commentId
            , @RequestBody String newCommentText) {

        return "2-efaa5db9409b2d4300535c70aaf6a66b";
    }

    /**
     * Change answer of a comment to an advisory
     * @param advisoryId id of the CSAF document to change the answer
     * @param commentId commentId of the comment
     * @param answerId id of the answer to change
     * @param newAnswerText new text content of the answer
     * @return new optimistic locking revision
     */
    @Operation(summary = "Change answer to an advisory comment", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/comments/{commentId}/answer/{answerId}")
    public String changeAnswer(@PathVariable long advisoryId, @PathVariable long commentId
                , @PathVariable long answerId,  @RequestBody String newAnswerText) {

        return "2-efaa5db9409b2d4300535c70aaf6a66b";
    }

}
