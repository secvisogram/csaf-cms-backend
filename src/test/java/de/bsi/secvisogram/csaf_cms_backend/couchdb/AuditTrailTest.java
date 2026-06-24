package de.bsi.secvisogram.csaf_cms_backend.couchdb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import de.bsi.secvisogram.csaf_cms_backend.fixture.TestModelRoot;
import de.bsi.secvisogram.csaf_cms_backend.json.AdvisoryWrapper;
import org.junit.jupiter.api.Test;

public class AuditTrailTest {

    @Test
    public void calculateJsonDiff_replaceAValue()  {

        ObjectMapper mapper = new JsonMapper();

        JsonNode source = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("BBB"));
        JsonNode target = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("AAA"));

        JsonNode patch = AdvisoryWrapper.calculateJsonDiff(source, target);
        assertThat(patch.get(0).get("op").asString(), equalTo("replace"));
        assertThat(patch.get(0).get("path").asString(), equalTo("/secondString"));
        assertThat(patch.get(0).get("value").asString(), equalTo("AAA"));

    }

    @Test
    public void calculateJsonDiff_applyJsonPatch()  {

        ObjectMapper mapper = new JsonMapper();

        JsonNode source = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("BBB"));
        JsonNode target = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("AAA"));

        JsonNode patch = AdvisoryWrapper.calculateJsonDiff(source, target);
        JsonNode patchedSource = AdvisoryWrapper.applyJsonPatchToNode(patch, source);
        assertThat(patchedSource.get("secondString").asString(), equalTo("AAA"));
    }
}
