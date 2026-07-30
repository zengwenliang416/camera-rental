package cn.iocoder.yudao.module.rental.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses explicit rental periods and deterministic logistics-date templates.
 * Incomplete or ambiguous remarks retain known dates but remain pending for operator review.
 */
@Component
public class SellerRemarkRentalPeriodParser {

    public static final String VERSION = "SELLER_REMARK_V6";

    private static final String DATE = "(?:(20\\d{2})\\s*[年./-]\\s*)?"
            + "(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})\\s*[日号]?";
    private static final String PERIOD_LABEL = "(?:租期|计租|用机|使用(?:日期|时间)?|租用(?:日期|时间)?)";
    private static final String RANGE_SEPARATOR = "(?:-|—|–|~|～|至|到)";
    private static final Pattern EXPLICIT_PERIOD = Pattern.compile(
            "(?:#\\s*)?" + PERIOD_LABEL + "\\s*[:：为是#]?\\s*" + DATE
                    + "\\s*" + RANGE_SEPARATOR + "\\s*" + DATE + "\\s*#?");
    private static final Pattern EXPLICIT_SAME_MONTH_PERIOD = Pattern.compile(
            "(?:#\\s*)?" + PERIOD_LABEL + "\\s*[:：为是#]?\\s*"
                    + "(?:(20\\d{2})\\s*[年./-]\\s*)?(\\d{1,2})\\s*[月./-]\\s*"
                    + "(\\d{1,2})\\s*[日号]?\\s*" + RANGE_SEPARATOR
                    + "\\s*(\\d{1,2})\\s*[日号]?\\s*#?");
    private static final Pattern DATE_TOKEN = Pattern.compile(DATE);
    private static final Pattern SHIP_DATE = labeledDatePattern("发货|出库|寄出|发出");
    private static final Pattern RECEIVED_DATE = labeledDatePattern("收货|收到货|到货|签收");
    private static final Pattern RETURN_DATE = labeledDatePattern("发回|寄回|回寄|归还|退回");
    private static final Pattern SELF_PICKUP_DATE = Pattern.compile(
            "(?:(?:发货|出库|取机)\\s*[:：#]?\\s*)?" + DATE
                    + "\\s*(?:上午|中午|下午|晚上)?\\s*"
                    + "(?:[\\u4E00-\\u9FFF·]{1,16}\\s*)?自提");

    public SellerRemarkRentalPeriod parse(String sellerRemark, LocalDate referenceDate) {
        if (sellerRemark == null || sellerRemark.isBlank()) {
            return SellerRemarkRentalPeriod.pending(VERSION, "MISSING_REMARK");
        }
        if (referenceDate == null) {
            return SellerRemarkRentalPeriod.pending(VERSION, "MISSING_ORDER_DATE");
        }

        String normalizedRemark = normalize(sellerRemark);
        PeriodMatch explicitPeriod = extractExplicitPeriod(normalizedRemark, referenceDate);
        List<DatePoint> shipDates = extractDates(SHIP_DATE, normalizedRemark, referenceDate);
        List<DatePoint> receiveDates = extractDates(RECEIVED_DATE, normalizedRemark, referenceDate);
        List<DatePoint> returnDates = extractDates(RETURN_DATE, normalizedRemark, referenceDate);
        List<DatePoint> selfPickupDates = extractDates(SELF_PICKUP_DATE, normalizedRemark, referenceDate);
        LogisticsDates logistics = inferLogisticsDates(normalizedRemark, referenceDate, explicitPeriod,
                shipDates, receiveDates, returnDates, selfPickupDates);
        LocalDate shipDate = date(logistics.ship());
        LocalDate receiveDate = date(logistics.receive());
        LocalDate returnDate = date(logistics.returned());
        if (isInvalidLogisticsRange(shipDate, receiveDate, returnDate)) {
            return SellerRemarkRentalPeriod.failure(VERSION, "INVALID_LOGISTICS_RANGE");
        }
        if (isBeforeReferenceDate(referenceDate, shipDate, receiveDate, returnDate)) {
            return SellerRemarkRentalPeriod.failure(VERSION, "LOGISTICS_DATE_BEFORE_ORDER");
        }

        if (explicitPeriod != null) {
            if (explicitPeriod.start() == null || explicitPeriod.end() == null) {
                return SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_DATE");
            }
            if (explicitPeriod.end().isBefore(explicitPeriod.start())) {
                return SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_RANGE");
            }
            return finishPeriod(explicitPeriod.start(), explicitPeriod.end(),
                    shipDate, receiveDate, returnDate);
        }

        if (receiveDate == null) {
            String reason = shipDate == null && returnDate == null
                    ? "RENTAL_PERIOD_NOT_FOUND" : "MISSING_RECEIVE_DATE";
            return pending(null, null, shipDate, null, returnDate, reason);
        }
        if (returnDate == null) {
            return pending(null, null, shipDate, receiveDate, null, "MISSING_RETURN_DATE");
        }
        LocalDate billableStart = receiveDate.plusDays(1);
        if (returnDate.isBefore(billableStart)) {
            return SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_RANGE");
        }
        return finishPeriod(billableStart, returnDate, shipDate, receiveDate, returnDate);
    }

