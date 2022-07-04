package de.bsi.secvisogram.csaf_cms_backend.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class AdvisoryAuditTrailDiffWrapperTest {


    @Test
    public void createNewFromCsafTest() throws IOException {

        var oldWrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitleVersion("OldTitle", "0.0.1"), "John");
        var newWrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitleVersion("NewTitle", "0.0.2"), "John");

        AdvisoryAuditTrailDiffWrapper wrapper = AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(oldWrapper, newWrapper);
        assertThat(wrapper.getDiffPatch().at("/0/op").asText(), equalTo("replace"));
        assertThat(wrapper.getDiffPatch().at("/0/path").asText(), equalTo("/csaf/document/title"));
        assertThat(wrapper.getDiffPatch().at("/0/value").asText(), equalTo("NewTitle"));
        assertThat(wrapper.getType(), equalTo(ObjectType.AuditTrailDocument.name()));
        assertThat(wrapper.getDocVersion(), equalTo("0.0.2"));
        assertThat(wrapper.getOldDocVersion(), equalTo("0.0.1"));
    }

    @Test
    public void createNewFromCsafTest_noVersion() throws IOException {

        var oldWrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitle("OldTitle"), "John");
        var newWrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonTitleVersion("NewTitle", "0.0.2"), "John");

        AdvisoryAuditTrailDiffWrapper wrapper = AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(oldWrapper, newWrapper);
        assertThat(wrapper.getDiffPatch().at("/0/op").asText(), equalTo("replace"));
        assertThat(wrapper.getDiffPatch().at("/0/path").asText(), equalTo("/csaf/document/title"));
        assertThat(wrapper.getDiffPatch().at("/0/value").asText(), equalTo("NewTitle"));
        assertThat(wrapper.getType(), equalTo(ObjectType.AuditTrailDocument.name()));
        assertThat(wrapper.getDocVersion(), equalTo("0.0.2"));
        assertThat(wrapper.getOldDocVersion(), is(""));
    }

    private String csafJsonTitleVersion(String documentTitle, String documentTrackingVer) {

        return """
                { "document": {
                      "category": "Category1",
                      "title": "%s",
                      "tracking": {
                        "version": "%s"
                      }
                   }
                }""".formatted(documentTitle, documentTrackingVer);
    }

    private String csafJsonTitle(String documentTitle) {

        return """
                { "document": {
                      "category": "Category1",
                      "title": "%s"
                   }
                }""".formatted(documentTitle);
    }

}
