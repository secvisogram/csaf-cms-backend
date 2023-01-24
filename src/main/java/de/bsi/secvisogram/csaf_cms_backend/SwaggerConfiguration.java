package de.bsi.secvisogram.csaf_cms_backend;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Autowired
    private BuildProperties buildProperties;

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Secvisogram 2.0 CSAF-Backend API")
                        .description("CSAF Content Management Systems: Secvisogram 2.0")
                        .version(buildProperties.getVersion())
                        .license(new License().name("MIT").url("https://mit-license.org/")))
                .externalDocs(new ExternalDocumentation()
                        .description("Common Security Advisory Framework Version 2.0")
                        .url("https://docs.oasis-open.org/csaf/csaf/v2.0/csaf-v2.0.html"));
    }

}
