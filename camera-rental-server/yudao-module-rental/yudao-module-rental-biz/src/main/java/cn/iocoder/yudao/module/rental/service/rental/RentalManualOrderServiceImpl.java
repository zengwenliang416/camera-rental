package cn.iocoder.yudao.module.rental.service.rental;

import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceDispatchReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualOrderCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualOrderCreateRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalCustomerDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceModelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalCustomerMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceModelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.enums.rental.RentalDeliveryMethodEnum;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentCommand;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.admin.RentalDeviceOpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_ASSIGNMENT_INCOMPLETE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_CONFIRM_EXPRESS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_DELIVERY_METHOD_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_DEVICE_ASSIGN_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_ORDER_MODEL_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_ORDER_NOT_EXISTS;

@Service
public class RentalManualOrderServiceImpl implements RentalManualOrderService {

    private static final String SOURCE_TYPE_OFFLINE = "OFFLINE";
    private static final String STATUS_PENDING_ALLOCATION = "PENDING_ALLOCATION";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String PREPARATION_READY = "READY";
    private static final String ASSIGNMENT_STATUS_ASSIGNED = "ASSIGNED";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final RentalOrderMapper orderMapper;
    private final RentalOrderItemMapper orderItemMapper;
    private final RentalCustomerMapper customerMapper;
    private final RentalOrderDeliveryMapper orderDeliveryMapper;
    private final RentalDeviceModelMapper deviceModelMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalDeviceAssignmentService assignmentService;
    private final RentalDeviceOpsService deviceOpsService;
    private final Clock clock;

    @Autowired
    public RentalManualOrderServiceImpl(RentalOrderMapper orderMapper,
                                        RentalOrderItemMapper orderItemMapper,
                                        RentalCustomerMapper customerMapper,
                                        RentalOrderDeliveryMapper orderDeliveryMapper,
                                        RentalDeviceModelMapper deviceModelMapper,
                                        RentalDeviceAssignmentMapper assignmentMapper,
                                        RentalDeviceAssignmentService assignmentService,
                                        RentalDeviceOpsService deviceOpsService) {
        this(orderMapper, orderItemMapper, customerMapper, orderDeliveryMapper, deviceModelMapper,
                assignmentMapper, assignmentService, deviceOpsService, Clock.system(BUSINESS_ZONE));
    }

    RentalManualOrderServiceImpl(RentalOrderMapper orderMapper,
                                 RentalOrderItemMapper orderItemMapper,
                                 RentalCustomerMapper customerMapper,
                                 RentalOrderDeliveryMapper orderDeliveryMapper,
                                 RentalDeviceModelMapper deviceModelMapper,
                                 RentalDeviceAssignmentMapper assignmentMapper,
                                 RentalDeviceAssignmentService assignmentService,
                                 RentalDeviceOpsService deviceOpsService,
                                 Clock clock) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.customerMapper = customerMapper;
        this.orderDeliveryMapper = orderDeliveryMapper;
        this.deviceModelMapper = deviceModelMapper;
        this.assignmentMapper = assignmentMapper;
        this.assignmentService = assignmentService;
        this.deviceOpsService = deviceOpsService;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RentalManualOrderCreateRespVO createManualOrder(RentalManualOrderCreateReqVO reqVO) {
        LocalDate start = reqVO.getBillableStartDate();
        LocalDate end = reqVO.getBillableEndDate();
        if (end.isBefore(start)) {
            throw exception(RENTAL_MANUAL_ORDER_INVALID, "计租结束日期早于开始日期");
        }
        if (start.isBefore(LocalDate.now(clock))) {
            throw exception(RENTAL_MANUAL_ORDER_INVALID, "计租开始日期早于当天");
        }
        RentalDeliveryMethodEnum deliveryMethod = resolveDeliveryMethod(reqVO.getDelivery());
        long totalRentAmount = 0L;
        Set<Long> selectedDeviceIds = new HashSet<>();
        for (RentalManualOrderCreateReqVO.Item item : reqVO.getItems()) {
            RentalDeviceModelDO model = deviceModelMapper.selectByCode(item.getModelCode().trim());
            if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
                throw exception(RENTAL_MANUAL_ORDER_MODEL_INVALID, item.getModelCode().trim());
            }
            validateSelectedDevices(item, selectedDeviceIds);
            totalRentAmount += item.getRentAmount();
        }

