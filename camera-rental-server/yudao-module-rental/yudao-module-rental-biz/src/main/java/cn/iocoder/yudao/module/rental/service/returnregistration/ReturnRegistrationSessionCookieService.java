package cn.iocoder.yudao.module.rental.service.returnregistration;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ReturnRegistrationSessionCookieService {

    public static final String COOKIE_NAME = "rental_return_session";
    public static final Duration SESSION_DURATION = Duration.ofHours(24);
    private static final String COOKIE_PATH = "/app-api/rental/return-registration";

    public void write(HttpServletResponse response, String sessionToken) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, sessionToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(SESSION_DURATION)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
