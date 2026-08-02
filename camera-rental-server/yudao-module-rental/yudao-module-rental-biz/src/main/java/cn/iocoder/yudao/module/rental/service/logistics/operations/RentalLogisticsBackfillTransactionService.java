package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryDirectionEnum;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryCreateCommand;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryDeviceCommand;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RentalLogisticsBackfillTransactionService {

    private final RentalLogisticsOperationsMapper operationsMapper;
    private final RentalDeliveryService deliveryService;

    public RentalLogisticsBackfillTransactionService(RentalLogisticsOperationsMapper operationsMapper,
                                                     RentalDeliveryService deliveryService) {
        this.operationsMapper = operationsMapper;
        this.deliveryService = deliveryService;
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalDeliveryResult apply(RentalLogisticsOperationsMapper.BackfillCandidateRow candidate,
                                      boolean enqueueProviderTasks) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalDeliveryResult result = enqueueProviderTasks
                ? deliveryService.createOrReuse(toCommand(candidate))
                : deliveryService.createOrReuseLocalOnly(toCommand(candidate));
        if (candidate.getShipmentId() != null) {
            int updated = operationsMapper.bindShipmentDelivery(
                    tenantId, candidate.getShipmentId(), result.deliveryId());
            if (updated != 1) {
                throw new RentalLogisticsException("BACKFILL_SHIPMENT_BIND_CONFLICT");
            }
        }
        return result;
    }

    private RentalDeliveryCreateCommand toCommand(
            RentalLogisticsOperationsMapper.BackfillCandidateRow candidate) {
        return new RentalDeliveryCreateCommand(candidate.getRentalOrderId(), candidate.getChannelOrderId(),
                RentalDeliveryDirectionEnum.OUTBOUND, "XIANYU",
                candidate.getShipmentId() == null
                        ? "xianyu-order:" + candidate.getChannelOrderId()
                        : "legacy-shipment:" + candidate.getShipmentId(),
                candidate.getExpressCode(), candidate.getExpressName(), candidate.getWaybillNo(),
                candidate.getReceiverMobile(),
                candidate.getShipmentId() == null
                        ? List.of()
                        : List.of(new RentalDeliveryDeviceCommand(candidate.getRentalOrderItemId(),
                                candidate.getAssignmentId(), candidate.getDeviceId())));
    }
}
