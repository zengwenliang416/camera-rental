package cn.iocoder.yudao.module.rental.integration.xianyu.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
/**
 * Creates a deterministic compact JSON body before signing and transport.
 */
public class XianyuCanonicalJson {

    private final ObjectMapper objectMapper;

    public XianyuCanonicalJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(JsonNode body) {
        try {
            return objectMapper.writeValueAsString(canonicalize(body == null ? JsonNodeFactory.instance.objectNode() : body));
        } catch (JsonProcessingException exception) {
            throw new XianyuClientException(XianyuClientException.Kind.MALFORMED_REQUEST,
                    "Unable to serialize XianGuanJia request body", exception);
        }
    }

    private JsonNode canonicalize(JsonNode value) {
        if (value.isObject()) {
            ObjectNode canonical = JsonNodeFactory.instance.objectNode();
            List<String> fieldNames = new ArrayList<>();
            Iterator<String> iterator = value.fieldNames();
            iterator.forEachRemaining(fieldNames::add);
            Collections.sort(fieldNames);
            for (String fieldName : fieldNames) {
                canonical.set(fieldName, canonicalize(value.get(fieldName)));
            }
            return canonical;
        }
        if (value.isArray()) {
            ArrayNode canonical = JsonNodeFactory.instance.arrayNode();
            for (JsonNode item : value) {
                canonical.add(canonicalize(item));
            }
            return canonical;
        }
        return value.deepCopy();
    }

}
