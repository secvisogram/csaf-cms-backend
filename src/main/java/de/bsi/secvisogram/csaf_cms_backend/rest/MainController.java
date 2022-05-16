package de.bsi.secvisogram.csaf_cms_backend.rest;

import de.bsi.secvisogram.csaf_cms_backend.SecvisogramApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(SecvisogramApplication.BASE_ROUTE)
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    @GetMapping(value = "about")
    public String about() {
        LOG.info("about");
        return "BSI Secvisogram Backend";
    }

}
