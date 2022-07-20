package de.bsi.secvisogram.csaf_cms_backend.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.bsi.secvisogram.csaf_cms_backend.service.PatchType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;


public class SemanticVersioningTest {


    @Test
    public void getInitialVersionTest()    {

        assertThat(SemanticVersioning.getDefault().getInitialVersion(),  is("0.0.1"));
    }

    @Test
    public void getVersioningTypeTest()    {

        assertThat(SemanticVersioning.getDefault().getVersioningType(),  is(VersioningType.Semantic));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void getNextVersionTest() {

        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.PATCH, "0.0.1", 0), is("0.0.2"));
        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.MINOR, "0.0.1", 0), is("0.0.2"));
        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.MAJOR, "0.0.1", 0), is("0.1.0"));

        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.MAJOR, "1.0.0-1.0", 0), is("1.0.0-1.1"));
        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.MAJOR, "1.0.0-1.4", 0), is("1.0.0-1.5"));

        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.MAJOR, "1.0.1-1.0", 1), is("2.0.0-1.1"));
        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.MAJOR, "2.0.0-1.0", 1), is("2.0.0-1.1"));

        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.MINOR, "1.0.1-1.2", 1), is("1.1.0-1.3"));
        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.MINOR, "2.0.0-1.2", 1), is("2.0.0-1.3"));

        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.PATCH, "1.0.1-1.0", 1), is("1.0.1-1.1"));
        assertThat(SemanticVersioning.getDefault().getNextVersion(PatchType.PATCH, "2.0.0-1.10", 1), is("2.0.0-1.11"));

    }
    @Test
    public void getNextApprovedVersionTest() {
        assertThat(SemanticVersioning.getDefault().getNextApprovedVersion("0.0.1"), is("1.0.0-1.0"));
        assertThat(SemanticVersioning.getDefault().getNextApprovedVersion("0.1.0"), is("1.0.0-1.0"));
        assertThat(SemanticVersioning.getDefault().getNextApprovedVersion("1.0.1"), is("1.0.1-1.0"));

        assertThat(SemanticVersioning.getDefault().getNextApprovedVersion("1.0.1-1.0"), is("1.0.1-2.0"));
        assertThat(SemanticVersioning.getDefault().getNextApprovedVersion("1.0.1-2.11"), is("1.0.1-3.0"));
    }

    @Test
    public void getRemoveVersionTest() {
        assertThat(SemanticVersioning.getDefault().removeVersionSuffix("0.0.1"), is("0.0.1"));
        assertThat(SemanticVersioning.getDefault().removeVersionSuffix("0.1.0-1.0"), is("0.1.0"));
        assertThat(SemanticVersioning.getDefault().removeVersionSuffix("2.0.1-2.11"), is("2.0.1"));
    }
}
