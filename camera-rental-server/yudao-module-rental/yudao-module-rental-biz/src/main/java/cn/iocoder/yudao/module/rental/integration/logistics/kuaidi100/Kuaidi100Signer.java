package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class Kuaidi100Signer {

    public String signQuery(String param, String key, String customer) {
        return md5Upper(param + key + customer);
    }

    public String signCallback(String param, String salt) {
        return md5Upper(param + salt);
    }

    public boolean verifyCallback(String param, String salt, String signature) {
        if (param == null || salt == null || signature == null) {
            return false;
        }
        byte[] expected = signCallback(param, salt).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = signature.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    private String md5Upper(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 must be available", exception);
        }
    }
}
