package cn.iocoder.yudao.module.rental.service.rental;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100AddressResolutionClient;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsProviderConfigService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsProviderCredentialService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RentalAddressParseService {

    static final String PROVIDER_CODE = "KUAIDI100";
    private static final Pattern MOBILE_PATTERN = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern LEADING_SEPARATOR = Pattern.compile("^[\\s,，;；、:：-]+");

    private final RentalLogisticsProviderConfigService configService;
    private final RentalLogisticsProviderCredentialService credentialService;
    private final Kuaidi100AddressResolutionClient addressClient;

    public RentalAddressParseService(RentalLogisticsProviderConfigService configService,
                                     RentalLogisticsProviderCredentialService credentialService,
                                     Kuaidi100AddressResolutionClient addressClient) {
        this.configService = configService;
        this.credentialService = credentialService;
        this.addressClient = addressClient;
    }

    public RentalAddressParseResult parse(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        RentalAddressParseResult local = parseLocally(text);
        if (!configService.isAddressParseEnabled(PROVIDER_CODE)) {
            return withWarning(local, "PROVIDER_DISABLED_LOCAL_FALLBACK");
        }
        RentalLogisticsProviderCredentialDO credential = credentialService.resolveForAddressParse(PROVIDER_CODE);
        if (credential == null) {
            return withWarning(local, "PROVIDER_CREDENTIAL_MISSING_LOCAL_FALLBACK");
        }
        try {
            return merge(addressClient.resolve(text, credential), local);
        } catch (Exception ignored) {
            return withWarning(local, "PROVIDER_UNAVAILABLE_LOCAL_FALLBACK");
        }
    }

    RentalAddressParseResult parseLocally(String text) {
        Set<String> mobiles = new LinkedHashSet<>();
        Matcher matcher = MOBILE_PATTERN.matcher(text);
        while (matcher.find()) {
            mobiles.add(matcher.group());
        }
        List<String> warnings = new ArrayList<>();
        String mobile = mobiles.size() == 1 ? mobiles.iterator().next() : null;
        if (mobiles.size() > 1) {
            warnings.add("MULTIPLE_MOBILES_REVIEW");
        }

        String[] tokens = text.split("[，,;；\\n]", -1);
        String name = null;
        for (String token : tokens) {
            String candidate = token.trim();
            if (candidate.isEmpty() || MOBILE_PATTERN.matcher(candidate).find() || looksLikeAddress(candidate)) {
                continue;
            }
            name = candidate.length() <= 64 ? candidate : null;
            break;
        }

        String address = text;
        if (StringUtils.hasText(name) && address.startsWith(name)) {
            address = address.substring(name.length());
        }
        for (String foundMobile : mobiles) {
            address = address.replace(foundMobile, "");
        }
        address = LEADING_SEPARATOR.matcher(address).replaceFirst("").trim();
        address = normalizeMunicipalityPrefix(address);
        if (!looksLikeAddress(address)) {
            address = null;
        }
        return new RentalAddressParseResult(name, mobile, address, null, null, null, null,
                "LOCAL", true, warnings);
    }

    private RentalAddressParseResult merge(RentalAddressParseResult provider, RentalAddressParseResult local) {
        List<String> warnings = new ArrayList<>(provider.warnings());
        local.warnings().forEach(warning -> {
            if (!warnings.contains(warning)) {
                warnings.add(warning);
            }
        });
        return new RentalAddressParseResult(first(provider.name(), local.name()),
                first(provider.mobile(), local.mobile()), first(provider.address(), local.address()),
                provider.province(), provider.city(), provider.district(), provider.street(),
                provider.source(), false, warnings);
    }

    private RentalAddressParseResult withWarning(RentalAddressParseResult result, String warning) {
        List<String> warnings = new ArrayList<>(result.warnings());
        warnings.add(warning);
        return new RentalAddressParseResult(result.name(), result.mobile(), result.address(),
                result.province(), result.city(), result.district(), result.street(),
                result.source(), true, warnings);
    }

    private String first(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private String normalizeMunicipalityPrefix(String address) {
        if (!StringUtils.hasText(address)) {
            return address;
        }
        for (String municipality : List.of("北京", "上海", "天津", "重庆")) {
            address = address.replaceFirst("^" + municipality + municipality + "市", municipality + "市");
            address = address.replaceFirst("^" + municipality + "市" + municipality + "市", municipality + "市");
        }
        return address;
    }

    private boolean looksLikeAddress(String value) {
        return StringUtils.hasText(value) && (value.matches(".*(省|市|区|县|镇|乡|街道|路|街|道|巷|号|村|酒店|大厦|小区).*"));
    }
}
