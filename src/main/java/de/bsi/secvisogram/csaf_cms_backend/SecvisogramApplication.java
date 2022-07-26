package de.bsi.secvisogram.csaf_cms_backend;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication()
@EnableConfigurationProperties(CsafConfiguration.class)
public class SecvisogramApplication {

    public static final String BASE_ROUTE = "/api/v1/";

    public static void main(String[] args) {
        SpringApplication.run(SecvisogramApplication.class, args);
    }

}
