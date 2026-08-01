package cn.iocoder.yudao.module.rental.service.returnregistration;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ReturnSerialNormalizer {

    private static final Pattern DASHES = Pattern.compile("[‐‑‒–—―−﹘﹣－]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern VALID =
            Pattern.compile("^[A-Z0-9]{1,8}(?:-[A-Z0-9]{1,8}){1,4}$");

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = DASHES.matcher(value).replaceAll("-");
        normalized = WHITESPACE.matcher(normalized).replaceAll("");
        return normalized.toUpperCase(Locale.ROOT);
    }

    public boolean isValid(String value) {
        return VALID.matcher(normalize(value)).matches();
    }
}
