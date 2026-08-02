package cn.iocoder.yudao.module.rental.service.returnregistration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnRegistrationSessionCookieServiceTest {

    @Test
    void writesSecureHttpOnlySameSiteCookieForTwentyFourHours() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ReturnRegistrationSessionCookieService().write(response, "session-token");

        String cookie = response.getHeader("Set-Cookie");
        assertTrue(cookie.startsWith("rental_return_session=session-token;"));
        assertTrue(cookie.contains("Path=/app-api/rental/return-registration"));
        assertTrue(cookie.contains("Max-Age=86400"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
    }
}
