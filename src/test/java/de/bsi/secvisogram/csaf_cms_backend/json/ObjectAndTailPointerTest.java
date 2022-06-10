package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ObjectAndTailPointerTest {

    @Test
    @SuppressFBWarnings(value = "CE_CLASS_ENVY", justification = "Only for Test")
    void lastObjectNodeAndTailTest() throws JsonProcessingException {

        String jsonString = """
                {
                    "topLevel": {
                        "key1": "123",
                        "key2": {
                            "subkey1": 123,
                            "subkey2": "abcd",
                            "subkey3": {
                                "deepestKey": "deep"
                            }
                        }
                    }
                }
                """;

        final ObjectMapper jacksonMapper = new ObjectMapper();
        JsonNode jsonTree = jacksonMapper.readValue(jsonString, JsonNode.class);

        JsonPointer jPtrNoTarget = JsonPointer.compile("/targets/something/that/does/not/exist");
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjectAndTailPointer.lastObjectNodeAndTail(jPtrNoTarget, jsonTree));

        JsonPointer objectPtr = JsonPointer.compile("/topLevel");
        ObjectAndTailPointer objNoTail = ObjectAndTailPointer.lastObjectNodeAndTail(objectPtr, jsonTree);
        Assertions.assertEquals("/topLevel", objNoTail.objPtr.toString());
        Assertions.assertEquals("", objNoTail.tailPtr.toString());

        JsonPointer objectPtr2 = JsonPointer.compile("/topLevel/key2/subkey3");
        ObjectAndTailPointer objNoTail2 = ObjectAndTailPointer.lastObjectNodeAndTail(objectPtr2, jsonTree);
        Assertions.assertEquals("/topLevel/key2/subkey3", objNoTail2.objPtr.toString());
        Assertions.assertEquals("", objNoTail2.tailPtr.toString());

        JsonPointer leafPtr = JsonPointer.compile("/topLevel/key2/subkey1");
        ObjectAndTailPointer objTail = ObjectAndTailPointer.lastObjectNodeAndTail(leafPtr, jsonTree);
        Assertions.assertEquals("/topLevel/key2", objTail.objPtr.toString());
        Assertions.assertEquals("/subkey1", objTail.tailPtr.toString());


        JsonPointer leafPtr2 = JsonPointer.compile("/topLevel/key2/subkey3/deepestKey");
        ObjectAndTailPointer objTail2 = ObjectAndTailPointer.lastObjectNodeAndTail(leafPtr2, jsonTree);
        Assertions.assertEquals("/topLevel/key2/subkey3", objTail2.objPtr.toString());
        Assertions.assertEquals("/deepestKey", objTail2.tailPtr.toString());

    }

}
