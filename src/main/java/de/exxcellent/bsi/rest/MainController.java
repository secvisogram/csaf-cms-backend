package de.exxcellent.bsi.rest;


import de.exxcellent.bsi.SecvisogramApplication;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(SecvisogramApplication.BASE_ROUTE)
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    @GetMapping(value="about")
    public String about() {
        LOG.info("about");
        return "BSI Secvisogram Backend";
    }

}
