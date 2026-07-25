package cn.iocoder.yudao.module.rental.integration.xianyu.security;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class XianyuSafeErrorCodeTest {

    @Test
    void shouldExposeOnlyStructuredClientDiagnostics() {
        XianyuClientException exception = new XianyuClientException(
                XianyuClientException.Kind.REMOTE_RESPONSE,
                "secret-bearing remote response", 200, 100001);

        String code = XianyuSafeErrorCode.from(exception);

        assertEquals("XGJ_REMOTE_RESPONSE_HTTP_200_REMOTE_100001", code);
        assertFalse(code.contains("secret-bearing"));
    }

}
