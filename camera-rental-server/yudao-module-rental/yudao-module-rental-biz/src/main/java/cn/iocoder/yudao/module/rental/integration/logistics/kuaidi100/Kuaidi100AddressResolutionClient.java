package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import cn.iocoder.yudao.module.rental.service.rental.RentalAddressParseResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class Kuaidi100AddressResolutionClient {

    private final Kuaidi100Gateway gateway;
    private final Kuaidi100Signer signer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public Kuaidi100AddressResolutionClient(Kuaidi100Gateway gateway, Kuaidi100Signer signer,
                                             ObjectMapper objectMapper) {
        this(gateway, signer, objectMapper, Clock.systemUTC());
    }

    Kuaidi100AddressResolutionClient(Kuaidi100Gateway gateway, Kuaidi100Signer signer,
                                     ObjectMapper objectMapper, Clock clock) {
        this.gateway = gateway;
        this.signer = signer;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public RentalAddressParseResult resolve(String text, RentalLogisticsProviderCredentialDO credential)
            throws IOException {
        String param = objectMapper.writeValueAsString(Map.of("text", text));
        String timestamp = Long.toString(clock.millis());
        Map<String, String> form = new LinkedHashMap<>();
        form.put("key", credential.getApiKey());
        form.put("t", timestamp);
        form.put("param", param);
        form.put("sign", signer.signAddress(param, timestamp, credential.getApiKey(), credential.getApiSecret()));

        JsonNode root = objectMapper.readTree(gateway.resolveAddress(form));
        if (!"200".equals(root.path("code").asText())) {
            throw new IOException("KUAIDI100_ADDRESS_" + root.path("code").asText("UNKNOWN"));
        }
        JsonNode data = unwrapData(root);
        String name = text(data, "name");
        List<String> mobiles = stringList(data.get("mobile"));
        if (mobiles.isEmpty()) {
            mobiles = stringList(data.get("phone"));
        }
        List<String> warnings = new ArrayList<>();
        String mobile = mobiles.size() == 1 ? mobiles.get(0) : null;
        if (mobiles.size() > 1) {
            warnings.add("MULTIPLE_MOBILES_REVIEW");
        }
        String province = text(data, "province");
        String city = text(data, "city");
        String district = firstText(data, "district", "county");
        String street = firstText(data, "fourth", "street", "town");
        String detail = firstText(data, "address", "detail");
        return new RentalAddressParseResult(name, mobile,
                assembleAddress(province, city, district, street, detail),
                province, city, district, street, "KUAIDI100", false, warnings);
    }

    private JsonNode unwrapData(JsonNode root) {
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            return root;
        }
        JsonNode result = data.path("result");
        return result.isObject() ? result : data;
    }

    private String assembleAddress(String province, String city, String district, String street, String detail) {
        List<String> parts = new ArrayList<>();
        addDistinct(parts, normalizeProvince(province));
        if (!sameRegion(province, city)) {
            addDistinct(parts, city);
        }
        addDistinct(parts, district);
        addDistinct(parts, street);
        addDistinct(parts, detail);
        return parts.isEmpty() ? null : String.join("", parts);
    }

    private void addDistinct(List<String> parts, String value) {
        if (StringUtils.hasText(value) && (parts.isEmpty() || !parts.get(parts.size() - 1).equals(value.trim()))) {
            parts.add(value.trim());
        }
    }

    private String normalizeProvince(String province) {
        if (!StringUtils.hasText(province)) {
            return null;
        }
        String value = province.trim();
        if (List.of("北京", "上海", "天津", "重庆").contains(value)) {
            return value + "市";
        }
        return value;
    }

    private boolean sameRegion(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return stripRegionSuffix(left).equals(stripRegionSuffix(right));
    }

    private String stripRegionSuffix(String value) {
        return value.trim().replaceFirst("(特别行政区|自治区|省|市)$", "");
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String value = text(node, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node == null ? null : node.get(name);
        return value != null && value.isValueNode() && StringUtils.hasText(value.asText())
                ? value.asText().trim() : null;
    }

    private List<String> stringList(JsonNode value) {
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (value.isArray()) {
            List<String> result = new ArrayList<>();
            value.forEach(item -> {
                if (item.isValueNode() && StringUtils.hasText(item.asText())) {
                    result.add(item.asText().trim());
                }
            });
            return result;
        }
        return StringUtils.hasText(value.asText()) ? List.of(value.asText().trim()) : List.of();
    }
}
