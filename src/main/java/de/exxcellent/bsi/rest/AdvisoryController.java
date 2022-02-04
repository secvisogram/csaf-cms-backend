package de.exxcellent.bsi.rest;


import de.exxcellent.bsi.SecvisogramApplication;
import de.exxcellent.bsi.model.ExportFormat;
import de.exxcellent.bsi.model.WorkflowState;
import de.exxcellent.bsi.rest.response.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping(SecvisogramApplication.BASE_ROUTE +"advisories")
@Api(value = "Advisory API", description = "API for for Creating, Retrieving, Updating and Deleting of CSAF Dokuments, " +
        "including their Versions, Audit Trails, Comments and Workflow States.")
public class AdvisoryController {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryController.class);


    /**
     * Auflisten/Ansehen von CSAF-Dokumente
     * Ansehen des Status von CSAF-Dokument
     * @return
     */
    @GetMapping("/")
    @ApiOperation(value="Get all authorized advisories")
    public List<AdvisoryInformationResponse> findAllAdvisories() {

        LOG.info("findAll");
        return Collections.emptyList();
    }


    /**
     * Ansehen von CSAF-Dokumente
     * Export von CSAF-Dokumenten
     * @param advisoryId Id des CSAF-Dokumente, dass geladen werden soll
     * @format optional - format of the result
     * @return dokument
     */
    @GetMapping("/{advisoryId}/")
    public AdvisoryResponse advisoryById(@PathVariable long advisoryId, @RequestParam(required = false) ExportFormat format) {

        return new AdvisoryResponse();
    }


    /**
     * Erstellen von CSAF-Dokumenten
     * @param newCsafJson
     */
    @PostMapping("/")
    public void createCsafDocument(@RequestBody String newCsafJson) {

    }

    /**
     * Aendern von CSAF-Dokumenten
     * @param changedCsafJson
     */
    @PatchMapping("/{advisoryId}/")
    public void changeCsafDocument(@PathVariable long advisoryId, @RequestBody String changedCsafJson) {

    }


    /**
     * Loeschen von CSAF-Dokumenten
     * @param advisoryId Id des Dokument, dass geloescht werden soll
     */
    @DeleteMapping("/{advisoryId}/")
    public void deleteAdvisoryWithId(@PathVariable long advisoryId) {

    }

/*
    */
/**
     * Auflisten/Ansehen des Audits-Trails von CSAF-Dokumente
     * @param advisoryId
     * @return
     *//*

    @GetMapping("/{advisoryId}/auditTrails")
    public List<AuditTrailEntryResponse> auditTrail(@PathVariable long advisoryId) {

        return Collections.emptyList();
    }
*/

    /**
     * Auflisten der Versionen von CSAF-Dokumente
     * @param advisoryId
     * @return
     */
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
    @GetMapping("/{advisoryId}/versions/{version}/")
    public AdvisoryResponse version(@PathVariable long advisoryId, @PathVariable String version) {

        return new AdvisoryResponse();
    }

    /**
     * Auflisten/Ansehen der Worklow Status Aenderungen von CSAF-Dokument
     * @param advisoryId
     * @return
     */
    @GetMapping("/{advisoryId}/states/")
    public List<WorkflowChangeResponse> stateChanges(@PathVariable long advisoryId) {

        return Collections.emptyList();
    }

    /**
     * Workflow Status Aenderung von CSAF-Dokumenten
     * @param advisoryId
     * @param newState
     */
    @PatchMapping("/{advisoryId}/state/")
    public void changeWorkflowState(@PathVariable long advisoryId, @RequestBody WorkflowState newState) {

    }

    /**
     * Auflisten/Ansehen der Kommentare und Antworten von CSAF-Dokument
     * @param advisoryId
     * @return
     */
    @GetMapping("/{advisoryId}/comments/")
    public List<CommentResponse> comments(@PathVariable long advisoryId) {

        return Collections.emptyList();
    }

    /**
     * Kommentieren von CSAF-Dokumenten - Kommentar erzeugen
     * @param advisoryId
     * @param commentText
     */
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
    @PatchMapping("/{advisoryId}/comments/{commentId}/answer/{answerId}")
    public List<CommentResponse> changeAnswer(@PathVariable long advisoryId, @PathVariable long commentId
                , @PathVariable long answerId,  @RequestBody String newAnswerText) {

        return Collections.emptyList();
    }



}