        RentalCustomerDO customer = resolveCustomer(reqVO.getCustomer());

        RentalOrderDO order = RentalOrderDO.builder()
                .orderNo("OFF-PENDING-" + UUID.randomUUID().toString().replace("-", ""))
                .sourceType(SOURCE_TYPE_OFFLINE)
                .status(STATUS_PENDING_ALLOCATION)
                .customerId(customer.getId())
                .rentAmount(totalRentAmount)
                .depositAmount(reqVO.getDepositAmount())
                .refundAmount(0L)
                .billableStartDate(start)
                .billableEndDate(end)
                .occupyStartDate(start)
                .occupyEndDateExclusive(end.plusDays(1))
                .expectedSendBackDate(end)
                .preparationStatus(PREPARATION_READY)
                .preparationUpdatedAt(LocalDateTime.now(clock))
                .build();
        orderMapper.insert(order);
        order.setOrderNo("OFF-" + String.format("%019d", order.getId()));
        orderMapper.updateById(order);

        RentalManualOrderCreateReqVO.Delivery delivery = reqVO.getDelivery();
        RentalOrderDeliveryDO orderDelivery = RentalOrderDeliveryDO.builder()
                .rentalOrderId(order.getId())
                .deliveryMethod(deliveryMethod.name())
                .receiverName(trimToNull(delivery.getReceiverName()))
                .receiverMobile(trimToNull(delivery.getReceiverMobile()))
                .receiverAddress(trimToNull(delivery.getReceiverAddress()))
                .deliveryRemark(trimToNull(delivery.getRemark()))
                .build();
        orderDeliveryMapper.insert(orderDelivery);

        for (RentalManualOrderCreateReqVO.Item item : reqVO.getItems()) {
            RentalOrderItemDO orderItem = RentalOrderItemDO.builder()
                    .rentalOrderId(order.getId())
                    .equipmentModelCode(item.getModelCode().trim())
                    .quantity(item.getQuantity())
                    .rentAmount(item.getRentAmount())
                    .billableStartDate(start)
                    .billableEndDate(end)
                    .occupyStartDate(start)
                    .occupyEndDateExclusive(end.plusDays(1))
                    .expectedSendBackDate(end)
                    .build();
            orderItemMapper.insert(orderItem);
            assignSelectedDevices(orderItem, item, start, end.plusDays(1));
        }

