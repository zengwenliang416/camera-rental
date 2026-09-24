package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceQuantityUpdateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceShipmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceShipmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderEligibilityPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalOrderQuantityService {
    private final XianyuOrderMapper channelMapper;
    private final RentalOrderMapper orderMapper;
    private final RentalOrderItemMapper itemMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalDeviceShipmentMapper shipmentMapper;
    private final RentalChannelOrderEligibilityPolicy eligibilityPolicy;

    @Transactional(rollbackFor = Exception.class)
    public Integer update(Long itemId, RentalDeviceQuantityUpdateReqVO request) {
        if (request.getQuantity() == null || request.getQuantity() < 1 || request.getQuantity() > 999
                || request.getExpectedQuantity() == null || request.getExpectedQuantity() < 1) {
            throw exception(RENTAL_DEVICE_QUANTITY_INVALID);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalOrderItemDO snapshot = itemMapper.selectById(itemId);
        if (snapshot == null || !Objects.equals(snapshot.getTenantId(), tenantId)) {
            throw exception(RENTAL_DEVICE_QUANTITY_NOT_FOUND);
        }
        RentalOrderDO orderSnapshot = orderMapper.selectById(snapshot.getRentalOrderId());
        if (orderSnapshot == null || !Objects.equals(orderSnapshot.getTenantId(), tenantId)) {
            throw exception(RENTAL_DEVICE_QUANTITY_NOT_FOUND);
        }
        if (!"XIANYU".equals(orderSnapshot.getSourceType()) || orderSnapshot.getChannelOrderId() == null) {
            throw exception(RENTAL_DEVICE_QUANTITY_NOT_EDITABLE);
        }
        // Same lock order as reconciliation/shipping; allocation also locks order before item.
        // History guards below also use current reads: initial snapshots may precede a concurrent assignment.
        XianyuOrderDO channel = channelMapper.selectByIdForUpdate(orderSnapshot.getChannelOrderId());
        RentalOrderDO order = orderMapper.selectByIdForUpdate(orderSnapshot.getId());
        RentalOrderItemDO item = itemMapper.selectByIdForUpdate(itemId);
        if (channel == null || order == null || item == null
                || !Objects.equals(channel.getTenantId(), tenantId)
                || !Objects.equals(order.getTenantId(), tenantId)
                || !Objects.equals(item.getTenantId(), tenantId)
                || !Objects.equals(item.getRentalOrderId(), order.getId())
                || !Objects.equals(channel.getRentalOrderId(), order.getId())
                || !Objects.equals(order.getChannelOrderId(), channel.getId())) {
            throw exception(RENTAL_DEVICE_QUANTITY_NOT_FOUND);
        }
        if (!"XIANYU".equals(order.getSourceType()) || !"PENDING_ALLOCATION".equals(order.getStatus())
                || order.getSettledAt() != null || !"12".equals(channel.getOrderStatus())
                || channel.getConsignTime() != null || eligibilityPolicy.ineligibleReason(channel) != null
                || !assignmentMapper.selectList(new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                    .select(RentalDeviceAssignmentDO::getId)
                    .eq(RentalDeviceAssignmentDO::getTenantId, tenantId)
                    .eq(RentalDeviceAssignmentDO::getRentalOrderId, order.getId())
                    .last("LIMIT 1 FOR UPDATE")).isEmpty()
                || !shipmentMapper.selectList(new LambdaQueryWrapper<RentalDeviceShipmentDO>()
                    .select(RentalDeviceShipmentDO::getId)
                    .eq(RentalDeviceShipmentDO::getTenantId, tenantId)
                    .eq(RentalDeviceShipmentDO::getChannelOrderId, channel.getId())
                    .last("LIMIT 1 FOR UPDATE")).isEmpty()) {
            throw exception(RENTAL_DEVICE_QUANTITY_NOT_EDITABLE);
        }
        if (!Objects.equals(item.getQuantity(), request.getExpectedQuantity())) {
            throw exception(RENTAL_DEVICE_QUANTITY_CHANGED);
        }
        Integer previous = item.getQuantity();
        if (!Objects.equals(previous, request.getQuantity()) || !"CONFIRMED".equals(item.getQuantitySource())) {
            item.setQuantity(request.getQuantity());
            item.setQuantitySource("CONFIRMED");
            itemMapper.updateById(item);
            log.info("[rental][device-quantity] tenantId={} orderId={} itemId={} from={} to={}",
                    tenantId, order.getId(), itemId, previous, request.getQuantity());
        }
        return item.getQuantity();
    }
}
