package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AdvisoryResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;


public class AdvisoryJsonServiceTest {

    String csafJson = "{" +
                      "    \"document\": {" +
                      "        \"category\": \"CSAF_BASE\"" +
                      "    }" +
                      "}";

    String fullAdvisoryJson = String.format("{" +
                                            "    \"owner\": \"Musterfrau\"," +
                                            "    \"type\": \"Advisory\"," +
                                            "    \"workflowState\": \"Draft\"," +
                                            "    \"csaf\": %s," +
                                            "    \"_rev\": \"revision\"," +
                                            "    \"_id\": \"id\"" +
                                            "}", csafJson);

    @Test
    public void convertCsafToJsonTest() throws IOException {
        AdvisoryJsonService jsonService = new AdvisoryJsonService();

        InputStream csafStream = new ByteArrayInputStream(csafJson.getBytes(StandardCharsets.UTF_8));

        ObjectNode node = jsonService.convertCsafToJson(csafStream, "Mustermann", WorkflowState.Draft);

        Assertions.assertEquals("Mustermann", node.at("/owner").asText());
        Assertions.assertEquals("Advisory", node.at("/type").asText());
        Assertions.assertEquals("Draft", node.at("/workflowState").asText());
        Assertions.assertEquals(csafJson.replaceAll("\\s+", ""), node.at("/csaf").toString());
        Assertions.assertEquals("CSAF_BASE", node.at("/csaf/document/category").toString());
    }

    @Test
    public void covertCouchDbCsafToAdvisoryTest() throws IOException {
        AdvisoryJsonService jsonService = new AdvisoryJsonService();


        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(fullAdvisoryJson);
        String advisoryId = "anId";

        AdvisoryResponse response = jsonService.covertCouchDbCsafToAdvisory(jsonNode, advisoryId);

        Assertions.assertEquals(advisoryId, response.getAdvisoryId());
        Assertions.assertEquals("Musterfrau", response.getOwner());
        Assertions.assertEquals(WorkflowState.Draft, response.getWorkflowState());
        Assertions.assertEquals(csafJson.replaceAll("\\s+", ""), response.getCsaf());

    }

}
