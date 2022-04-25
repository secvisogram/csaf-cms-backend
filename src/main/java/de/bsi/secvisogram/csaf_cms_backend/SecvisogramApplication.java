package de.bsi.secvisogram.csaf_cms_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication()
public class SecvisogramApplication {

	public static final String BASE_ROUTE = "/api/2.0/";

	public static void main(String[] args) {
		SpringApplication.run(SecvisogramApplication.class, args);
	}

}
