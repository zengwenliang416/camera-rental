package cn.iocoder.yudao.module.rental.service.device;

import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Permanent device QR payload: signs deviceNo + equipmentModelCode so a label cannot claim a wrong model.
 * Format: {@code CRD1|{deviceNo}|{equipmentModelCode}|{sig16}} — deterministic and stable for a device.
 */
@Component
public class RentalDeviceQrCodec {

    public static final String VERSION = "CRD1";
    private static final String SEP = "|";
    private static final int SIG_HEX_LEN = 16;

    private final RentalDeviceProperties properties;

    public RentalDeviceQrCodec(RentalDeviceProperties properties) {
        this.properties = properties;
    }

    public String encode(String deviceNo, String equipmentModelCode) {
        String dn = requireToken(deviceNo, "deviceNo");
        String mc = requireToken(equipmentModelCode, "equipmentModelCode");
        String body = VERSION + SEP + dn + SEP + mc;
        String sig = properties.isQrSigned() ? signHex16(body) : "";
        return body + SEP + sig;
    }

    public ParsedPayload decode(String payload) {
        if (!StringUtils.hasText(payload)) {
            throw new IllegalArgumentException("QR payload is blank");
        }
        String text = payload.trim();
        String[] parts = text.split("\\" + SEP, -1);
        if (parts.length != 4 || !VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("Unsupported device QR payload");
        }
        String deviceNo = requireToken(parts[1], "deviceNo");
        String modelCode = requireToken(parts[2], "equipmentModelCode");
        String sig = parts[3] == null ? "" : parts[3].trim();
        String body = VERSION + SEP + deviceNo + SEP + modelCode;
        boolean signed = false;
        if (properties.isQrSigned()) {
            if (!StringUtils.hasText(sig)) {
                throw new IllegalArgumentException("Device QR signature missing");
            }
            String expected = signHex16(body);
            if (!constantTimeEquals(expected, sig.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Device QR signature invalid");
            }
            signed = true;
        }
        return new ParsedPayload(VERSION, deviceNo, modelCode, signed, text);
    }

    private String signHex16(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.getQrSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] full = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(full).substring(0, SIG_HEX_LEN);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign device QR payload", ex);
        }
    }

    private static String requireToken(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is blank");
        }
        String token = value.trim();
        if (token.contains(SEP)) {
            throw new IllegalArgumentException(field + " must not contain '|'");
        }
        return token;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    public record ParsedPayload(String version, String deviceNo, String equipmentModelCode, boolean signed,
                                String rawPayload) {
    }

}
