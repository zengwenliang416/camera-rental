package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RentalRemarkPlanChangeClassifier {

    private static final Pattern SPECIAL_CASE_SUFFIX = Pattern.compile(
            "(?:^|[/|,，;；#\\s])(续租|早退|改期|换机|损坏|遗失|逾期|物流延误)\\s*$");

    public RentalRemarkPlanChangeType classify(String sellerRemark,
                                               SellerRemarkRentalPeriod previousEffectivePlan,
                                               SellerRemarkRentalPeriod candidate) {
        if (candidate == null || !candidate.isSuccess()) {
            return RentalRemarkPlanChangeType.INVALID;
        }
        List<RentalRemarkPlanChangeType> suffixTypes = suffixTypes(sellerRemark);
        if (suffixTypes.size() > 1) {
            return RentalRemarkPlanChangeType.AMBIGUOUS;
        }
        RentalRemarkPlanChangeType dateChange = classifyDateChange(previousEffectivePlan, candidate);
        if (!suffixTypes.isEmpty()) {
            RentalRemarkPlanChangeType suffixType = suffixTypes.get(0);
            if (isPlanChange(suffixType)) {
                if (previousEffectivePlan == null || suffixType != dateChange) {
                    return RentalRemarkPlanChangeType.AMBIGUOUS;
                }
            }
            return suffixType;
        }
        return dateChange;
    }

    private static RentalRemarkPlanChangeType classifyDateChange(
            SellerRemarkRentalPeriod previousEffectivePlan,
            SellerRemarkRentalPeriod candidate) {
        if (previousEffectivePlan == null) {
            return RentalRemarkPlanChangeType.INITIAL;
        }
        if (samePlan(previousEffectivePlan, candidate)) {
            return RentalRemarkPlanChangeType.UNCHANGED;
        }
        if (sameStartFacts(previousEffectivePlan, candidate)
                && movedLater(previousEffectivePlan.billableEndDate(), candidate.billableEndDate())
                && movedLater(previousEffectivePlan.returnDate(), candidate.returnDate())) {
            return RentalRemarkPlanChangeType.EXTENSION;
        }
        if (sameStartFacts(previousEffectivePlan, candidate)
                && movedEarlier(previousEffectivePlan.billableEndDate(), candidate.billableEndDate())
                && movedEarlier(previousEffectivePlan.returnDate(), candidate.returnDate())) {
            return RentalRemarkPlanChangeType.EARLY_RETURN;
        }
        return RentalRemarkPlanChangeType.RESCHEDULE;
    }

    private static boolean isPlanChange(RentalRemarkPlanChangeType type) {
        return type == RentalRemarkPlanChangeType.EXTENSION
                || type == RentalRemarkPlanChangeType.EARLY_RETURN
                || type == RentalRemarkPlanChangeType.RESCHEDULE;
    }

    private static List<RentalRemarkPlanChangeType> suffixTypes(String sellerRemark) {
        if (sellerRemark == null || sellerRemark.isBlank()) {
            return List.of();
        }
        String normalized = Normalizer.normalize(sellerRemark, Normalizer.Form.NFKC).trim();
        List<RentalRemarkPlanChangeType> result = new ArrayList<>();
        String remaining = normalized;
        while (!remaining.isBlank()) {
            Matcher matcher = SPECIAL_CASE_SUFFIX.matcher(remaining);
            if (!matcher.find()) {
                break;
            }
            result.add(toType(matcher.group(1)));
            remaining = remaining.substring(0, matcher.start()).trim();
        }
        return result;
    }

    private static RentalRemarkPlanChangeType toType(String keyword) {
        return switch (keyword) {
            case "续租" -> RentalRemarkPlanChangeType.EXTENSION;
            case "早退" -> RentalRemarkPlanChangeType.EARLY_RETURN;
            case "改期" -> RentalRemarkPlanChangeType.RESCHEDULE;
            case "换机" -> RentalRemarkPlanChangeType.REPLACEMENT;
            case "损坏" -> RentalRemarkPlanChangeType.DAMAGE;
            case "遗失" -> RentalRemarkPlanChangeType.LOSS;
            case "逾期" -> RentalRemarkPlanChangeType.OVERDUE;
            case "物流延误" -> RentalRemarkPlanChangeType.LOGISTICS_DELAY;
            default -> throw new IllegalArgumentException("Unsupported seller remark suffix: " + keyword);
        };
    }

    private static boolean samePlan(SellerRemarkRentalPeriod previous, SellerRemarkRentalPeriod candidate) {
        return Objects.equals(previous.billableStartDate(), candidate.billableStartDate())
                && Objects.equals(previous.billableEndDate(), candidate.billableEndDate())
                && Objects.equals(previous.shipDate(), candidate.shipDate())
                && Objects.equals(previous.receiveDate(), candidate.receiveDate())
                && Objects.equals(previous.returnDate(), candidate.returnDate());
    }

    private static boolean sameStartFacts(SellerRemarkRentalPeriod previous,
                                          SellerRemarkRentalPeriod candidate) {
        return Objects.equals(previous.billableStartDate(), candidate.billableStartDate())
                && Objects.equals(previous.shipDate(), candidate.shipDate())
                && Objects.equals(previous.receiveDate(), candidate.receiveDate());
    }

    private static boolean movedLater(LocalDate previous, LocalDate candidate) {
        return previous != null && candidate != null && candidate.isAfter(previous);
    }

    private static boolean movedEarlier(LocalDate previous, LocalDate candidate) {
        return previous != null && candidate != null && candidate.isBefore(previous);
    }

}
