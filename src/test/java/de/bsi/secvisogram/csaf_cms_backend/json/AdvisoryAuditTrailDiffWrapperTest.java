package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonCategoryTitleId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class AdvisoryAuditTrailDiffWrapperTest {

    @Test
    public void createNewFromCsafTest() throws IOException {

        var oldWrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonCategoryTitleId("Category1", "OldTitle", "Id1"), "John");
        var newWrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonCategoryTitleId("Category1", "NewTitle", "Id2"), "John");

        AdvisoryAuditTrailDiffWrapper wrapper = AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(oldWrapper, newWrapper);
        assertThat(wrapper.getDiffPatch().at("/0/op").asText(), equalTo("replace"));
        assertThat(wrapper.getDiffPatch().at("/0/path").asText(), equalTo("/csaf/document/title"));
        assertThat(wrapper.getDiffPatch().at("/0/value").asText(), equalTo("NewTitle"));
        assertThat(wrapper.getType(), equalTo(ObjectType.AuditTrailDocument.name()));
        assertThat(wrapper.getDocVersion(), equalTo("0.0.1"));
        assertThat(wrapper.getOldDocVersion(), equalTo("0.0.1"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void createNewFromCsafTest_emptyAdvisory() throws IOException {

        var oldWrapper = AdvisoryWrapper.createInitialEmptyAdvisoryForUser("John");
        var newWrapper = AdvisoryWrapper.createNewFromCsaf(csafJsonCategoryTitleId("Category1", "NewTitle", "ID01"), "John");

        AdvisoryAuditTrailDiffWrapper wrapper = AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(oldWrapper, newWrapper);
        assertThat(wrapper.getDiffPatch().at("/0/op").asText(), equalTo("add"));
        assertThat(wrapper.getDiffPatch().at("/0/path").asText(), equalTo("/csaf/document/category"));
        assertThat(wrapper.getDiffPatch().at("/0/value").asText(), equalTo("Category1"));
        assertThat(wrapper.getType(), equalTo(ObjectType.AuditTrailDocument.name()));
        assertThat(wrapper.getDocVersion(), equalTo("0.0.1"));
        assertThat(wrapper.getOldDocVersion(), is(""));
    }



}
