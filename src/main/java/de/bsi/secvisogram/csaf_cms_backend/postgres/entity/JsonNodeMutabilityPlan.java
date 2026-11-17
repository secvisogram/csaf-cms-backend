package de.bsi.secvisogram.csaf_cms_backend.postgres.entity;

import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import tools.jackson.databind.JsonNode;

/**
 * A {@link JsonNode} is mutable: Hibernate keeps a deep copy to detect changes made in place.
 */
public class JsonNodeMutabilityPlan extends MutableMutabilityPlan<JsonNode> {

    @Override
    protected JsonNode deepCopyNotNull(JsonNode value) {
        return value.deepCopy();
    }
}
