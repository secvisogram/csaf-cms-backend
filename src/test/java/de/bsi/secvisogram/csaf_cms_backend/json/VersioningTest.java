package de.bsi.secvisogram.csaf_cms_backend.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;

public class VersioningTest {

    @Test
    public void getStrategyTest() {

        assertThat(Versioning.getStrategy("Semantic"),  instanceOf(SemanticVersioning.class));
        assertThat(Versioning.getStrategy("Integer"),  instanceOf(IntegerVersioning.class));
        assertThat(Versioning.getStrategy(null),  instanceOf(SemanticVersioning.class));
        assertThat(Versioning.getStrategy(""),  instanceOf(SemanticVersioning.class));
        assertThat(Versioning.getStrategy("123"),  instanceOf(SemanticVersioning.class));
    }
}
