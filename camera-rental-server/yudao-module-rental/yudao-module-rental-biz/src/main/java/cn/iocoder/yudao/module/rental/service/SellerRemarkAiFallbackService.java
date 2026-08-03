package cn.iocoder.yudao.module.rental.service;

import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.ai.util.AiUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SellerRemarkAiFallbackService {

    static final BigDecimal MIN_CONFIDENCE = new BigDecimal("0.9800");
    private static final int MAX_REMARK_LENGTH = 2_000;
    private static final String SYSTEM_PROMPT = """
            You extract rental dates from a Chinese seller remark. Never guess missing dates.
            Return one JSON object only, without Markdown:
            {"sourceMode":"EXPLICIT_PERIOD|LOGISTICS_DERIVED|INSUFFICIENT",
             "shipDate":"yyyy-MM-dd|null","receiveDate":"yyyy-MM-dd|null",
             "returnDate":"yyyy-MM-dd|null","billableStartDate":"yyyy-MM-dd|null",
             "billableEndDate":"yyyy-MM-dd|null","confidence":0.0,
             "ambiguous":true,"evidenceFields":["shipDate","receiveDate","returnDate"]}
            Rules: an explicit rental period overrides derived billing dates. Otherwise billing starts
            the day after receipt and includes the return day. A repeated later shipping date may mean
            customer return only when context makes that explicit. Missing years must be inferred from
            the supplied order date within six months. Use INSUFFICIENT when any required fact conflicts
            or is not supported. Do not include customer text, names, phones, addresses or explanations.
            """;

    private final AiModelService aiModelService;
    private final ObjectMapper objectMapper;

    public SellerRemarkAiFallbackService(AiModelService aiModelService, ObjectMapper objectMapper) {
        this.aiModelService = aiModelService;
        this.objectMapper = objectMapper;
    }

    public Optional<SellerRemarkResolution> resolve(String sellerRemark, LocalDate referenceDate) {
        try {
            AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            ChatModel chatModel = aiModelService.getChatModel(model.getId());
            AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
            ChatOptions options = AiUtils.buildChatOptions(
                    platform, model.getModel(), model.getTemperature(), model.getMaxTokens());
            String input = objectMapper.writeValueAsString(new Request(
                    referenceDate, abbreviate(sellerRemark)));
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(input)), options));
            return validateDecision(AiUtils.getChatResponseContent(response), referenceDate, model);
        } catch (Exception ex) {
            log.warn("[seller-remark-ai] fallback unavailable code={}",
                    ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    Optional<SellerRemarkResolution> validateDecision(String rawContent, LocalDate referenceDate,
                                                       AiModelDO model) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(rawContent));
            String sourceMode = text(root, "sourceMode");
            if ("INSUFFICIENT".equals(sourceMode) || root.path("ambiguous").asBoolean(true)) {
                return Optional.empty();
            }
            BigDecimal confidence = root.path("confidence").decimalValue()
                    .setScale(4, RoundingMode.HALF_UP);
            if (confidence.compareTo(MIN_CONFIDENCE) < 0) {
                return Optional.empty();
            }
            LocalDate shipDate = date(root, "shipDate");
            LocalDate receiveDate = date(root, "receiveDate");
            LocalDate returnDate = date(root, "returnDate");
            LocalDate billableStartDate = date(root, "billableStartDate");
            LocalDate billableEndDate = date(root, "billableEndDate");
            if (!hasRequiredEvidence(root) || !validLogistics(referenceDate, shipDate, receiveDate, returnDate)) {
                return Optional.empty();
            }
            if ("LOGISTICS_DERIVED".equals(sourceMode)) {
                billableStartDate = receiveDate.plusDays(1);
                billableEndDate = returnDate;
            } else if (!"EXPLICIT_PERIOD".equals(sourceMode)) {
                return Optional.empty();
            }
            if (!validBillable(receiveDate, returnDate, billableStartDate, billableEndDate)) {
                return Optional.empty();
            }
            SellerRemarkRentalPeriod period = SellerRemarkRentalPeriod.success(
                    SellerRemarkRentalPeriodResolver.VERSION, billableStartDate, billableEndDate,
                    shipDate, receiveDate, returnDate);
            String evidenceJson = objectMapper.writeValueAsString(root.path("evidenceFields"));
            return Optional.of(new SellerRemarkResolution(
                    period, "AI", confidence, model.getModel(), evidenceJson));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static boolean validLogistics(LocalDate referenceDate, LocalDate shipDate,
                                          LocalDate receiveDate, LocalDate returnDate) {
        return shipDate != null && receiveDate != null && returnDate != null
                && !shipDate.isBefore(referenceDate)
                && !receiveDate.isBefore(shipDate)
                && !returnDate.isBefore(receiveDate)
                && !returnDate.isAfter(referenceDate.plusYears(1));
    }

    private static boolean validBillable(LocalDate receiveDate, LocalDate returnDate,
                                         LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null
                && !startDate.isBefore(receiveDate)
                && !endDate.isBefore(startDate)
                && !endDate.isAfter(returnDate);
    }

    private static boolean hasRequiredEvidence(JsonNode root) {
        JsonNode fields = root.path("evidenceFields");
        return fields.isArray()
                && contains(fields, "shipDate")
                && contains(fields, "receiveDate")
                && contains(fields, "returnDate");
    }

    private static boolean contains(JsonNode array, String value) {
        for (JsonNode element : array) {
            if (value.equals(element.asText())) {
                return true;
            }
        }
        return false;
    }

    private static LocalDate date(JsonNode root, String field) {
        String value = text(root, field);
        if (value == null || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException ex) {
            return null;
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String stripCodeFence(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        return firstLineEnd < 0 || lastFence <= firstLineEnd
                ? trimmed : trimmed.substring(firstLineEnd + 1, lastFence).trim();
    }

    private static String abbreviate(String value) {
        return value.length() <= MAX_REMARK_LENGTH ? value : value.substring(0, MAX_REMARK_LENGTH);
    }

    private record Request(LocalDate orderDate, String sellerRemark) {
    }

}
