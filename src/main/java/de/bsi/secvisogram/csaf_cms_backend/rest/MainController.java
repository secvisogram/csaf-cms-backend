package de.bsi.secvisogram.csaf_cms_backend.rest;

import de.bsi.secvisogram.csaf_cms_backend.SecvisogramApplication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(SecvisogramApplication.BASE_ROUTE)
@Tag(
        name = "Main",
        description = "API for CSAF independent services"
)
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private BuildProperties buildProperties;

    @GetMapping(value = "about")
    @Operation(summary = "Current version", tags = {"Main"},
            description = "Get the current version of the backend service.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Version String as JSON", required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(title = "JSON", description = "Version string as JSON."),
                            examples = {@ExampleObject(
                                    name = "Version string",
                                    value = "{\"version\":\"1.0.0\"}"
                            )}
                    )
            ))
    public Map<String, String> about() {
        LOG.info("about");

        return Map.of("version", buildProperties.getVersion());
    }

}
