package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses authorized-shop list responses without logging private fields.
 */
public class XianyuAuthorizedShopListParser {

    public static final String GUARANTEE_STATUS_HEALTHY = "HEALTHY";
    public static final String GUARANTEE_STATUS_DEPOSIT_INSUFFICIENT = "DEPOSIT_INSUFFICIENT";
    public static final String GUARANTEE_STATUS_UNKNOWN = "UNKNOWN";

    public List<XianyuAuthorizedShop> parse(JsonNode payload) {
        List<XianyuAuthorizedShop> shops = new ArrayList<>();
        if (payload == null) {
            throw malformed();
        }
        JsonNode list = payload.path("data").path("list");
        if (!list.isArray()) {
            list = payload.path("list");
        }
        if (!list.isArray()) {
            throw malformed();
        }
        for (JsonNode item : list) {
            String authorizeId = text(item, "authorize_id");
            String externalShopId = firstNonBlank(text(item, "seller_id"), text(item, "shop_id"));
            String shopName = firstNonBlank(text(item, "shop_name"), text(item, "user_nick"), text(item, "user_name"));
            boolean valid = !item.has("is_valid") || item.path("is_valid").asBoolean(true);
            Long validEnd = item.path("valid_end_time").isNumber() ? item.path("valid_end_time").asLong() : null;
            String guaranteeStatus = parseGuaranteeStatus(item);
            if (authorizeId == null || externalShopId == null) {
                continue;
            }
            shops.add(new XianyuAuthorizedShop(authorizeId, externalShopId, shopName, valid, validEnd, guaranteeStatus));
        }
        return shops;
    }

    private static String parseGuaranteeStatus(JsonNode item) {
        JsonNode value = item.path("is_deposit_enough");
        if (value.isMissingNode() || value.isNull()) {
            return GUARANTEE_STATUS_UNKNOWN;
        }
        if (value.isBoolean()) {
            return value.asBoolean() ? GUARANTEE_STATUS_HEALTHY : GUARANTEE_STATUS_DEPOSIT_INSUFFICIENT;
        }
        String text = value.asText(null);
        if (text == null || text.isBlank()) {
            return GUARANTEE_STATUS_UNKNOWN;
        }
        String normalized = text.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return GUARANTEE_STATUS_HEALTHY;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return GUARANTEE_STATUS_DEPOSIT_INSUFFICIENT;
        }
        return GUARANTEE_STATUS_UNKNOWN;
    }

    private static XianyuClientException malformed() {
        return new XianyuClientException(XianyuClientException.Kind.MALFORMED_RESPONSE,
                "XianGuanJia authorized-shop response is missing data.list");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        // authorize_id / seller_id are often numeric in the JSON payload
        if (value.isNumber()) {
            return value.asText();
        }
        String text = value.asText(null);
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

}
