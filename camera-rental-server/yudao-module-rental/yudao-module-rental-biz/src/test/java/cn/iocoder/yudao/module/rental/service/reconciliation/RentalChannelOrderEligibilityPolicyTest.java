package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RentalChannelOrderEligibilityPolicyTest {

    private final RentalChannelOrderEligibilityPolicy policy =
            new RentalChannelOrderEligibilityPolicy();

    @ParameterizedTest
    @ValueSource(strings = {"12", "21", "22"})
    void acceptsEveryOfficialPaidOrderStatus(String orderStatus) {
        XianyuOrderDO order = XianyuOrderDO.builder()
                .orderStatus(orderStatus)
                .payAmount(0L)
                .refundStatus(0)
                .build();

        assertNull(policy.ineligibleReason(order));
    }

    @Test
    void rejectsPendingPaymentRefundedAndClosedOrders() {
        assertEquals("ORDER_NOT_PAID", policy.ineligibleReason(order("11", 0)));
        assertEquals("ORDER_REFUNDED", policy.ineligibleReason(order("23", 0)));
        assertEquals("ORDER_REFUNDED", policy.ineligibleReason(order("12", 5)));
        assertEquals("ORDER_CLOSED", policy.ineligibleReason(order("24", 0)));
    }

    private static XianyuOrderDO order(String orderStatus, Integer refundStatus) {
        return XianyuOrderDO.builder()
                .orderStatus(orderStatus)
                .payAmount(100L)
                .refundStatus(refundStatus)
                .build();
    }

}
