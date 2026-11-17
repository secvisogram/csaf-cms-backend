package de.bsi.secvisogram.csaf_cms_backend.rest;

import de.bsi.secvisogram.csaf_cms_backend.SecvisogramApplication;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.rest.error.ApiError;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryPageResponse;
import de.bsi.secvisogram.csaf_cms_backend.service.AdvisoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Version 2 of the advisory list: paged, sorted, and filtered only on fields stored as columns.
 */
@RestController
@RequestMapping(SecvisogramApplication.BASE_ROUTE_V2 + "advisories")
@Tag(name = "Advisory", description = "API for Creating, Retrieving, Updating and Deleting of CSAF documents,"
        + " including their Versions, Comments and Workflow States.")
public class AdvisoryV2Controller {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryV2Controller.class);

    @Autowired
    private AdvisoryService advisoryService;

    /**
     * One page of the advisories the user may see.
     *
     * @param expression optional filter expression as JSON
     * @param page       zero-based page number
     * @param size       entries per page
     * @param sort       sort criteria
     * @return the page and the total number of matching advisories
     */
    @GetMapping("")
    @Operation(
            summary = "Get one page of the authorized advisories.",
            tags = {"Advisory"},
            description = "Like GET /api/v1/advisories, but paged and sorted, and filter and sort accept only fields"
                    + " stored as columns. Auditors also get the version snapshots of published advisories, in the"
                    + " same sorted list. For a snapshot, revision is null and lastUpdated is the time the snapshot"
                    + " was taken (v1 returns null there)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The page of advisories.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = AdvisoryPageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid filter expression, sort or page parameter.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access."),
        @ApiResponse(responseCode = "403", description = "The user has none of the roles that may list advisories.")
    })
    // sort is read from the request itself (see below), so it is documented here instead of on a method parameter
    @Parameter(name = "sort", in = ParameterIn.QUERY,
            array = @ArraySchema(schema = @Schema(type = "string", example = "title,asc")),
            description = """
                    Sort criteria, repeatable: field or field,asc or field,desc. Fields: workflowState, owner,
                    lastUpdated, trackingId, title, currentReleaseDate, trackingVersion (text order), trackingStatus,
                    category, tlp. Empty values sort last. Blank values are ignored. Default: lastUpdated,desc.
                    Text fields sort by the collation of the database (e.g. under C or POSIX upper case comes
                    before lower case).""")
    public ResponseEntity<?> listAdvisoryPage(
            @RequestParam(required = false)
            @Parameter(in = ParameterIn.QUERY, name = "expression",
                    description = """
                            The filter expression in JSON format, as for GET /api/v1/advisories, but only on these
                            selectors with valueType 'Text': ["owner"], ["workflowState"], ["lastUpdated"],
                            ["csaf","document","tracking","id"], ["csaf","document","title"],
                            ["csaf","document","tracking","current_release_date"],
                            ["csaf","document","tracking","version"], ["csaf","document","tracking","status"],
                            ["csaf","document","category"], ["csaf","document","distribution","tlp","label"],
                            ["csaf","document","tracking","aliases"], ["csaf","vulnerabilities","cve"],
                            ["csaf","vulnerabilities","ids","text"]. Date-times (lastUpdated,
                            current_release_date) are ISO-8601 with offset and compared as instants. On the array
                            fields (aliases, cve, ids text) an advisory matches if any element matches.""",
                    schema = @Schema(type = "string", format = "json"))
            String expression,
            @RequestParam(defaultValue = "0")
            @Parameter(description = "Zero-based page number.")
            int page,
            @RequestParam(defaultValue = "50")
            @Parameter(description = "Entries per page, 1 to " + AdvisoryService.MAX_PAGE_SIZE + ".")
            int size,
            @Parameter(hidden = true)
            HttpServletRequest request
    ) {
        LOG.debug("listAdvisoryPage");
        // the raw values: binding to a list would split a single "title,asc" at the comma
        String[] sortValues = request.getParameterValues("sort");
        List<String> sort = sortValues == null ? List.of()
                : Arrays.stream(sortValues).filter(value -> !value.isBlank()).toList();
        try {
            return ResponseEntity.ok(advisoryService.getAdvisoryPage(expression, page, size, sort));
        } catch (CsafException ex) {
            return ResponseEntity.status(ex.getRecommendedHttpState()).body(new ApiError(ex.getMessage()));
        }
    }
}
