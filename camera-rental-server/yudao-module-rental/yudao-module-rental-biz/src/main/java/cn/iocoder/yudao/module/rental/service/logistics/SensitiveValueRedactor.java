package cn.iocoder.yudao.module.rental.service.logistics;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SensitiveValueRedactor {

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1\\d{10}(?!\\d)");
    private static final Pattern LONG_IDENTIFIER = Pattern.compile("(?<![A-Za-z0-9])[A-Za-z0-9]{10,}(?![A-Za-z0-9])");

    public String redact(String value) {
        if (value == null) {
            return null;
        }
        String safe = PHONE.matcher(value).replaceAll("1**********");
        return LONG_IDENTIFIER.matcher(safe).replaceAll("***");
    }
}
