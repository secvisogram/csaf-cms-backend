package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.rest.request.CreateCommentRequest;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.AnswerInformationResponse;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.CommentInformationResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings(value = {"VA_FORMAT_STRING_USES_NEWLINE", "CE_CLASS_ENVY"},
        justification = "False positives on multiline format strings; class envy tolerated for tests")
public class CommentWrapperTest {

    private static final String commentText = "This is a comment.";
    private static final String owner = "Mustermann";


    @Test
    public void createNewCommentFromJsonTest_missingTextField() {

        String advisoryId = UUID.randomUUID().toString();
        CreateCommentRequest comment = new CreateCommentRequest(null, "nodeId123");

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CommentWrapper.createNew(advisoryId, comment), "missing commentText");
    }

    @Test
    public void createNewCommentFromJsonTest_CsafNodeIdNUll() {

        String advisoryId = UUID.randomUUID().toString();
        CreateCommentRequest comment = new CreateCommentRequest("This is a comment", null);
        CommentWrapper.createNew(advisoryId, comment);
    }


    @Test
    public void createNewCommentFromJsonTest_minimumFields() {

        String advisoryId = UUID.randomUUID().toString();
        CreateCommentRequest comment = new CreateCommentRequest(commentText,  UUID.randomUUID().toString());

        CommentWrapper wrapper = CommentWrapper.createNew(advisoryId, comment);

        Assertions.assertEquals(commentText, wrapper.getText());
        Assertions.assertEquals(advisoryId, wrapper.getAdvisoryId());
    }

    @Test
    public void createNewCommentFromJsonTest_allFields() {

        String advisoryId = UUID.randomUUID().toString();
        String csafNodeId = UUID.randomUUID().toString();
        CreateCommentRequest comment = new CreateCommentRequest()
                .setCommentText(commentText).setCsafNodeId(csafNodeId).setFieldName("publisher");

        CommentWrapper wrapper = CommentWrapper.createNew(advisoryId, comment);

        Assertions.assertEquals(commentText, wrapper.getText());
        Assertions.assertNull(wrapper.getAnswerTo());
        Assertions.assertEquals(csafNodeId, wrapper.getCsafNodeId());
        Assertions.assertEquals(advisoryId, wrapper.getAdvisoryId());
        Assertions.assertNull(wrapper.getOwner());
        Assertions.assertEquals("publisher", wrapper.getFieldName());
    }

    @Test
    public void createNewAnswerFromJsonTest() {

        String advisoryId = UUID.randomUUID().toString();
        String commentId = UUID.randomUUID().toString();
        String answerText = "This is an answer.";

        CommentWrapper wrapper = CommentWrapper.createNewAnswerFromJson(advisoryId, commentId, answerText);

        Assertions.assertEquals("This is an answer.", wrapper.getText());
        Assertions.assertNull(wrapper.getCsafNodeId());
        Assertions.assertEquals(advisoryId, wrapper.getAdvisoryId());
        Assertions.assertEquals(commentId, wrapper.getAnswerTo());
    }

    @Test
    public void createFromCouchDbTest() throws IOException {

        String revision = "revision-abcd-1234";
        String commentId = UUID.randomUUID().toString();
        String advisoryId = UUID.randomUUID().toString();
        String commentDbString = String.format(
                """
                {
                    "commentText": "%s",
                    "csafNodeId": "nodeId123",
                    "advisoryId": "%s",
                    "owner": "%s",
                    "type": "Comment",
                    "_rev": "%s",
                    "_id": "%s"
                }
                """, commentText, advisoryId, owner, revision, commentId);

        InputStream commentStream = new ByteArrayInputStream(commentDbString.getBytes(StandardCharsets.UTF_8));
        CommentWrapper comment = CommentWrapper.createFromCouchDb(commentStream);

        Assertions.assertEquals(commentText, comment.getText());
        Assertions.assertEquals("nodeId123", comment.getCsafNodeId());
        Assertions.assertEquals(owner, comment.getOwner());
        Assertions.assertEquals(revision, comment.getRevision());
        Assertions.assertEquals(commentId, comment.getCommentId());
    }

    @Test
    public void convertToCommentInfoTest() {

        String revision = "rev-abc-123";
        String commentId = UUID.randomUUID().toString();
        String advisoryId = UUID.randomUUID().toString();

        ObjectNode commentNode = new ObjectMapper().createObjectNode();
        commentNode.put("owner", owner);
        commentNode.put("text", commentText);
        commentNode.put("csafNodeId", "field123");
        commentNode.put("advisoryId", advisoryId);
        commentNode.put("type", "Comment");
        commentNode.put("_rev", revision);
        commentNode.put("_id", commentId);

        CommentInformationResponse commentInfo = CommentWrapper.convertToCommentInfo(commentNode);

        Assertions.assertEquals(commentId, commentInfo.getCommentId());
        Assertions.assertEquals(advisoryId, commentInfo.getAdvisoryId());
        Assertions.assertEquals(owner, commentInfo.getOwner());

    }

    @Test
    public void convertToAnswerInfoTest() {

        String revision = "rev-abc-123";
        String answerId = UUID.randomUUID().toString();
        String commentId = UUID.randomUUID().toString();

        ObjectNode commentNode = new ObjectMapper().createObjectNode();
        commentNode.put("owner", owner);
        commentNode.put("text", commentText);
        commentNode.put("answerTo", commentId);
        commentNode.put("type", "Answer");
        commentNode.put("_rev", revision);
        commentNode.put("_id", answerId);

        AnswerInformationResponse answerInfo = CommentWrapper.convertToAnswerInfo(commentNode);

        Assertions.assertEquals(answerId, answerInfo.getAnswerId());
        Assertions.assertEquals(commentId, answerInfo.getAnswerTo());
        Assertions.assertEquals(owner, answerInfo.getOwner());

    }

}
