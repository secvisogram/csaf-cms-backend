package de.exxcellent.bsi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

@SpringBootApplication()
public class SecvisogramApplication {

	public static final String BASE_ROUTE = "/api/2.0/";

	public static void main(String[] args) {
		SpringApplication.run(SecvisogramApplication.class, args);
	}

}
