package de.bsi.secvisogram.csaf_cms_backend.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import de.bsi.secvisogram.csaf_cms_backend.model.WorkflowState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdvisoryWrapperTest {

    String csafJson = """
            {
              "document": {
                "category": "CSAF_BASE",
                "publisher": {
                  "category": "other",
                  "name": "John Doe"
                }
              }
            }
            """;

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void createNewFromCsafTest() throws IOException {

        var csafJson = """
                { "document": {
                      "category": "CSAF_BASE"    }
                }""";

        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann");

        assertThat(advisory.getWorkflowState(), equalTo(WorkflowState.Draft));
        assertThat(advisory.getWorkflowStateString(), equalTo("Draft"));
        assertThat(advisory.getOwner(), equalTo("Mustermann"));
        assertThat(advisory.getRevision(), is(nullValue()));
        assertThat(advisory.getAdvisoryId(), is(nullValue()));
        assertThat(advisory.at("/csaf/document/category").asText(), equalTo("CSAF_BASE"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void createFromCouchDbTest() throws IOException {

        var revision = "rev-aa-12";
        var id = "id-aaa-bbb";
        var advisoryDbString = """
                {   "owner": "Musterfrau",
                    "type": "Advisory",
                    "workflowState": "Draft",
                    "csaf": { "document": {
                                "category": "CSAF_BASE"
                              }
                            },
                    "_rev": "%s",
                    "_id": "%s"}""".formatted(revision, id);

        var advisoryStream = new ByteArrayInputStream(advisoryDbString.getBytes(StandardCharsets.UTF_8));
        var advisory = AdvisoryWrapper.createFromCouchDb(advisoryStream);

        assertThat(advisory.getWorkflowState(), equalTo(WorkflowState.Draft));
        assertThat(advisory.getWorkflowStateString(), equalTo("Draft"));
        assertThat(advisory.getOwner(), equalTo("Musterfrau"));
        assertThat(advisory.getRevision(), equalTo(revision));
        assertThat(advisory.getAdvisoryId(), equalTo(id));
        assertThat(advisory.at("/csaf/document/category").asText(), equalTo("CSAF_BASE"));
    }

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    public void updateFromExistingTest() throws IOException {

        var revision = "rev-aa-12";
        var id = "id-aaa-bbb";
        var advisoryDbString = """
                {   "owner": "Musterfrau",
                    "type": "Advisory",
                    "workflowState": "Draft",
                    "csaf": { "document": {
                                "category": "CSAF_BASE"
                              }
                            },
                    "_rev": "%s",
                    "_id": "%s"}""".formatted(revision, id);

        var updateCsafJson = """
                { "document": {
                      "category": "CHANGED",
                          "title": "New Title"
                       }
                }""";


        var advisoryStream = new ByteArrayInputStream(advisoryDbString.getBytes(StandardCharsets.UTF_8));
        var advisory = AdvisoryWrapper.createFromCouchDb(advisoryStream);
        AdvisoryWrapper updatedWrapper = AdvisoryWrapper.updateFromExisting(advisory, updateCsafJson);
        assertThat(updatedWrapper.getWorkflowState(), equalTo(WorkflowState.Draft));
        assertThat(updatedWrapper.getOwner(), equalTo("Musterfrau"));
        assertThat(updatedWrapper.getRevision(), is(nullValue()));
        assertThat(updatedWrapper.getAdvisoryId(), equalTo(id));
        assertThat(updatedWrapper.at("/csaf/document/category").asText(), equalTo("CHANGED"));
        assertThat(updatedWrapper.at("/csaf/document/title").asText(), equalTo("New Title"));
    }


    @Test
    void addCommentIdTest_invalidJsonPointer() throws IOException {
        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann");
        Assertions.assertThrows(IllegalArgumentException.class, () -> advisory.addCommentId("notAPointer", "commentId"), "A valid JSON pointer should be used.");
    }

    @Test
    void addCommentIdTest_invalidTarget() throws IOException {
        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann");
        Assertions.assertThrows(IllegalArgumentException.class, () -> advisory.addCommentId("/document/category", "commentId"), "The target must be an object node.");
    }

    @Test
    void addCommentIdTest_Field() throws IOException {
        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann");
        advisory.addCommentId("/document/publisher", "commentId");
        Assertions.assertEquals("[\"commentId\"]", advisory.getCsaf().at("/document/publisher/$comment").toString(), "A comment ID should be added to the publisher node.");
    }

    @Test
    void addCommentIdTest_FieldExistingComments() throws IOException {
        String csafJsonWithComments = """
                {
                  "document": {
                    "category": "CSAF_BASE",
                    "publisher": {
                      "category": "other",
                      "name": "John Doe",
                      "$comment": ["aCommentId"]
                    }
                  }
                }
                """;
        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJsonWithComments, "Mustermann");
        advisory.addCommentId("/document/publisher", "commentId");
        Assertions.assertEquals("[\"aCommentId\",\"commentId\"]", advisory.getCsaf().at("/document/publisher/$comment").toString(), "An additional comment ID should be added to the publisher node.");
    }


    @Test
    void removeCommentId_notExisting() throws IOException {
        AdvisoryWrapper advisory = AdvisoryWrapper.createNewFromCsaf(csafJson, "Mustermann");
        String beforeRemoval = advisory.advisoryAsString();
        advisory.removeCommentId("does not exist");
        Assertions.assertEquals(beforeRemoval, advisory.advisoryAsString(), "The advisory should not change, when there is no target");
    }

    @Test
    void removeCommentId() throws IOException {
        String csafJsonWithComments = """
                {
                  "document": {
                    "category": "CSAF_BASE",
                    "publisher": {
                      "category": "other",
                      "name": "John Doe",
                      "$comment": ["commentId"]
                    }
                  }
                }
                """;
        AdvisoryWrapper advisoryWithComment = AdvisoryWrapper.createNewFromCsaf(csafJsonWithComments, "Mustermann");
        advisoryWithComment.removeCommentId("commentId");

        String csafJsonWithCommentIDRemoved = """
                {
                  "document": {
                    "category": "CSAF_BASE",
                    "publisher": {
                      "category": "other",
                      "name": "John Doe",
                      "$comment": []
                    }
                  }
                }
                """;
        AdvisoryWrapper advisoryRemovedComment = AdvisoryWrapper.createNewFromCsaf(csafJsonWithCommentIDRemoved, "Mustermann");
        Assertions.assertEquals(advisoryRemovedComment.advisoryAsString(), advisoryWithComment.advisoryAsString(), "The comment should be removed.");
    }


}
