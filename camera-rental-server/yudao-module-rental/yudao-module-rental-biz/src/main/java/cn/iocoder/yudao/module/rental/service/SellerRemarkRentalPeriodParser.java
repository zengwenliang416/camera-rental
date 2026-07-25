package cn.iocoder.yudao.module.rental.service;

import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses only documented rental-date conventions and returns a review reason for everything else.
 */
@Component
public class SellerRemarkRentalPeriodParser {

    public static final String VERSION = "SELLER_REMARK_V1";

    private static final Pattern EXPLICIT_PERIOD = Pattern.compile(
            "#\\s*租期\\s*(\\d{1,2})[./月](\\d{1,2})(?:日)?\\s*[-~至到]+\\s*(\\d{1,2})[./月](\\d{1,2})(?:日)?\\s*#");
    private static final Pattern RECEIVED_DATE = Pattern.compile(
            "(?:收货|收到货)\\s*[:：#]?\\s*(\\d{1,2})[./月](\\d{1,2})(?:日)?");
    private static final Pattern RETURN_DATE = Pattern.compile(
            "(?:发回|寄回)\\s*[:：#]?\\s*(\\d{1,2})[./月](\\d{1,2})(?:日)?");

    public SellerRemarkRentalPeriod parse(String sellerRemark, LocalDate referenceDate) {
        if (sellerRemark == null || sellerRemark.isBlank()) {
            return SellerRemarkRentalPeriod.failure(VERSION, "MISSING_REMARK");
        }
        if (referenceDate == null) {
            return SellerRemarkRentalPeriod.failure(VERSION, "MISSING_ORDER_DATE");
        }

        Matcher explicit = EXPLICIT_PERIOD.matcher(sellerRemark);
        if (explicit.find()) {
            return createPeriod(referenceDate, explicit.group(1), explicit.group(2), explicit.group(3), explicit.group(4));
        }

        Matcher received = RECEIVED_DATE.matcher(sellerRemark);
        Matcher returned = RETURN_DATE.matcher(sellerRemark);
        if (!received.find() || !returned.find()) {
            return SellerRemarkRentalPeriod.failure(VERSION, "RENTAL_PERIOD_NOT_FOUND");
        }
        LocalDate receivedDate = inferDate(referenceDate, received.group(1), received.group(2));
        LocalDate returnDate = inferDate(referenceDate, returned.group(1), returned.group(2));
        if (receivedDate == null || returnDate == null) {
            return SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_DATE");
        }
        if (returnDate.isBefore(receivedDate)) {
            returnDate = returnDate.plusYears(1);
        }
        LocalDate billableStart = receivedDate.plusDays(1);
        return !returnDate.isBefore(billableStart)
                ? SellerRemarkRentalPeriod.success(VERSION, billableStart, returnDate)
                : SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_RANGE");
    }

    private SellerRemarkRentalPeriod createPeriod(LocalDate referenceDate, String startMonth, String startDay,
                                                   String endMonth, String endDay) {
        LocalDate start = inferDate(referenceDate, startMonth, startDay);
        LocalDate end = inferDate(referenceDate, endMonth, endDay);
        if (start == null || end == null) {
            return SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_DATE");
        }
        if (end.isBefore(start)) {
            end = end.plusYears(1);
        }
        return !end.isBefore(start)
                ? SellerRemarkRentalPeriod.success(VERSION, start, end)
                : SellerRemarkRentalPeriod.failure(VERSION, "INVALID_RENTAL_RANGE");
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