        RentalManualOrderCreateRespVO respVO = new RentalManualOrderCreateRespVO();
        respVO.setId(order.getId());
        respVO.setOrderNo(order.getOrderNo());
        return respVO;
    }

    private void validateSelectedDevices(RentalManualOrderCreateReqVO.Item item, Set<Long> selectedDeviceIds) {
        if (item.getDeviceIds().size() != item.getQuantity()) {
            throw exception(RENTAL_MANUAL_ORDER_INVALID, "设备数量必须与选中的具体设备数量一致");
        }
        for (Long deviceId : item.getDeviceIds()) {
            if (!selectedDeviceIds.add(deviceId)) {
                throw exception(RENTAL_MANUAL_ORDER_INVALID, "同一具体设备不能重复选择");
            }
        }
    }

    private void assignSelectedDevices(RentalOrderItemDO orderItem, RentalManualOrderCreateReqVO.Item item,
                                       LocalDate occupyStartDate, LocalDate occupyEndDateExclusive) {
        for (Long deviceId : item.getDeviceIds()) {
            try {
                assignmentService.assign(new RentalDeviceAssignmentCommand(
                        orderItem.getId(), deviceId, occupyStartDate, occupyEndDateExclusive,
                        "offline-create:" + orderItem.getRentalOrderId() + ":" + orderItem.getId() + ":" + deviceId));
            } catch (RentalDeviceAssignmentException ex) {
                throw exception(RENTAL_MANUAL_ORDER_DEVICE_ASSIGN_FAILED, ex.getCode().name());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOutbound(Long orderId) {
        RentalOrderDO order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw exception(RENTAL_ORDER_NOT_EXISTS);
        }
        if (STATUS_CANCELED.equals(order.getStatus())) {
            throw exception(RENTAL_MANUAL_ORDER_INVALID, "订单已取消");
        }
        RentalOrderDeliveryDO delivery = orderDeliveryMapper.selectByRentalOrderId(orderId);
        if (delivery == null) {
            throw exception(RENTAL_MANUAL_ORDER_INVALID, "订单缺少配送信息");
        }
        RentalDeliveryMethodEnum deliveryMethod =
                RentalDeliveryMethodEnum.of(delivery.getDeliveryMethod());
        if (deliveryMethod == null) {
            throw exception(RENTAL_MANUAL_ORDER_DELIVERY_METHOD_INVALID);
        }
        if (deliveryMethod == RentalDeliveryMethodEnum.EXPRESS) {
            throw exception(RENTAL_MANUAL_ORDER_CONFIRM_EXPRESS);
        }
        List<RentalOrderItemDO> items = orderItemMapper.selectListByRentalOrderIds(List.of(orderId));
        for (RentalOrderItemDO item : items) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (assignmentMapper.countAssignedByOrderItem(item.getId()) < quantity) {
                throw exception(RENTAL_MANUAL_ORDER_ASSIGNMENT_INCOMPLETE);
            }
        }
        for (RentalDeviceAssignmentDO assignment
                : assignmentMapper.selectActiveListByRentalOrderId(orderId)) {
            if (!ASSIGNMENT_STATUS_ASSIGNED.equals(assignment.getStatus())) {
                continue;
            }
            RentalDeviceDispatchReqVO dispatchReqVO = new RentalDeviceDispatchReqVO();
            dispatchReqVO.setDeviceId(assignment.getDeviceId());
            dispatchReqVO.setAssignmentId(assignment.getId());
            deviceOpsService.dispatch(dispatchReqVO);
        }
    }

    @Override
    public RentalCustomerDO suggestCustomer(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return null;
        }
        return customerMapper.selectByMobile(mobile.trim());
    }

    private RentalCustomerDO resolveCustomer(RentalManualOrderCreateReqVO.Customer request) {
        String mobile = request.getMobile().trim();
        String name = request.getName().trim();
        String wechatId = trimToNull(request.getWechatId());
        RentalCustomerDO existing = customerMapper.selectByMobile(mobile);
        if (existing != null) {
            if (!Objects.equals(existing.getName(), name)
                    || !Objects.equals(existing.getWechatId(), wechatId)) {
                existing.setName(name);
                existing.setWechatId(wechatId);
                customerMapper.updateById(existing);
            }
            return existing;
        }
        RentalCustomerDO customer = RentalCustomerDO.builder()
                .name(name)
                .mobile(mobile)
                .wechatId(wechatId)
                .build();
        customerMapper.insert(customer);
        return customer;
    }

    private static RentalDeliveryMethodEnum resolveDeliveryMethod(
            RentalManualOrderCreateReqVO.Delivery delivery) {
        RentalDeliveryMethodEnum method = RentalDeliveryMethodEnum.of(delivery.getMethod());
        if (method == null) {
            throw exception(RENTAL_MANUAL_ORDER_DELIVERY_METHOD_INVALID);
        }
        if (method.requiresReceiverInfo()
                && (!StringUtils.hasText(delivery.getReceiverName())
                || !StringUtils.hasText(delivery.getReceiverMobile())
                || !StringUtils.hasText(delivery.getReceiverAddress()))) {
            throw exception(RENTAL_MANUAL_ORDER_INVALID, "跑腿/自送配送必须填写收货人姓名、手机号和地址");
        }
        return method;
    }

    private static String trimToNull(String value) {
        return !StringUtils.hasText(value) ? null : value.trim();
    }

}
