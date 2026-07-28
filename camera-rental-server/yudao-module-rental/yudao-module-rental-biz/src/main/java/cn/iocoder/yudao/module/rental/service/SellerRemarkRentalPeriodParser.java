package cn.iocoder.yudao.module.rental.service;

import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses only documented rental-date conventions and keeps incomplete remarks pending for later order refreshes.
 */
@Component
public class SellerRemarkRentalPeriodParser {

    public static final String VERSION = "SELLER_REMARK_V3";

    private static final Pattern EXPLICIT_PERIOD = Pattern.compile(
            "#\\s*租期\\s*(\\d{1,2})[./月](\\d{1,2})(?:日)?\\s*[-~至到]+\\s*(\\d{1,2})[./月](\\d{1,2})(?:日)?\\s*#");
    private static final Pattern SHIP_DATE = Pattern.compile(
            "发货\\s*[:：#]?\\s*(\\d{1,2})[./月](\\d{1,2})(?:日)?");
    private static final Pattern RECEIVED_DATE = Pattern.compile(
            "(?:收货|收到货)\\s*[:：#]?\\s*(\\d{1,2})[./月](\\d{1,2})(?:日)?");
    private static final Pattern RETURN_DATE = Pattern.compile(
            "(?:发回|寄回)\\s*[:：#]?\\s*(\\d{1,2})[./月](\\d{1,2})(?:日)?");

    public SellerRemarkRentalPeriod parse(String sellerRemark, LocalDate referenceDate) {
        if (sellerRemark == null || sellerRemark.isBlank()) {
            return SellerRemarkRentalPeriod.pending(VERSION, "MISSING_REMARK");
        }
        if (referenceDate == null) {
            return SellerRemarkRentalPeriod.pending(VERSION, "MISSING_ORDER_DATE");
        }

        LocalDate shipDate = extractDate(SHIP_DATE, sellerRemark, referenceDate);
        LocalDate receiveDate = extractDate(RECEIVED_DATE, sellerRemark, referenceDate);
        LocalDate returnDate = extractDate(RETURN_DATE, sellerRemark, referenceDate);
        if (isInvalidLogisticsRange(shipDate, receiveDate, returnDate)) {
            return SellerRemarkRentalPeriod.failure(VERSION, "INVALID_LOGISTICS_RANGE");
        }
        if (isBeforeReferenceDate(referenceDate, shipDate, receiveDate, returnDate)) {
            return SellerRemarkRentalPeriod.failure(VERSION, "LOGISTICS_DATE_BEFORE_ORDER");
        }

        Matcher explicit = EXPLICIT_PERIOD.matcher(sellerRemark);
        if (explicit.find()) {
            return createPeriod(referenceDate, explicit.group(1), explicit.group(2),
                    explicit.group(3), explicit.group(4), shipDate, receiveDate, returnDate);
        }

        if (receiveDate == null || returnDate == null) {
            return SellerRemarkRentalPeriod.pending(VERSION, "RENTAL_PERIOD_NOT_FOUND");
        }
        LocalDate billableStart = receiveDate.plusDays(1);
        return !returnDate.isBefore(billableStart)
                ? SellerRemarkRentalPeriod.success(
                        VERSION, billableStart, returnDate, shipDate, receiveDate, returnDate)
                : SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_RANGE");
    }

    private SellerRemarkRentalPeriod createPeriod(LocalDate referenceDate, String startMonth, String startDay,
                                                   String endMonth, String endDay, LocalDate shipDate,
                                                   LocalDate receiveDate, LocalDate returnDate) {
        LocalDate start = inferDate(referenceDate, startMonth, startDay);
        LocalDate end = inferDate(referenceDate, endMonth, endDay);
        if (start == null || end == null) {
            return SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_DATE");
        }
        return !end.isBefore(start)
                ? SellerRemarkRentalPeriod.success(
                        VERSION, start, end, shipDate, receiveDate, returnDate)
                : SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_RANGE");
    }

    private LocalDate extractDate(Pattern pattern, String sellerRemark, LocalDate referenceDate) {
        Matcher matcher = pattern.matcher(sellerRemark);
        return matcher.find() ? inferDate(referenceDate, matcher.group(1), matcher.group(2)) : null;
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

    private LocalDate inferDate(LocalDate referenceDate, String monthText, String dayText) {
        try {
            LocalDate candidate = LocalDate.of(referenceDate.getYear(), Integer.parseInt(monthText), Integer.parseInt(dayText));
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

}
