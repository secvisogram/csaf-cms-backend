package de.exxcellent.bsi.rest;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("2.0")
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    @RequestMapping(value="about",method= RequestMethod.GET)
    public String about() {
        LOG.info("about");
        return "BSI Secvisogram Backend";
    }

}
