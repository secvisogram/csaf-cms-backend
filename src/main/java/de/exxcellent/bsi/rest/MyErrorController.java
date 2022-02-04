package de.exxcellent.bsi.rest;

import de.exxcellent.bsi.SecvisogramApplication;
import io.swagger.annotations.Api;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RestController
@RequestMapping("/error")
@Api(value = "Advisory API", description = "API for for Creating, Retrieving, Updating and Deleting of CSAF Dokuments, " +
        "including their Versions, Audit Trails, Comments and Workflow States.")
public class MyErrorController implements ErrorController {

    public String handleError() {
        //do something like logging
        return "error";
    }

    @GetMapping(value="about")
    public String aboutError() {
        //do something like logging
        return "error";
    }

}