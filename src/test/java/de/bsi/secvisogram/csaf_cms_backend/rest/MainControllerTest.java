package de.bsi.secvisogram.csaf_cms_backend.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MainController.class)
public class MainControllerTest {

    private static final String testVersion = "Test Version";

    @TestConfiguration
    public static class TestConfig {
        @Bean
        BuildProperties buildProperties() {
            Properties props = new Properties();
            props.setProperty("version", testVersion);
            return new BuildProperties(props);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    MainController mainController;

    @Test
    public void contextLoads() {
        Assertions.assertNotNull(mainController);
    }

    @Test
    void aboutTest() throws Exception {
        this.mockMvc.perform(get("/api/v1/about"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("{\"version\": \"%s\"}", testVersion)));

    }

}
