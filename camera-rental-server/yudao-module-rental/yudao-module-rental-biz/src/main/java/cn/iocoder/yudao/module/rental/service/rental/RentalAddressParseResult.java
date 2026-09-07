package cn.iocoder.yudao.module.rental.service.rental;

import java.util.List;

public record RentalAddressParseResult(
        String name,
        String mobile,
        String address,
        String province,
        String city,
        String district,
        String street,
        String source,
        boolean fallback,
        List<String> warnings
) {
    public RentalAddressParseResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
