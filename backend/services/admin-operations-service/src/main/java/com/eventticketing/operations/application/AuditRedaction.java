package com.eventticketing.operations.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Projection excludes credentials and unstructured transfer evidence at the source boundary. */
public final class AuditRedaction {
    private AuditRedaction(){}
    public static JsonNode redact(JsonNode value){
        if(value==null||value.isNull())return NullNode.instance;
        if(value.isObject()){
            ObjectNode safe=JsonNodeFactory.instance.objectNode();value.fields().forEachRemaining(e->{
                String name=e.getKey().toLowerCase(Locale.ROOT).replace("_","");
                if(name.matches(".*(password|secret|token|credential|encrypted|accountnumber|cardnumber|cvv|evidence|decision|authorization).*"))return;
                safe.set(e.getKey(),redact(e.getValue()));
            });return safe;
        }
        if(value.isArray()){ArrayNode safe=JsonNodeFactory.instance.arrayNode();value.forEach(v->safe.add(redact(v)));return safe;}
        if(value.isTextual())return TextNode.valueOf(value.asText().replaceAll("(?<![A-Za-z0-9])([0-9]{4,15})([0-9]{4})(?![A-Za-z0-9])","•••• $2"));
        return value.deepCopy();
    }
}
