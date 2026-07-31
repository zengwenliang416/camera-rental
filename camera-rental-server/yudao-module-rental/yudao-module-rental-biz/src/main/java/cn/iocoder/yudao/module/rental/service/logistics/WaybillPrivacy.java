package cn.iocoder.yudao.module.rental.service.logistics;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class WaybillPrivacy {

    public String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new RentalLogisticsException("WAYBILL_REQUIRED");
        }
        String normalized = raw.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);
        if (normalized.length() < 4 || normalized.length() > 128
                || !normalized.matches("[A-Z0-9]+")) {
            throw new RentalLogisticsException("WAYBILL_INVALID");
        }
        return normalized;
    }

    public String mask(String raw) {
        String normalized = normalize(raw);
        if (normalized.length() <= 6) {
            return normalized.substring(0, 2) + "***" + normalized.substring(normalized.length() - 1);
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }
}
