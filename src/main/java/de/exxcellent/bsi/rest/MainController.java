package de.exxcellent.bsi.rest;


import de.exxcellent.bsi.model.Csaf;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/csaf")
@Tag(name = "User API", description = "User Management API")
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    @GetMapping(value="about")
    public String about() {
        LOG.info("about");
        return "BSI Secvisogram Backend";
    }

    @GetMapping("/{id}")
    public Csaf findById(@PathVariable long id) {
        return new Csaf();
    }
}
