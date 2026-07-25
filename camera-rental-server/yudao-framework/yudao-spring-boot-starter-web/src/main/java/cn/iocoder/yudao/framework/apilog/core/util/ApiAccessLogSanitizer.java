package cn.iocoder.yudao.framework.apilog.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;

/**
 * Sanitizes API access-log request/response payloads before they reach ordinary logs or DB access logs.
 */
public final class ApiAccessLogSanitizer {

    private static final String REDACTED_TEXT = "[REDACTED]";

    private static final String UNPARSEABLE_TEXT = "[UNPARSEABLE_REDACTED]";

    private static final String[] DEFAULT_SENSITIVE_KEYS = new String[]{
            "password", "oldPassword", "newPassword",
            "token", "accessToken", "refreshToken", "access_token", "refresh_token", "authorization",
            "appKey", "appSecret", "secret", "clientSecret",
            "mobile", "phone", "tel", "contactMobile", "receiverMobile", "receiverPhone",
            "address", "detailAddress", "receiverAddress",
            "idCard", "idCardNo", "identityCard", "certNo", "certificateNo"
    };

    private ApiAccessLogSanitizer() {
    }

    public static String sanitizeMapToJson(Map<String, ?> map, String[] extraSensitiveKeys) {
        if (CollUtil.isEmpty(map)) {
            return null;
        }
        Set<String> sensitiveKeys = buildSensitiveKeys(extraSensitiveKeys);
        Map<String, Object> sanitized = new LinkedHashMap<>(map.size());
        map.forEach((key, value) -> {
            if (!isSensitiveKey(key, sensitiveKeys)) {
                sanitized.put(key, value);
            }
        });
        return JsonUtils.toJsonString(sanitized);
    }

    public static String sanitizeJsonString(String jsonString, String[] extraSensitiveKeys) {
        if (StrUtil.isEmpty(jsonString)) {
            return null;
        }
        try {
            JsonNode rootNode = JsonUtils.getObjectMapper().readTree(jsonString);
            sanitizeJsonNode(rootNode, buildSensitiveKeys(extraSensitiveKeys));
            return JsonUtils.toJsonString(rootNode);
        } catch (Exception ignored) {
            return containsSensitiveKeyHint(jsonString, extraSensitiveKeys) ? UNPARSEABLE_TEXT : jsonString;
        }
    }

    public static String sanitizeCommonResult(CommonResult<?> commonResult, String[] extraSensitiveKeys) {
        if (commonResult == null) {
            return null;
        }
        String jsonString = toJsonString(commonResult);
        try {
            JsonNode rootNode = JsonUtils.getObjectMapper().readTree(jsonString);
            sanitizeJsonNode(rootNode.get("data"), buildSensitiveKeys(extraSensitiveKeys));
            return JsonUtils.toJsonString(rootNode);
        } catch (Exception ignored) {
            return containsSensitiveKeyHint(jsonString, extraSensitiveKeys) ? UNPARSEABLE_TEXT : jsonString;
        }
    }

    public static String sanitizeForConsole(Map<String, String> queryString, String requestBody) {
        if (StrUtil.isNotEmpty(requestBody)) {
            return sanitizeJsonString(requestBody, null);
        }
        return sanitizeMapToJson(queryString, null);
    }

    private static void sanitizeJsonNode(JsonNode node, Set<String> sensitiveKeys) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode childNode : node) {
                sanitizeJsonNode(childNode, sensitiveKeys);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> iterator = node.properties().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (isSensitiveKey(entry.getKey(), sensitiveKeys)) {
                iterator.remove();
                continue;
            }
            sanitizeJsonNode(entry.getValue(), sensitiveKeys);
        }
    }

    private static Set<String> buildSensitiveKeys(String[] extraSensitiveKeys) {
        Set<String> sensitiveKeys = new HashSet<>();
        for (String key : DEFAULT_SENSITIVE_KEYS) {
            sensitiveKeys.add(normalizeKey(key));
        }
        if (extraSensitiveKeys != null) {
            for (String key : extraSensitiveKeys) {
                sensitiveKeys.add(normalizeKey(key));
            }
        }
        return sensitiveKeys;
    }

    private static boolean isSensitiveKey(String key, Set<String> sensitiveKeys) {
        return key != null && sensitiveKeys.contains(normalizeKey(key));
    }

    private static String normalizeKey(String key) {
        return StrUtil.nullToEmpty(key).replace("-", "").replace("_", "").toLowerCase();
    }

    private static boolean containsSensitiveKeyHint(String text, String[] extraSensitiveKeys) {
        String normalizedText = normalizeKey(text);
        for (String key : buildSensitiveKeys(extraSensitiveKeys)) {
            if (normalizedText.contains(key)) {
                return true;
            }
        }
        return false;
    }

}
