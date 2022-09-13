package de.bsi.secvisogram.csaf_cms_backend.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.bsi.secvisogram.csaf_cms_backend.service.PatchType;
import org.junit.jupiter.api.Test;

public class IntegerVersioningTest {

    @Test
    public void getInitialVersionTest()    {

        assertThat(IntegerVersioning.getDefault().getInitialVersion(),  is("0"));
    }

    @Test
    public void getVersioningTypeTest()    {

        assertThat(IntegerVersioning.getDefault().getVersioningType(),  is(VersioningType.Integer));
    }

    @Test
    public void getNextVersionTest() {

        assertThat(IntegerVersioning.getDefault().getNextVersion(PatchType.PATCH, "0", "0"), is("0"));
        assertThat(IntegerVersioning.getDefault().getNextVersion(PatchType.PATCH, "1", "0"), is("1"));
    }

    @Test
    public void getNextApprovedVersionTest() {
        assertThat(IntegerVersioning.getDefault().getNextApprovedVersion("0"), is("1"));
        assertThat(IntegerVersioning.getDefault().getNextApprovedVersion("1"), is("1"));
        assertThat(IntegerVersioning.getDefault().getNextApprovedVersion("2"), is("2"));
    }

    @Test
    public void removeVersionSuffixTest() {
        assertThat(IntegerVersioning.getDefault().removeVersionSuffix("1"), is("1"));
        assertThat(IntegerVersioning.getDefault().removeVersionSuffix("2"), is("2"));
    }

    @Test
    public void getNewDocumentVersionTest() {
        assertThat(IntegerVersioning.getDefault().getNewDocumentVersion("1"), is("2"));
        assertThat(IntegerVersioning.getDefault().getNewDocumentVersion("2"), is("3"));
        assertThat(IntegerVersioning.getDefault().getNewDocumentVersion("NoNumber"), is("1"));
    }

    @Test
    public void getNextDraftVersionTest() {
        assertThat(IntegerVersioning.getDefault().getNextDraftVersion("1"), is("1"));
        assertThat(IntegerVersioning.getDefault().getNextDraftVersion("2"), is("2"));
    }
}
