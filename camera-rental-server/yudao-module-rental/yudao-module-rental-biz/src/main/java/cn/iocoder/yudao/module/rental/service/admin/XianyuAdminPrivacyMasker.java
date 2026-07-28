package cn.iocoder.yudao.module.rental.service.admin;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts channel facts that ordinary management lists do not need in full.
 */
final class XianyuAdminPrivacyMasker {

    private static final Pattern MAINLAND_MOBILE = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");
    private static final Pattern LONG_IDENTIFIER = Pattern.compile("(?<!\\d)(\\d{10,})(?!\\d)");
    private static final int IDENTIFIER_EDGE_LENGTH = 3;
    private static final Pattern ADDRESS_LIKE = Pattern.compile(
            "(收货地址|寄回地址|发货地址|详细地址|地址)([:：\\s]*)([^#，,;；\\r\\n]+)");
    private static final Pattern NAME_LIKE = Pattern.compile(
            "(收件人|姓名|联系人)([:：\\s]*)([^#，,;；\\r\\n]+)");

    private XianyuAdminPrivacyMasker() {
    }

    static String maskFreeText(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String masked = replaceSensitiveValue(value, ADDRESS_LIKE);
        masked = replaceSensitiveValue(masked, NAME_LIKE);
        masked = replaceMobile(masked);
        return replaceLongIdentifier(masked);
    }

    static String maskIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String text = value.trim();
        if (text.length() <= IDENTIFIER_EDGE_LENGTH * 2) {
            return text.charAt(0) + "***" + text.charAt(text.length() - 1);
        }
        return text.substring(0, IDENTIFIER_EDGE_LENGTH) + "***"
                + text.substring(text.length() - IDENTIFIER_EDGE_LENGTH);
    }

    static String maskName(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String text = value.trim();
        int firstCodePointEnd = text.offsetByCodePoints(0, 1);
        return text.substring(0, firstCodePointEnd) + "*";
    }

    static String maskMobile(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String text = value.trim();
        String masked = replaceMobile(text);
        if (!masked.equals(text) || text.contains("*")) {
            return masked;
        }
        return maskIdentifier(text);
    }

    static String maskAddress(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String text = value.trim();
        if (text.contains("***")) {
            return text;
        }
        int codePointCount = text.codePointCount(0, text.length());
        int keptCodePoints = Math.min(6, codePointCount);
        int prefixEnd = text.offsetByCodePoints(0, keptCodePoints);
        return text.substring(0, prefixEnd) + "***";
    }

    private static String replaceSensitiveValue(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    matcher.group(1) + matcher.group(2) + "***"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceMobile(String value) {
        Matcher matcher = MAINLAND_MOBILE.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String text = matcher.group(1);
            matcher.appendReplacement(result, text.substring(0, 3) + "****" + text.substring(text.length() - 4));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceLongIdentifier(String value) {
        Matcher matcher = LONG_IDENTIFIER.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String text = matcher.group(1);
            String replacement = text.length() <= 6
                    ? "***"
                    : text.substring(0, 3) + "***" + text.substring(text.length() - 3);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }

}
