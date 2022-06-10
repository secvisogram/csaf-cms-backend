package de.bsi.secvisogram.csaf_cms_backend.json;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Convenience class to store a tuple of JSON pointers
 */
public class ObjectAndTailPointer {

    JsonPointer objPtr;
    JsonPointer tailPtr;

    public ObjectAndTailPointer(JsonPointer objPtr, JsonPointer tailPtr) {
        this.objPtr = objPtr;
        this.tailPtr = tailPtr;
    }

    public JsonPointer getObjPtr() {
        return objPtr;
    }

    public void setObjPtr(JsonPointer objPtr) {
        this.objPtr = objPtr;
    }

    public JsonPointer getTailPtr() {
        return tailPtr;
    }

    public void setTailPtr(JsonPointer tailPtr) {
        this.tailPtr = tailPtr;
    }

    /**
     * Traverses the JSON tree up to the last object node targeted by the given JSON pointer
     * and stores that node and an optional tail to a non-object leaf node
     *
     * @param jPtr the JSON pointer to any node in the JSON tree
     * @param tree the JSON tree to traverse
     * @return a tuple of JsonPointers to the last object node and the pointer from that node to the leaf
     */
    public static ObjectAndTailPointer lastObjectNodeAndTail(JsonPointer jPtr, JsonNode tree) {
        JsonNode curNode = tree.at(jPtr);
        if (curNode.isMissingNode()) {
            throw new IllegalArgumentException("The given json pointer expression leads nowhere.");
        }
        if (curNode.isObject()) {
            return new ObjectAndTailPointer(jPtr, JsonPointer.empty());
        } else {
            ObjectAndTailPointer prev = lastObjectNodeAndTail(jPtr.head(), tree);
            return new ObjectAndTailPointer(prev.getObjPtr(), prev.getTailPtr().append(jPtr.last()));
        }
    }
}
