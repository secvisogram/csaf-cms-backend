package de.exxcellent.bsi.json;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.exxcellent.bsi.model.WorkflowState;
import de.exxcellent.bsi.rest.response.AdvisoryResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class AdvisoryJsonService {

    public static final String WORKFLOW_STATE_FIELD = "workflowState";
    public static final String OWNER_FIELD = "owner";
    public static final String TYPE_FIELD = "type";
    public static final String CSAF_FIELD = "csaf";
    public static final String COUCHDB_REVISON_FIELD = "_rev";
    public static final String COUCHDB_ID_FIELD = "_id";

    public enum ObjectType {
        Advisory
    }

    private final ObjectMapper jacksonMapper = new ObjectMapper();

    public ObjectNode convertCsafToJson(InputStream csafJsonStream, String userName, WorkflowState state) throws IOException {

        JsonNode csafRootNode = jacksonMapper.readValue(csafJsonStream, JsonNode.class);

        ObjectNode rootNode = jacksonMapper.createObjectNode();
        final UUID uuid= UUID.randomUUID();
        rootNode.put(WORKFLOW_STATE_FIELD, state.name());
        rootNode.put(OWNER_FIELD, userName);
        rootNode.put(TYPE_FIELD, ObjectType.Advisory.name());
        rootNode.set(CSAF_FIELD, csafRootNode);

        return rootNode;
    }

    public AdvisoryResponse covertCoudbCsafToAdvisory(JsonNode document, String advisoryId) throws IOException {

        JsonNode workflowState = document.get(WORKFLOW_STATE_FIELD);
        final AdvisoryResponse response =  new AdvisoryResponse(advisoryId, WorkflowState.valueOf(workflowState.asText())
                , "");
        response.setOwner(document.get(OWNER_FIELD).asText());
        response.setRevision(document.get(COUCHDB_REVISON_FIELD).asText());
        JsonNode csafNode = document.get(CSAF_FIELD);
        ObjectWriter writer = this.jacksonMapper.writer();
        String updateString = writer.writeValueAsString(csafNode);
        response.setCsafJsonWithComments(updateString);

        return response;
    }
}
