package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryJsonService.*;

import com.fasterxml.jackson.core.JsonProcessingException;
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

        InputStream csafStream = new ByteArrayInputStream(csafJson.getBytes(StandardCharsets.UTF_8));

        ObjectNode node = convertCsafToJson(csafStream, "Mustermann", WorkflowState.Draft);

        Assertions.assertEquals("Mustermann", node.at("/owner").asText());
        Assertions.assertEquals("Advisory", node.at("/type").asText());
        Assertions.assertEquals("Draft", node.at("/workflowState").asText());
        Assertions.assertEquals(csafJson.replaceAll("\\s+", ""), node.at("/csaf").toString());
        Assertions.assertEquals("CSAF_BASE", node.at("/csaf/document/category").asText());
    }

    @Test
    public void covertCouchDbCsafToAdvisoryTest() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(fullAdvisoryJson);
        String advisoryId = "anId";

        AdvisoryResponse response = covertCouchDbCsafToAdvisory(jsonNode, advisoryId);

        Assertions.assertEquals(advisoryId, response.getAdvisoryId());
        Assertions.assertEquals("Musterfrau", response.getOwner());
        Assertions.assertEquals(WorkflowState.Draft, response.getWorkflowState());
        Assertions.assertEquals(csafJson.replaceAll("\\s+", ""), response.getCsaf());

    }

    @Test
    public void changeWorkflowStateTest() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(fullAdvisoryJson);

        Assertions.assertEquals("Draft", jsonNode.at("/workflowState").asText());

        changeWorkflowState(jsonNode, WorkflowState.Approved);

        Assertions.assertEquals("Approved", jsonNode.at("/workflowState").asText());
    }

}