    private SellerRemarkRentalPeriod finishPeriod(LocalDate billableStart, LocalDate billableEnd,
                                                   LocalDate shipDate, LocalDate receiveDate,
                                                   LocalDate returnDate) {
        if (shipDate == null) {
            return pending(billableStart, billableEnd, null, receiveDate, returnDate, "MISSING_SHIP_DATE");
        }
        if (returnDate == null) {
            return pending(billableStart, billableEnd, shipDate, receiveDate, null, "MISSING_RETURN_DATE");
        }
        return SellerRemarkRentalPeriod.success(
                VERSION, billableStart, billableEnd, shipDate, receiveDate, returnDate);
    }

    private SellerRemarkRentalPeriod pending(LocalDate billableStart, LocalDate billableEnd,
                                              LocalDate shipDate, LocalDate receiveDate,
                                              LocalDate returnDate, String reasonCode) {
        return SellerRemarkRentalPeriod.pending(VERSION, billableStart, billableEnd,
                shipDate, receiveDate, returnDate, reasonCode);
    }

    private LogisticsDates inferLogisticsDates(String remark, LocalDate referenceDate,
                                               PeriodMatch explicitPeriod,
                                               List<DatePoint> shipDates,
                                               List<DatePoint> receiveDates,
                                               List<DatePoint> returnDates,
                                               List<DatePoint> selfPickupDates) {
        DatePoint shipDate = first(shipDates);
        DatePoint receiveDate = first(receiveDates);
        DatePoint returnDate = first(returnDates);
        DatePoint selfPickupDate = first(selfPickupDates);
        if (shipDate == null) {
            shipDate = selfPickupDate;
        }
        if (receiveDate == null) {
            receiveDate = selfPickupDate;
        }
        if (returnDate == null && receiveDate != null) {
            DatePoint resolvedReceiveDate = receiveDate;
            returnDate = shipDates.stream()
                    .filter(point -> point.start() > resolvedReceiveDate.end())
                    .findFirst().orElse(null);
        }

        Set<Integer> claimed = new HashSet<>();
        shipDates.forEach(point -> claimed.add(point.start()));
        receiveDates.forEach(point -> claimed.add(point.start()));
        returnDates.forEach(point -> claimed.add(point.start()));
        selfPickupDates.forEach(point -> claimed.add(point.start()));
        List<DatePoint> freeDates = extractDates(DATE_TOKEN, remark, referenceDate).stream()
                .filter(point -> !claimed.contains(point.start()))
                .filter(point -> explicitPeriod == null
                        || point.start() < explicitPeriod.startOffset()
                        || point.start() >= explicitPeriod.endOffset())
                .toList();

        if (shipDate != null && receiveDate == null) {
            List<DatePoint> candidates = new ArrayList<>();
            for (DatePoint point : freeDates) {
                if (point.start() > shipDate.end()
                        && (returnDate == null || point.end() < returnDate.start())) {
                    candidates.add(point);
                }
            }
            if (returnDate == null && candidates.size() == 2) {
                receiveDate = candidates.get(0);
                returnDate = candidates.get(1);
            } else if (candidates.size() == 1) {
                receiveDate = candidates.get(0);
            }
        }
        if (receiveDate != null && returnDate == null) {
            List<DatePoint> candidates = new ArrayList<>();
            for (DatePoint point : freeDates) {
                if (point.start() > receiveDate.end()) {
                    candidates.add(point);
                }
            }
            if (candidates.size() == 1) {
                returnDate = candidates.get(0);
            }
        }
        return new LogisticsDates(shipDate, receiveDate, returnDate);
    }

