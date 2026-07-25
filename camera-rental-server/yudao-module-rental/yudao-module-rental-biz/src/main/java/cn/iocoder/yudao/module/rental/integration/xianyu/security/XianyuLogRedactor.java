package cn.iocoder.yudao.module.rental.integration.xianyu.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Produces safe diagnostics without keeping ordinary logs useful for PII recovery.
 */
public class XianyuLogRedactor {

    private static final String MASK = "***";
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "appsecret", "appkey", "sign", "authorization", "token",
            "receiver_name", "receiver_mobile", "mobile", "phone",
            "address", "buyer_eid", "buyer_nick", "user_name", "user_identity",
            "id_card", "identity_no", "pay_no");

    private final ObjectMapper objectMapper;

    public XianyuLogRedactor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String redactJson(String rawJson) {
        try {
            return objectMapper.writeValueAsString(redact(objectMapper.readTree(rawJson)));
        } catch (JsonProcessingException exception) {
            return "[unparseable payload redacted]";
        }
    }

    private JsonNode redact(JsonNode value) {
        if (value.isObject()) {
            ObjectNode redacted = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = value.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                redacted.set(field.getKey(), isSensitive(field.getKey())
                        ? JsonNodeFactory.instance.textNode(MASK)
                        : redact(field.getValue()));
            }
            return redacted;
        }
        if (value.isArray()) {
            ArrayNode redacted = JsonNodeFactory.instance.arrayNode();
            for (JsonNode item : value) {
                redacted.add(redact(item));
            }
            return redacted;
        }
        return value.deepCopy();
    }

    private boolean isSensitive(String fieldName) {
        return SENSITIVE_FIELDS.contains(fieldName.toLowerCase(Locale.ROOT));
    }

}
