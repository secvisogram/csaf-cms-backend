package de.bsi.secvisogram.csaf_cms_backend.json;

import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafJsonCategoryTitleId;
import static de.bsi.secvisogram.csaf_cms_backend.fixture.CsafDocumentJsonCreator.csafToRequest;
import static de.bsi.secvisogram.csaf_cms_backend.json.VersioningType.Semantic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class AdvisoryAuditTrailDiffWrapperTest {

    @Test
    public void createNewFromCsafTest() throws IOException, CsafException {

        var oldWrapper = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonCategoryTitleId("Category1", "OldTitle", "Id1")), "John", Semantic.name());
        var newWrapper = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonCategoryTitleId("Category1", "NewTitle", "Id2")), "John", Semantic.name());

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
    public void createNewFromCsafTest_emptyAdvisory() throws IOException, CsafException {

        var oldWrapper = AdvisoryWrapper.createInitialEmptyAdvisoryForUser("John");
        var newWrapper = AdvisoryWrapper.createNewFromCsaf(csafToRequest(csafJsonCategoryTitleId("Category1", "NewTitle", "ID01")), "John", Semantic.name());

        AdvisoryAuditTrailDiffWrapper wrapper = AdvisoryAuditTrailDiffWrapper.createNewFromAdvisories(oldWrapper, newWrapper);
        assertThat(wrapper.getDiffPatch().at("/0/op").asText(), equalTo("add"));
        assertThat(wrapper.getDiffPatch().at("/0/path").asText(), equalTo("/csaf/document/category"));
        assertThat(wrapper.getDiffPatch().at("/0/value").asText(), equalTo("Category1"));
        assertThat(wrapper.getType(), equalTo(ObjectType.AuditTrailDocument.name()));
        assertThat(wrapper.getDocVersion(), equalTo("0.0.1"));
        assertThat(wrapper.getOldDocVersion(), is(""));
    }



}
