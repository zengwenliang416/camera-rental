package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class XianyuProductSkuPayloadParser {

    private final ObjectMapper objectMapper;
    private final XianyuChannelIdentifierNormalizer identifierNormalizer;

    public XianyuProductSkuPayloadParser(ObjectMapper objectMapper,
                                         XianyuChannelIdentifierNormalizer identifierNormalizer) {
        this.objectMapper = objectMapper;
        this.identifierNormalizer = identifierNormalizer;
    }

    public List<XianyuProductSkuGroup> parse(String rawPayload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException exception) {
            throw malformed("XianGuanJia product SKU payload is malformed", exception);
        }
        if (root.has("code") && (!root.path("code").canConvertToInt() || root.path("code").intValue() != 0)) {
            throw malformed("XianGuanJia product SKU payload is not a successful response");
        }
        JsonNode list = root.path("data").path("list");
        if (!list.isArray()) {
            throw malformed("XianGuanJia product SKU payload is missing data.list");
        }
        List<XianyuProductSkuGroup> groups = new ArrayList<>(list.size());
        for (JsonNode productNode : list) {
            groups.add(new XianyuProductSkuGroup(requiredIdentifier(productNode, "product_id"),
                    parseSkuItems(productNode.path("sku_items"))));
        }
        return List.copyOf(groups);
    }

    private List<XianyuProductSkuSnapshot> parseSkuItems(JsonNode skuItems) {
        if (skuItems == null || skuItems.isMissingNode() || skuItems.isNull()) {
            return List.of();
        }
        if (!skuItems.isArray()) {
            throw malformed("XianGuanJia product SKU payload field sku_items is invalid");
        }
        List<XianyuProductSkuSnapshot> snapshots = new ArrayList<>(skuItems.size());
        for (JsonNode skuNode : skuItems) {
            snapshots.add(new XianyuProductSkuSnapshot(
                    requiredIdentifier(skuNode, "sku_id"),
                    optionalIdentifier(skuNode, "xy_sku_id"),
                    requiredText(skuNode, "sku_text", 512),
                    requiredStock(skuNode)));
        }
        return List.copyOf(snapshots);
    }

    private Integer requiredStock(JsonNode node) {
        JsonNode value = node.path("stock");
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0) {
            throw malformed("XianGuanJia product SKU payload field stock is invalid");
        }
        return value.intValue();
    }

    private String requiredIdentifier(JsonNode node, String fieldName) {
        try {
            return identifierNormalizer.normalizeRequired(node, fieldName);
        } catch (IllegalArgumentException exception) {
            throw malformed("XianGuanJia product SKU payload has invalid " + fieldName, exception);
        }
    }

    private String optionalIdentifier(JsonNode node, String fieldName) {
        try {
            return identifierNormalizer.normalizeOptional(node, fieldName);
        } catch (IllegalArgumentException exception) {
            throw malformed("XianGuanJia product SKU payload has invalid " + fieldName, exception);
        }
    }

    private String requiredText(JsonNode node, String fieldName, int maxLength) {
        JsonNode value = node.path(fieldName);
        if (!value.isTextual() || value.textValue().isBlank() || value.textValue().length() > maxLength) {
            throw malformed("XianGuanJia product SKU payload field " + fieldName
                    + " must be a bounded non-empty string");
        }
        return value.textValue();
    }

    private XianyuClientException malformed(String message) {
        return new XianyuClientException(XianyuClientException.Kind.MALFORMED_RESPONSE, message);
    }

    private XianyuClientException malformed(String message, Exception cause) {
        return new XianyuClientException(XianyuClientException.Kind.MALFORMED_RESPONSE, message, cause);
    }

}
