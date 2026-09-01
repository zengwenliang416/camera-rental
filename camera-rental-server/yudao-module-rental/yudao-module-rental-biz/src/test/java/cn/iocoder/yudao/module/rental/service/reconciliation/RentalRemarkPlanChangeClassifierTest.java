package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RentalRemarkPlanChangeClassifierTest {

    private final RentalRemarkPlanChangeClassifier classifier = new RentalRemarkPlanChangeClassifier();

    @ParameterizedTest
    @CsvSource({
            "'发货8.31/收货9.1/发回9.8/续租', EXTENSION",
            "'发货8.31/收货9.1/发回9.4/早退', EARLY_RETURN",
            "'发货9.1/收货9.2/发回9.8/改期', RESCHEDULE",
            "'发货8.31/收货9.1/发回9.6/换机', REPLACEMENT",
            "'发货8.31/收货9.1/发回9.6/损坏', DAMAGE",
            "'发货8.31/收货9.1/发回9.6/遗失', LOSS",
            "'发货8.31/收货9.1/发回9.6/逾期', OVERDUE",
            "'发货8.31/收货9.1/发回9.6/物流延误', LOGISTICS_DELAY"
    })
    void shouldClassifyApprovedSpecialCaseSuffixes(String remark,
                                                   RentalRemarkPlanChangeType expected) {
        SellerRemarkRentalPeriod previous = plan(
                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 6),
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 6));
        SellerRemarkRentalPeriod candidate = switch (expected) {
            case EXTENSION -> plan(
                    LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 8),
                    LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 8));
            case EARLY_RETURN -> plan(
                    LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 4),
                    LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 4));
            case RESCHEDULE -> plan(
                    LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 8),
                    LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 9, 8));
            default -> previous;
        };

        assertEquals(expected, classifier.classify(remark, previous, candidate));
    }

    @Test
    void shouldInferDateChangesWithoutSuffixes() {
        SellerRemarkRentalPeriod previous = plan(
                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 6),
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 6));

        assertEquals(RentalRemarkPlanChangeType.EXTENSION,
                classifier.classify("发货8.31/收货9.1/发回9.8", previous,
                        plan(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 8),
                                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 8))));
        assertEquals(RentalRemarkPlanChangeType.EARLY_RETURN,
                classifier.classify("发货8.31/收货9.1/发回9.4", previous,
                        plan(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 4),
                                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 4))));
        assertEquals(RentalRemarkPlanChangeType.RESCHEDULE,
                classifier.classify("发货9.1/收货9.2/发回9.8", previous,
                        plan(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 8),
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2),
                                LocalDate.of(2026, 9, 8))));
    }

    @Test
    void shouldRejectInvalidOrContradictorySpecialCases() {
        SellerRemarkRentalPeriod previous = plan(
                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 6),
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 6));

        assertEquals(RentalRemarkPlanChangeType.INVALID,
                classifier.classify("换机",
                        previous, SellerRemarkRentalPeriod.pending("v1", "RENTAL_PERIOD_NOT_FOUND")));
        assertEquals(RentalRemarkPlanChangeType.AMBIGUOUS,
                classifier.classify("发货8.31/收货9.1/发回9.4/续租",
                        previous, plan(
                                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 4),
                                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 4))));
        assertEquals(RentalRemarkPlanChangeType.AMBIGUOUS,
                classifier.classify("发货8.31/收货9.1/发回9.8/续租/换机",
                        previous, plan(
                                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 8),
                                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 8))));
        assertEquals(RentalRemarkPlanChangeType.AMBIGUOUS,
                classifier.classify("发货8.31/收货9.1/发回9.8/续租",
                        null, plan(
                                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 8),
                                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 8))));
    }

    private static SellerRemarkRentalPeriod plan(LocalDate billableStartDate,
                                                 LocalDate billableEndDate,
                                                 LocalDate shipDate,
                                                 LocalDate receiveDate,
                                                 LocalDate returnDate) {
        return SellerRemarkRentalPeriod.success(
                "test", billableStartDate, billableEndDate, shipDate, receiveDate, returnDate);
    }

}
