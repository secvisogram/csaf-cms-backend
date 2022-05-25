package de.bsi.secvisogram.csaf_cms_backend.couchdb;

public class AuditTrailTest {

  /*  @Test
    public void calculateJsonDiff_replaceAValue()  {

        ObjectMapper mapper = new ObjectMapper();

        JsonNode source = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("BBB"));
        JsonNode target = mapper.valueToTree(new TestModelRoot().setFirstString("xxx").setSecondString("AAA"));

        JsonNode patch = new AdvisoryJsonService().calculateJsonDiff(source, target);
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

        JsonNode patch = new AdvisoryJsonService().calculateJsonDiff(source, target);
        JsonNode patchedSource = new AdvisoryJsonService().applyJsonPatch(patch, source);
        assertThat(patchedSource.get("secondString").asText(), equalTo("AAA"));
    }*/
}
