package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bsi.secvisogram.csaf_cms_backend.rest.response.CommentInformationResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "False positives on multiline format strings")
public class CommentWrapperTest {

    private static final String commentText = "This is a comment.";
    private static final String owner = "Mustermann";

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

}