    private PeriodMatch extractExplicitPeriod(String remark, LocalDate referenceDate) {
        Matcher matcher = EXPLICIT_PERIOD.matcher(remark);
        if (matcher.find()) {
            return new PeriodMatch(
                    inferDate(referenceDate, matcher.group(1), matcher.group(2), matcher.group(3)),
                    inferDate(referenceDate, matcher.group(4), matcher.group(5), matcher.group(6)),
                    matcher.start(), matcher.end());
        }
        Matcher sameMonth = EXPLICIT_SAME_MONTH_PERIOD.matcher(remark);
        if (!sameMonth.find()) {
            return null;
        }
        LocalDate start = inferDate(referenceDate,
                sameMonth.group(1), sameMonth.group(2), sameMonth.group(3));
        LocalDate end = start == null ? null : safeDate(
                start.getYear(), start.getMonthValue(), sameMonth.group(4));
        return new PeriodMatch(start, end, sameMonth.start(), sameMonth.end());
    }

    private List<DatePoint> extractDates(Pattern pattern, String remark, LocalDate referenceDate) {
        List<DatePoint> dates = new ArrayList<>();
        Matcher matcher = pattern.matcher(remark);
        while (matcher.find()) {
            LocalDate date = inferDate(referenceDate, matcher.group(1), matcher.group(2), matcher.group(3));
            int start = matcher.group(1) != null ? matcher.start(1) : matcher.start(2);
            dates.add(new DatePoint(date, start, matcher.end(3)));
        }
        return dates;
    }

    private boolean isBeforeReferenceDate(LocalDate referenceDate, LocalDate... dates) {
        for (LocalDate date : dates) {
            if (date != null && date.isBefore(referenceDate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInvalidLogisticsRange(LocalDate shipDate, LocalDate receiveDate, LocalDate returnDate) {
        return shipDate != null && receiveDate != null && receiveDate.isBefore(shipDate)
                || receiveDate != null && returnDate != null && returnDate.isBefore(receiveDate)
                || shipDate != null && returnDate != null && returnDate.isBefore(shipDate);
    }

    private LocalDate inferDate(LocalDate referenceDate, String yearText,
                                String monthText, String dayText) {
        try {
            if (yearText != null) {
                return LocalDate.of(Integer.parseInt(yearText),
                        Integer.parseInt(monthText), Integer.parseInt(dayText));
            }
            if (referenceDate == null) {
                return null;
            }
            LocalDate candidate = LocalDate.of(referenceDate.getYear(),
                    Integer.parseInt(monthText), Integer.parseInt(dayText));
            if (candidate.isAfter(referenceDate.plusMonths(6))) {
                return candidate.minusYears(1);
            }
            if (candidate.isBefore(referenceDate.minusMonths(6))) {
                return candidate.plusYears(1);
            }
            return candidate;
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private LocalDate safeDate(int year, int month, String dayText) {
        try {
            return LocalDate.of(year, month, Integer.parseInt(dayText));
        } catch (DateTimeException | NumberFormatException exception) {
            return null;
        }
    }

    private static Pattern labeledDatePattern(String labels) {
        return Pattern.compile("(?:" + labels + ")\\s*[:：#]?\\s*" + DATE);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('，', ',')
                .replace('；', ';');
    }

    private static DatePoint first(List<DatePoint> values) {
        return values.isEmpty() ? null : values.get(0);
    }

    private static LocalDate date(DatePoint point) {
        return point == null ? null : point.date();
    }

    private record DatePoint(LocalDate date, int start, int end) {
    }

    private record LogisticsDates(DatePoint ship, DatePoint receive, DatePoint returned) {
    }

    private record PeriodMatch(LocalDate start, LocalDate end, int startOffset, int endOffset) {
    }

}
