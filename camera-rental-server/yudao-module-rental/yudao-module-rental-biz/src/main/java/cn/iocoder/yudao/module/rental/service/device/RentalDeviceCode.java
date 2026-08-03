package cn.iocoder.yudao.module.rental.service.device;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Customer-facing machine code, for example {@code P4-01}.
 */
public final class RentalDeviceCode {

    private static final Pattern DASHES = Pattern.compile("[‐‑‒–—―−﹘﹣－]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern VALID =
            Pattern.compile("^(?=.{4,64}$)[A-Z0-9]+(?:-[A-Z0-9]+)*-\\d{2}$");

    private RentalDeviceCode() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = DASHES.matcher(value).replaceAll("-");
        normalized = WHITESPACE.matcher(normalized).replaceAll("");
        return normalized.toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String value) {
        return VALID.matcher(normalize(value)).matches();
    }

    public static String format(String prefix, int sequence) {
        if (sequence < 1 || sequence > 99) {
            throw new IllegalArgumentException("设备短码序号必须在 01-99 之间");
        }
        String normalizedPrefix = normalizePrefix(prefix);
        if (normalizedPrefix.isBlank()) {
            throw new IllegalArgumentException("设备短码前缀不能为空");
        }
        String code = normalizedPrefix + "-" + String.format(Locale.ROOT, "%02d", sequence);
        if (!isValid(code)) {
            throw new IllegalArgumentException("设备短码格式无效");
        }
        return code;
    }

    private static String normalizePrefix(String value) {
        String normalized = normalize(value).replaceAll("[^A-Z0-9-]", "-");
        normalized = normalized.replaceAll("-+", "-");
        return normalized.replaceAll("^-|-$", "");
    }
}
