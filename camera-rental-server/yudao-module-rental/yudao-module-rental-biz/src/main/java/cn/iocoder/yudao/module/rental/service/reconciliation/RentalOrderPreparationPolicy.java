package cn.iocoder.yudao.module.rental.service.reconciliation;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Component
public class RentalOrderPreparationPolicy {

    public RentalOrderPreparationDecision evaluate(XianyuOrderDO source,
                                                   RentalOrderItemDO item,
                                                   String modelReasonCode) {
        if (item == null || !StringUtils.hasText(item.getEquipmentModelCode())) {
            return RentalOrderPreparationDecision.waitingModel(
                    StringUtils.hasText(modelReasonCode) ? modelReasonCode : "MODEL_NOT_CONFIGURED");
        }
        if (item.getBillableStartDate() == null || item.getBillableEndDate() == null) {
            String reasonCode = source != null && StringUtils.hasText(source.getRentalPeriodReasonCode())
                    ? source.getRentalPeriodReasonCode() : "RENTAL_PERIOD_NOT_READY";
            return RentalOrderPreparationDecision.waitingRemark(reasonCode);
        }
        if (item.getOccupyStartDate() == null || item.getOccupyEndDateExclusive() == null
                || !item.getOccupyStartDate().isBefore(item.getOccupyEndDateExclusive())) {
            return RentalOrderPreparationDecision.waitingRemark("OCCUPIED_PERIOD_NOT_READY");
        }
        return RentalOrderPreparationDecision.ready();
    }

    public void requireReady(RentalOrderDO order, RentalOrderItemDO item) {
        if (order == null || item == null
                || !"READY".equals(order.getPreparationStatus())
                || !Objects.equals(order.getId(), item.getRentalOrderId())
                || !StringUtils.hasText(item.getEquipmentModelCode())
                || item.getBillableStartDate() == null || item.getBillableEndDate() == null
                || item.getOccupyStartDate() == null || item.getOccupyEndDateExclusive() == null
                || !item.getOccupyStartDate().isBefore(item.getOccupyEndDateExclusive())) {
            String reasonCode = order != null && StringUtils.hasText(order.getPreparationReasonCode())
                    ? order.getPreparationReasonCode() : "ORDER_NOT_READY";
            throw new RentalDeviceAssignmentException(
                    RentalDeviceAssignmentException.Code.ORDER_NOT_READY, reasonCode);
        }
    }

}
