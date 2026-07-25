package cn.iocoder.yudao.framework.apilog.core.util;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAccessLogSanitizerTest {

    @Test
    void sanitizeForConsole_removesSensitiveJsonFields() {
        String body = """
                {
                  "username": "admin",
                  "password": "admin123",
                  "nested": {
                    "AppSecret": "secret-value",
                    "receiverPhone": "13800138000",
                    "safe": "visible"
                  }
                }
                """;

        String result = ApiAccessLogSanitizer.sanitizeForConsole(Map.of(), body);

        assertTrue(result.contains("admin"));
        assertTrue(result.contains("visible"));
        assertFalse(result.contains("admin123"));
        assertFalse(result.contains("secret-value"));
        assertFalse(result.contains("13800138000"));
        assertFalse(result.contains("password"));
        assertFalse(result.contains("AppSecret"));
        assertFalse(result.contains("receiverPhone"));
    }

    @Test
    void sanitizeMapToJson_removesCaseAndSeparatorInsensitiveTokenFields() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("refresh_token", "refresh-secret");
        query.put("Access-Token", "access-secret");
        query.put("pageNo", "1");

        String result = ApiAccessLogSanitizer.sanitizeMapToJson(query, null);

        assertTrue(result.contains("pageNo"));
        assertTrue(result.contains("1"));
        assertFalse(result.contains("refresh-secret"));
        assertFalse(result.contains("access-secret"));
        assertFalse(result.contains("refresh_token"));
        assertFalse(result.contains("Access-Token"));
    }

    @Test
    void sanitizeJsonString_redactsUnparseableSensitiveBody() {
        String result = ApiAccessLogSanitizer.sanitizeJsonString(
                "password=admin123&mobile=13800138000", null);

        assertTrue(result.contains("[UNPARSEABLE_REDACTED]"));
        assertFalse(result.contains("admin123"));
        assertFalse(result.contains("13800138000"));
    }

    @Test
    void sanitizeCommonResult_onlySanitizesDataFields() {
        CommonResult<Map<String, Object>> response = CommonResult.success(Map.of(
                "accessToken", "access-secret",
                "refreshToken", "refresh-secret",
                "nickname", "visible"));

        String result = ApiAccessLogSanitizer.sanitizeCommonResult(response, null);

        assertTrue(result.contains("visible"));
        assertTrue(result.contains("\"code\""));
        assertFalse(result.contains("access-secret"));
        assertFalse(result.contains("refresh-secret"));
        assertFalse(result.contains("accessToken"));
        assertFalse(result.contains("refreshToken"));
    }

}
