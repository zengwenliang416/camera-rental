package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ReturnRegistrationTokenService {

    private final SecureRandom random = new SecureRandom();

    public IssuedToken issue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedToken(token, hash(token));
    }

    public String hash(String token) {
        if (token == null || token.length() < 40 || token.length() > 128) {
            return DigestUtil.sha256Hex("invalid-return-registration-token");
        }
        return DigestUtil.sha256Hex(token);
    }

    public static String rateLimitKey(String token) {
        if (token == null || token.isBlank()) {
            return "invalid";
        }
        return DigestUtil.sha256Hex(token).substring(0, 16);
    }

    public record IssuedToken(String plaintext, String hash) {
    }
}
