package cn.iocoder.yudao.module.rental.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerRemarkRentalPeriodParserTest {

    private final SellerRemarkRentalPeriodParser parser = new SellerRemarkRentalPeriodParser();

    @Test
    void shouldUseVersionSixForValidatedLogisticsDates() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货7.28/收货7.29/发回8.05", LocalDate.of(2026, 7, 28));

        assertEquals("SELLER_REMARK_V6", result.version());
    }

    @Test
    void shouldPreferAnExplicitRentalPeriod() {
        SellerRemarkRentalPeriod result = parser.parse("请按 #租期7.25-7.27# 安排", LocalDate.of(2026, 7, 20));

        assertTrue(result.isPending());
        assertEquals(LocalDate.of(2026, 7, 25), result.billableStartDate());
        assertEquals(LocalDate.of(2026, 7, 27), result.billableEndDate());
        assertEquals("MISSING_SHIP_DATE", result.reasonCode());
        assertEquals(SellerRemarkRentalPeriodParser.VERSION, result.version());
    }

    @Test
    void shouldParseFullYearChineseExplicitPeriodWithoutHashes() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货2026年8月1日，收货2026年8月2日，发回2026年8月8日，计租：2026年8月3日至8月8日",
                LocalDate.of(2026, 7, 30));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 8, 3), result.billableStartDate());
        assertEquals(LocalDate.of(2026, 8, 8), result.billableEndDate());
    }

    @Test
    void shouldParseSameMonthCompactPeriod() {
        SellerRemarkRentalPeriod result = parser.parse(
                "出库8.1 到货8.2 归还8.8 用机8月3日至8日",
                LocalDate.of(2026, 7, 30));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 8, 3), result.billableStartDate());
        assertEquals(LocalDate.of(2026, 8, 8), result.billableEndDate());
    }

    @Test
    void shouldUseReceiptNextDayAndReturnDayWhenNoExplicitPeriodExists() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货 7.22，收货 7.24，发回 7.27", LocalDate.of(2026, 7, 20));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 7, 22), result.shipDate());
        assertEquals(LocalDate.of(2026, 7, 24), result.receiveDate());
        assertEquals(LocalDate.of(2026, 7, 27), result.returnDate());
        assertEquals(LocalDate.of(2026, 7, 25), result.billableStartDate());
        assertEquals(LocalDate.of(2026, 7, 27), result.billableEndDate());
        assertEquals(LocalDate.of(2026, 7, 22), result.occupyStartDate());
        assertEquals(LocalDate.of(2026, 7, 28), result.occupyEndDateExclusive());
    }

    @Test
    void shouldKeepLogisticsDatesWhenExplicitRentalPeriodIsPresent() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货7.28上午/收货7.28下午/发回8.05 #租期7.29-8.05#",
                LocalDate.of(2026, 7, 28));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 7, 28), result.shipDate());
        assertEquals(LocalDate.of(2026, 7, 28), result.receiveDate());
        assertEquals(LocalDate.of(2026, 8, 5), result.returnDate());
        assertEquals(LocalDate.of(2026, 7, 29), result.billableStartDate());
        assertEquals(LocalDate.of(2026, 8, 5), result.billableEndDate());
        assertEquals(LocalDate.of(2026, 7, 28), result.occupyStartDate());
        assertEquals(LocalDate.of(2026, 8, 6), result.occupyEndDateExclusive());
    }

    @Test
    void shouldRemainPendingInsteadOfGuessingWhenRentalPeriodIsIncomplete() {
        SellerRemarkRentalPeriod result = parser.parse("客户稍后确认时间", LocalDate.of(2026, 7, 20));

        assertTrue(result.isPending());
        assertEquals("RENTAL_PERIOD_NOT_FOUND", result.reasonCode());
        assertNull(result.billableStartDate());
        assertNull(result.billableEndDate());
    }

    @Test
    void shouldKeepKnownShipDateWhenRemarkIsIncomplete() {
        SellerRemarkRentalPeriod result = parser.parse("发货8.01", LocalDate.of(2026, 7, 30));

        assertTrue(result.isPending());
        assertEquals("MISSING_RECEIVE_DATE", result.reasonCode());
        assertEquals(LocalDate.of(2026, 8, 1), result.shipDate());
    }

    @Test
    void shouldInferAnUnlabeledReceiptBetweenShipAndReturn() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货7.30 7.31 发回8.10", LocalDate.of(2026, 7, 30));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 7, 31), result.receiveDate());
        assertEquals(LocalDate.of(2026, 8, 1), result.billableStartDate());
    }

    @Test
    void shouldTreatARepeatedShipLabelAfterReceiptAsHistoricalReturnAlias() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货7.30 收货7.31 发货8.10", LocalDate.of(2026, 7, 30));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 8, 10), result.returnDate());
        assertEquals(LocalDate.of(2026, 8, 10), result.billableEndDate());
    }

    @Test
    void shouldInferAnUnlabeledReturnAfterReceipt() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货7.30 收货7.31 8.02", LocalDate.of(2026, 7, 30));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 8, 2), result.returnDate());
    }

    @Test
    void shouldTreatSelfPickupDateAsShipAndReceiveDate() {
        SellerRemarkRentalPeriod result = parser.parse(
                "8.01下午自提/发回8.04", LocalDate.of(2026, 7, 30));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 8, 1), result.shipDate());
        assertEquals(LocalDate.of(2026, 8, 1), result.receiveDate());
        assertEquals(LocalDate.of(2026, 8, 2), result.billableStartDate());
        assertEquals(LocalDate.of(2026, 8, 4), result.billableEndDate());
    }

    @Test
    void shouldKeepSelfPickupWithoutReturnDatePending() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货8.01上午自提", LocalDate.of(2026, 7, 30));

        assertTrue(result.isPending());
        assertEquals("MISSING_RETURN_DATE", result.reasonCode());
        assertEquals(LocalDate.of(2026, 8, 1), result.shipDate());
        assertEquals(LocalDate.of(2026, 8, 1), result.receiveDate());
    }

    @Test
    void shouldParseSelfPickupWithShortChineseDescriptor() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货8.07王源长沙自提", LocalDate.of(2026, 7, 22));

        assertTrue(result.isPending());
        assertEquals("MISSING_RETURN_DATE", result.reasonCode());
        assertEquals(LocalDate.of(2026, 8, 7), result.shipDate());
        assertEquals(LocalDate.of(2026, 8, 7), result.receiveDate());
    }

    @Test
    void shouldNotReuseLabeledDatesAsUnlabeledCandidates() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货7.30 收货7.31 预计8.01确认", LocalDate.of(2026, 7, 30));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 7, 30), result.shipDate());
        assertEquals(LocalDate.of(2026, 7, 31), result.receiveDate());
        assertEquals(LocalDate.of(2026, 8, 1), result.returnDate());
    }

    @Test
    void shouldRemainPendingWhenSellerRemarkIsMissing() {
        SellerRemarkRentalPeriod result = parser.parse(null, LocalDate.of(2026, 7, 20));

        assertTrue(result.isPending());
        assertEquals("MISSING_REMARK", result.reasonCode());
    }

    @Test
    void shouldRemainPendingWhenOrderDateIsMissing() {
        SellerRemarkRentalPeriod result = parser.parse("收货7.24/发回7.27", null);

        assertTrue(result.isPending());
        assertEquals("MISSING_ORDER_DATE", result.reasonCode());
    }

    @Test
    void shouldFailWhenRentalDateIsInvalid() {
        SellerRemarkRentalPeriod result = parser.parse("#租期2.30-3.02#", LocalDate.of(2026, 2, 20));

        assertEquals("FAILED", result.status());
        assertEquals("INVALID_RENTAL_DATE", result.reasonCode());
    }

    @Test
    void shouldFailWhenLogisticsDatesAreBeforeTheOrderDate() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货7.06收货7.07发回7.09", LocalDate.of(2026, 7, 28));

        assertEquals("FAILED", result.status());
        assertEquals("LOGISTICS_DATE_BEFORE_ORDER", result.reasonCode());
        assertNull(result.billableStartDate());
        assertNull(result.billableEndDate());
    }

    @Test
    void shouldFailInsteadOfAddingAYearWhenReturnDatePrecedesReceiptDate() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货7.30/收货7.31/发回7.10", LocalDate.of(2026, 7, 23));

        assertEquals("FAILED", result.status());
        assertEquals("INVALID_LOGISTICS_RANGE", result.reasonCode());
        assertNull(result.billableStartDate());
        assertNull(result.billableEndDate());
    }

    @Test
    void shouldStillInferARealCrossYearPeriod() {
        SellerRemarkRentalPeriod result = parser.parse(
                "发货12.30/收货12.31/发回1.03", LocalDate.of(2026, 12, 28));

        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2026, 12, 30), result.shipDate());
        assertEquals(LocalDate.of(2026, 12, 31), result.receiveDate());
        assertEquals(LocalDate.of(2027, 1, 3), result.returnDate());
        assertEquals(LocalDate.of(2027, 1, 1), result.billableStartDate());
        assertEquals(LocalDate.of(2027, 1, 3), result.billableEndDate());
    }

}
