package cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalTrackingStatusEnum;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsOperationResult;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingEvent;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class Kuaidi100Converter {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    private final ObjectMapper objectMapper;
    private final Kuaidi100StatusMapper statusMapper;
    private final Clock clock;

    public Kuaidi100Converter(ObjectMapper objectMapper, Kuaidi100StatusMapper statusMapper) {
        this(objectMapper, statusMapper, Clock.system(BUSINESS_ZONE));
    }

    Kuaidi100Converter(ObjectMapper objectMapper, Kuaidi100StatusMapper statusMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.statusMapper = statusMapper;
        this.clock = clock;
    }

    public LogisticsOperationResult parseSubscribe(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            boolean success = root.path("result").asBoolean(false)
                    || "200".equals(root.path("returnCode").asText());
            if (success) {
                return LogisticsOperationResult.success(text(root, "taskId"), null);
            }
            String code = safeCode(root, "KUAIDI100_SUBSCRIBE_REJECTED");
            return LogisticsOperationResult.failure(code, isRetryable(code));
        } catch (Exception exception) {
            return LogisticsOperationResult.failure("KUAIDI100_RESPONSE_INVALID", false);
        }
    }

    public LogisticsOperationResult parseQuery(String raw, String source, Long inboxId) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode result = root.has("lastResult") ? root.path("lastResult") : root;
            String status = result.path("status").asText();
            if (StringUtils.hasText(status) && !"200".equals(status)) {
                String code = safeCode(result, "KUAIDI100_QUERY_REJECTED");
                return LogisticsOperationResult.failure(code, isRetryable(code));
            }
            return LogisticsOperationResult.success(text(root, "taskId"),
                    toSnapshot(result, source, inboxId));
        } catch (Exception exception) {
            return LogisticsOperationResult.failure("KUAIDI100_RESPONSE_INVALID", false);
        }
    }

    private LogisticsTrackingSnapshot toSnapshot(JsonNode result, String source, Long inboxId) {
        String overallState = text(result, "state");
        List<LogisticsTrackingEvent> events = new ArrayList<>();
        JsonNode data = result.path("data");
        if (data.isArray()) {
            for (JsonNode item : data) {
                String rawTime = firstText(item, "ftime", "time");
                String providerStatus = firstText(item, "status", "state");
                if (!StringUtils.hasText(providerStatus)) {
                    providerStatus = overallState;
                }
                RentalTrackingStatusEnum status = statusMapper.map(providerStatus);
                events.add(new LogisticsTrackingEvent(parseTime(rawTime), rawTime, status, providerStatus,
                        firstText(item, "context", "message"), text(item, "location"), source, inboxId));
            }
        }
        return new LogisticsTrackingSnapshot(events, parseTime(text(result, "predictTime")),
                LocalDateTime.now(clock.withZone(BUSINESS_ZONE)));
    }

    private LocalDateTime parseTime(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(raw.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next documented timestamp representation.
            }
        }
        return null;
    }

    private String safeCode(JsonNode root, String fallback) {
        String raw = firstText(root, "returnCode", "status", "message");
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        String normalized = raw.replaceAll("[^A-Za-z0-9_-]", "_");
        return "KUAIDI100_" + normalized.substring(0, Math.min(48, normalized.length())).toUpperCase();
    }

    private boolean isRetryable(String code) {
        return code.endsWith("_408") || code.endsWith("_429") || code.endsWith("_500")
                || code.endsWith("_503") || code.contains("TIMEOUT") || code.contains("SYSTEM");
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
