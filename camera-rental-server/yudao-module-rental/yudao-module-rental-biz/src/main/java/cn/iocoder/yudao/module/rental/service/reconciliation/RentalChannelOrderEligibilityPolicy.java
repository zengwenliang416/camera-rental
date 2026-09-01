package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RentalChannelOrderEligibilityPolicy {

    private static final Set<String> PAID_ORDER_STATUSES = Set.of("12", "21", "22");
    private static final int REFUND_SUCCESS_STATUS = 5;

    public String ineligibleReason(XianyuOrderDO order) {
        if (order == null || order.getPayAmount() == null || order.getPayAmount() < 0) {
            return "INVALID_PAY_AMOUNT";
        }
        if ("23".equals(order.getOrderStatus())
                || Integer.valueOf(REFUND_SUCCESS_STATUS).equals(order.getRefundStatus())) {
            return "ORDER_REFUNDED";
        }
        if ("24".equals(order.getOrderStatus()) || order.getCancelTime() != null) {
            return "ORDER_CLOSED";
        }
        if (!PAID_ORDER_STATUSES.contains(order.getOrderStatus())) {
            return "ORDER_NOT_PAID";
        }
        return null;
    }

}
