package cn.iocoder.yudao.module.rental.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerRemarkRentalPeriodParserTest {

    private final SellerRemarkRentalPeriodParser parser = new SellerRemarkRentalPeriodParser();

    @Test
    void shouldPreferAnExplicitRentalPeriod() {
        SellerRemarkRentalPeriod result = parser.parse("请按 #租期7.25-7.27# 安排", LocalDate.of(2026, 7, 20));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 7, 25), result.billableStartDate());
        assertEquals(LocalDate.of(2026, 7, 27), result.billableEndDate());
        assertEquals(SellerRemarkRentalPeriodParser.VERSION, result.version());
    }

    @Test
    void shouldUseReceiptNextDayAndReturnDayWhenNoExplicitPeriodExists() {
        SellerRemarkRentalPeriod result = parser.parse("收货 7.24，发回 7.27", LocalDate.of(2026, 7, 20));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 7, 25), result.billableStartDate());
        assertEquals(LocalDate.of(2026, 7, 27), result.billableEndDate());
    }

    @Test
    void shouldReturnAReviewReasonInsteadOfGuessing() {
        SellerRemarkRentalPeriod result = parser.parse("客户稍后确认时间", LocalDate.of(2026, 7, 20));

        assertFalse(result.isSuccess());
        assertEquals("RENTAL_PERIOD_NOT_FOUND", result.reasonCode());
    }

}
