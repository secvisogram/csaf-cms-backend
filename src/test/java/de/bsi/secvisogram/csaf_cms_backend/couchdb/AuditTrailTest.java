package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelRoot;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import org.junit.jupiter.api.Test;

public class AuditTrailTest {

    @Test
    public void calculateJsonDiff_replaceAValue()  {

        ObjectMapper mapper = new ObjectMapper();

        JsonNode source = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("BBB"));
        JsonNode target = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("AAA"));

        JsonNode patch = AdvisoryWrapper.calculateJsonDiff(source, target);
        System.out.println(patch);
        assertThat(patch.get(0).get("op").asText(), equalTo("replace"));
        assertThat(patch.get(0).get("path").asText(), equalTo("/secondString"));
        assertThat(patch.get(0).get("value").asText(), equalTo("AAA"));

    }

    @Test
    public void calculateJsonDiff_applyJsonPatch()  {

        ObjectMapper mapper = new ObjectMapper();

        JsonNode source = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("BBB"));
        JsonNode target = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("AAA"));

        JsonNode patch = AdvisoryWrapper.calculateJsonDiff(source, target);
        JsonNode patchedSource = AdvisoryWrapper.applyJsonPatchToNode(patch, source);
        assertThat(patchedSource.get("secondString").asText(), equalTo("AAA"));
    }
}
