package cn.iocoder.yudao.module.rental.integration.xianyu.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implements the official self-developed application MD5 signing contract.
 */
public class XianyuRequestSigner {

    public String sign(String appKey, String appSecret, long timestamp, String bodyString) {
        String bodyMd5 = md5Hex(bodyString);
        return md5Hex(appKey + "," + bodyMd5 + "," + timestamp + "," + appSecret);
    }

    public boolean matches(String appKey, String appSecret, long timestamp, String bodyString, String signature) {
        if (signature == null) {
            return false;
        }
        return MessageDigest.isEqual(
                sign(appKey, appSecret, timestamp, bodyString).getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII));
    }

    private String md5Hex(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 is required by the XianGuanJia protocol", exception);
        }
    }

}
