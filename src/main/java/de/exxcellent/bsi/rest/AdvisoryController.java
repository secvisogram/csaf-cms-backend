package de.exxcellent.bsi.rest;


import de.exxcellent.bsi.SecvisogramApplication;
import de.exxcellent.bsi.model.ExportFormat;
import de.exxcellent.bsi.model.WorkflowState;
import de.exxcellent.bsi.model.filter.FilterExpression;
import de.exxcellent.bsi.rest.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * API for for Creating, Retrieving, Updating and Deleting of CSAF Dokuments,
 * including their Versions, Audit Trails, Comments and Workflow States.
 */
@RestController
@RequestMapping(SecvisogramApplication.BASE_ROUTE +"advisories")
@Tag(name = "Advisory", description = "API for for Creating, Retrieving, Updating and Deleting of CSAF Dokuments, including their Versions, Audit Trails, Comments and Workflow States.")
public class AdvisoryController {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryController.class);


    /**
     * Auflisten/Ansehen von CSAF-Dokumente
     * Ansehen des Status von CSAF-Dokument
     * @return
     */
    @GetMapping("/")
    @Operation(summary = "Get all authorized advisories", tags = { "Advisory" })
    public List<AdvisoryInformationResponse> findAdvisories(@RequestParam(required = false)
                        @Parameter(in = ParameterIn.QUERY, name = "expression",
                                description = "the filter expression in JSON format",
                                schema = @Schema(
                                        type = "string",
                                        format = "json",
                                        description = "filter expression")) String expression) {

        LOG.info("findAll");
        return Collections.emptyList();
    }


    /**
     * Ansehen von CSAF-Dokumente
     * @param advisoryId Id des CSAF-Dokumente, dass geladen werden soll
     * @return dokument
     */
    @GetMapping("/{advisoryId}/")
    @Operation(summary = "show advisory", tags = { "Advisory" })
    public AdvisoryResponse advisoryById(@PathVariable long advisoryId) {

        return new AdvisoryResponse(advisoryId, WorkflowState.Draft, "");
    }

    /**
     * Export von CSAF-Dokumenten
     * @param advisoryId Id des CSAF-Dokumente, dass geladen werden soll
     * @param format optional - format of the result
     * @return dokument
     */
    @GetMapping(value="/{advisoryId}/csaf",produces = {MediaType.APPLICATION_JSON_VALUE,MediaType.TEXT_HTML_VALUE
            , MediaType.TEXT_MARKDOWN_VALUE, MediaType.APPLICATION_PDF_VALUE})
    @Operation(summary = "export advisory csaf in different formats", tags = { "Advisory" })
    public String advisoryById(@PathVariable long advisoryId, @RequestParam(required = false) ExportFormat format) {

        return "";
    }


    /**
     * Erstellen von CSAF-Dokumenten
     * @param newCsafJson
     */
    @PostMapping("/")
    @Operation(summary = "Create advisory", tags = { "Advisory" })
    public void createCsafDocument(@RequestBody String newCsafJson) {

    }

    /**
     * Aendern von CSAF-Dokumenten
     * @param changedCsafJson
     */
    @Operation(summary = "Change advisory", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/")
    public void changeCsafDocument(@PathVariable long advisoryId, @RequestBody String changedCsafJson) {

    }


    /**
     * Loeschen von CSAF-Dokumenten
     * @param advisoryId Id des Dokument, dass geloescht werden soll
     */
    @Operation(summary = "Delete advisory", tags = { "Advisory" })
    @DeleteMapping("/{advisoryId}/")
    public void deleteAdvisoryWithId(@PathVariable long advisoryId) {

    }

    /**
     * Auflisten/Ansehen des Audits-Trails von CSAF-Dokumente
     * @param advisoryId
     * @return
     */
    @GetMapping("/{advisoryId}/auditTrails")
    @Operation(summary = "Show audit trail of an advisory", tags = { "Advisory" }
            ,responses = {
                @ApiResponse(responseCode = "200", description = "Audit Trail Response ", content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(type = "object", oneOf = {CommentChangeResponse.class
                                    , DocumentChangeResponse.class, WorkflowChangeResponse.class})
                    )
            ))})
    public List<AuditTrailEntryResponse> auditTrail(@PathVariable long advisoryId) {

        return Collections.emptyList();
    }

    /**
     * Auflisten der Versionen von CSAF-Dokumente
     * @param advisoryId
     * @return
     */
    @Operation(summary = "Show versions of an advisory", tags = { "Advisory" })
    @GetMapping("/{advisoryId}/versions/")
    public List<DocumentChangeResponse> versions(@PathVariable long advisoryId) {

        return Collections.emptyList();
    }

    /**
     * Ansehen einer Version eines CSAF Dokumente
     * @param advisoryId
     * @param version
     * @return
     */
    @Operation(summary = "Show all version of an advisory", tags = { "Advisory" })
    @GetMapping("/{advisoryId}/versions/{version}/")
    public AdvisoryResponse version(@PathVariable long advisoryId, @PathVariable String version) {

        return new AdvisoryResponse(advisoryId, WorkflowState.Draft, "");
    }

    /**
     * Auflisten/Ansehen der Worklow Status Aenderungen von CSAF-Dokument
     * @param advisoryId
     * @return
     */
    @Operation(summary = "Show workflow state of an advisory", tags = { "Advisory" })
    @GetMapping("/{advisoryId}/states/")
    public List<WorkflowChangeResponse> stateChanges(@PathVariable long advisoryId) {

        return Collections.emptyList();
    }

    /**
     * Workflow Status Aenderung von CSAF-Dokumenten
     * @param advisoryId
     * @param newState
     */
    @Operation(summary = "Change workflow state of an advisory", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/state/")
    public void changeWorkflowState(@PathVariable long advisoryId, @RequestBody WorkflowState newState) {

    }

    /**
     * Auflisten/Ansehen der Kommentare und Antworten von CSAF-Dokument
     * @param advisoryId
     * @return
     */
    @Operation(summary = "Show comments an answers of an advisory", tags = { "Advisory" })
    @GetMapping("/{advisoryId}/comments/")
    public List<CommentResponse> comments(@PathVariable long advisoryId) {

        return Collections.emptyList();
    }

    /**
     * Kommentieren von CSAF-Dokumenten - Kommentar erzeugen
     * @param advisoryId
     * @param commentText
     */
    @Operation(summary = "Add comment to an advisory", tags = { "Advisory" })
    @PostMapping("/{advisoryId}/comments")
    public void createComment(@PathVariable long advisoryId, @RequestBody String commentText) {

    }

    /**
     * Kommentieren von CSAF-Dokumenten - Antwort anlegen
     * @param advisoryId
     * @param commentId
     * @param answerText
     * @return
     */
    @Operation(summary = "Add answer to an advisory comment", tags = { "Advisory" })
    @PostMapping("/{advisoryId}/comments/{commentId}/answer")
    public void createAnswer(@PathVariable long advisoryId, @PathVariable long commentId, @RequestBody String answerText) {

    }

    /**
     * Kommentieren von CSAF-Dokumenten - Kommentar aendern
     * @param advisoryId
     * @param commentId
     * @param newCommentText
     * @return
     */
    @Operation(summary = "Change comment to an advisory", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/comments/{commentId}")
    public List<CommentResponse> changeComments(@PathVariable long advisoryId, @PathVariable long commentId
            , @RequestBody String newCommentText) {

        return Collections.emptyList();
    }

    /**
     * Kommentieren von CSAF-Dokumenten - Antwort aendern
     * @param advisoryId
     * @param commentId
     * @param answerId
     * @param newAnswerText
     * @return
     */
    @Operation(summary = "Change answer to an advisory comment", tags = { "Advisory" })
    @PatchMapping("/{advisoryId}/comments/{commentId}/answer/{answerId}")
    public List<CommentResponse> changeAnswer(@PathVariable long advisoryId, @PathVariable long commentId
                , @PathVariable long answerId,  @RequestBody String newAnswerText) {

        return Collections.emptyList();
    }



}
