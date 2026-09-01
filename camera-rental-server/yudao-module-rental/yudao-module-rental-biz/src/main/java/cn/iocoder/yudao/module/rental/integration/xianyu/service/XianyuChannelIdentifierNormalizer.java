package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Preserves channel identifiers as strings without coercing unrelated JSON types.
 */
@Component
public class XianyuChannelIdentifierNormalizer {

    public String normalizeOptional(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isIntegralNumber()) {
            return value.asText();
        }
        if (value.isTextual()) {
            String normalized = value.textValue().trim();
            return normalized.isEmpty() ? null : normalized;
        }
        throw new IllegalArgumentException("Channel identifier " + fieldName + " must be a string or integer");
    }

    public String normalizeRequired(JsonNode node, String fieldName) {
        String value = normalizeOptional(node, fieldName);
        if (value == null) {
            throw new IllegalArgumentException("Channel identifier " + fieldName + " is required");
        }
        return value;
    }

}
