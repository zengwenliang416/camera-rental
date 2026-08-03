package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDeviceRelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsCarrierMappingDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryDeviceRelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalDeliveryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryLifecycleStatusEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryOutboxEventTypeEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryQueryStatusEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliverySubscribeStatusEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalLogisticsPhoneRequirementEnum;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalTrackingStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class RentalDeliveryServiceImpl implements RentalDeliveryService {

    private static final String MAPPING_READY = "READY";
    private static final String MAPPING_REQUIRED = "MAPPING_REQUIRED";
    private static final String SYSTEM_OPERATOR = "system";

    private final RentalDeliveryMapper deliveryMapper;
    private final RentalDeliveryDeviceRelMapper relationMapper;
    private final RentalOrderMapper orderMapper;
    private final XianyuOrderMapper xianyuOrderMapper;
    private final RentalOrderItemMapper orderItemMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalDeviceMapper deviceMapper;
    private final RentalCarrierMappingService carrierMappingService;
    private final RentalLogisticsProviderConfigService providerConfigService;
    private final RentalDeliveryOutboxService outboxService;
    private final WaybillPrivacy waybillPrivacy;

    public RentalDeliveryServiceImpl(RentalDeliveryMapper deliveryMapper,
                                     RentalDeliveryDeviceRelMapper relationMapper,
                                     RentalOrderMapper orderMapper,
                                     XianyuOrderMapper xianyuOrderMapper,
                                     RentalOrderItemMapper orderItemMapper,
                                     RentalDeviceAssignmentMapper assignmentMapper,
                                     RentalDeviceMapper deviceMapper,
                                     RentalCarrierMappingService carrierMappingService,
                                     RentalLogisticsProviderConfigService providerConfigService,
                                     RentalDeliveryOutboxService outboxService,
                                     WaybillPrivacy waybillPrivacy) {
        this.deliveryMapper = deliveryMapper;
        this.relationMapper = relationMapper;
        this.orderMapper = orderMapper;
        this.xianyuOrderMapper = xianyuOrderMapper;
        this.orderItemMapper = orderItemMapper;
        this.assignmentMapper = assignmentMapper;
        this.deviceMapper = deviceMapper;
        this.carrierMappingService = carrierMappingService;
        this.providerConfigService = providerConfigService;
        this.outboxService = outboxService;
        this.waybillPrivacy = waybillPrivacy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RentalDeliveryResult createOrReuse(RentalDeliveryCreateCommand command) {
        return createOrReuse(command, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RentalDeliveryResult createOrReuseLocalOnly(RentalDeliveryCreateCommand command) {
        return createOrReuse(command, false);
    }

    private RentalDeliveryResult createOrReuse(RentalDeliveryCreateCommand command, boolean enqueueProviderTasks) {
        validateCommand(command);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (command.rentalOrderId() != null) {
            RentalOrderDO order = orderMapper.selectByIdForUpdate(command.rentalOrderId());
            requireEntity(order, tenantId, "RENTAL_ORDER_NOT_FOUND");
        }
        if (command.channelOrderId() != null) {
            XianyuOrderDO channelOrder = xianyuOrderMapper.selectById(command.channelOrderId());
            requireEntity(channelOrder, tenantId, "CHANNEL_ORDER_NOT_FOUND");
        }

        String normalizedWaybill = waybillPrivacy.normalize(command.waybillNo());
        RentalCarrierResolution carrier = carrierMappingService.resolve(
                command.sourceType(), command.sourceCarrierCode());
        RentalLogisticsCarrierMappingDO mapping = carrier.mapping();
        RentalDeliveryDO delivery = deliveryMapper.selectByReferenceBusinessKeyForUpdate(
                tenantId, command.rentalOrderId(), command.channelOrderId(),
                command.direction().name(), carrier.canonicalCarrierCode(), normalizedWaybill);
        boolean created = delivery == null;
        if (created) {
            delivery = createDelivery(command, carrier, normalizedWaybill, tenantId);
        } else if (delivery.getRentalOrderId() == null && command.rentalOrderId() != null) {
            delivery.setRentalOrderId(command.rentalOrderId());
            delivery.setUpdater(SYSTEM_OPERATOR);
            deliveryMapper.updateById(delivery);
        }
        bindDevices(delivery, command.devices(), tenantId);
        if (created && enqueueProviderTasks && !isTrackingPhoneRequired(delivery)) {
            if (RentalDeliverySubscribeStatusEnum.PENDING.name().equals(delivery.getSubscribeStatus())) {
                outboxService.enqueue(delivery.getId(), RentalDeliveryOutboxEventTypeEnum.SUBSCRIBE, null,
                        "delivery tracking subscription");
            }
            if (RentalDeliveryQueryStatusEnum.PENDING.name().equals(delivery.getQueryStatus())) {
                outboxService.enqueue(delivery.getId(), RentalDeliveryOutboxEventTypeEnum.INITIAL_QUERY, null,
                        "delivery initial tracking query");
            }
        }
        return toResult(delivery, created);
    }

    @Override
    public RentalDeliveryResult getResult(Long deliveryId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalDeliveryDO delivery = deliveryMapper.selectByTenantIdAndId(tenantId, deliveryId);
        if (delivery == null) {
            throw new RentalLogisticsException("DELIVERY_NOT_FOUND");
        }
        return toResult(delivery, false);
    }

    private RentalDeliveryDO createDelivery(RentalDeliveryCreateCommand command,
                                            RentalCarrierResolution carrier,
                                            String normalizedWaybill,
                                            Long tenantId) {
        RentalLogisticsCarrierMappingDO mapping = carrier.mapping();
        String providerCode = mapping == null ? null : mapping.getProviderCode();
        RentalLogisticsProviderConfigDO config =
                providerCode == null ? null : providerConfigService.get(providerCode);
        boolean providerEnabled = config != null && Boolean.TRUE.equals(config.getEnabled());
        boolean trackingPhoneRequired = requiresTrackingPhone(mapping)
                && !StringUtils.hasText(command.trackingPhone());
        String subscribeStatus = mapping == null
                ? RentalDeliverySubscribeStatusEnum.MAPPING_REQUIRED.name()
                : trackingPhoneRequired
                ? RentalDeliverySubscribeStatusEnum.PHONE_REQUIRED.name()
                : providerEnabled && Boolean.TRUE.equals(config.getSubscribeEnabled())
                ? RentalDeliverySubscribeStatusEnum.PENDING.name()
                : RentalDeliverySubscribeStatusEnum.PROVIDER_DISABLED.name();
        String queryStatus = mapping == null
                ? RentalDeliveryQueryStatusEnum.MAPPING_REQUIRED.name()
                : trackingPhoneRequired
                ? RentalDeliveryQueryStatusEnum.PHONE_REQUIRED.name()
                : providerEnabled && Boolean.TRUE.equals(config.getQueryEnabled())
                ? RentalDeliveryQueryStatusEnum.PENDING.name()
                : RentalDeliveryQueryStatusEnum.PROVIDER_DISABLED.name();
        Integer maxSeq = command.channelOrderId() == null
                ? deliveryMapper.selectMaxPackageSeq(tenantId, command.rentalOrderId(), command.direction().name())
                : deliveryMapper.selectMaxChannelPackageSeq(
                        tenantId, command.channelOrderId(), command.direction().name());
        RentalDeliveryDO delivery = RentalDeliveryDO.builder()
                .rentalOrderId(command.rentalOrderId())
                .channelOrderId(command.channelOrderId())
                .direction(command.direction().name())
                .packageSeq((maxSeq == null ? 0 : maxSeq) + 1)
                .sourceType(carrier.sourceType())
                .sourceIdentifier(trimToNull(command.sourceIdentifier()))
                .sourceCarrierCode(carrier.sourceCarrierCode())
                .sourceCarrierName(trimToNull(command.sourceCarrierName()))
                .canonicalCarrierCode(carrier.canonicalCarrierCode())
                .providerCode(providerCode)
                .providerCarrierCode(mapping == null ? null : mapping.getProviderCarrierCode())
                .waybillNo(command.waybillNo().trim())
                .normalizedWaybillNo(normalizedWaybill)
                .trackingPhone(trimToNull(command.trackingPhone()))
                .lifecycleStatus(RentalDeliveryLifecycleStatusEnum.ACTIVE.name())
                .mappingStatus(mapping == null ? MAPPING_REQUIRED : MAPPING_READY)
                .subscribeStatus(subscribeStatus)
                .queryStatus(queryStatus)
                .trackingStatus(RentalTrackingStatusEnum.CREATED.name())
                .trackingVersion(0)
                .subscribeCount(0)
                .lastErrorCode(trackingPhoneRequired ? "TRACKING_PHONE_REQUIRED" : null)
                .lastErrorMessage(trackingPhoneRequired ? "Tracking phone is required by carrier mapping" : null)
                .build();
        delivery.setCreator(SYSTEM_OPERATOR);
        delivery.setUpdater(SYSTEM_OPERATOR);
        deliveryMapper.insert(delivery);
        return delivery;
    }

    private boolean requiresTrackingPhone(RentalLogisticsCarrierMappingDO mapping) {
        return mapping != null && RentalLogisticsPhoneRequirementEnum.REQUIRED.name()
                .equals(mapping.getPhoneRequirement());
    }

    private boolean isTrackingPhoneRequired(RentalDeliveryDO delivery) {
        return RentalDeliverySubscribeStatusEnum.PHONE_REQUIRED.name().equals(delivery.getSubscribeStatus())
                || RentalDeliveryQueryStatusEnum.PHONE_REQUIRED.name().equals(delivery.getQueryStatus());
    }

    private RentalDeliveryResult toResult(RentalDeliveryDO delivery, boolean created) {
        return new RentalDeliveryResult(delivery.getId(), created, delivery.getMappingStatus(),
                delivery.getSubscribeStatus(), delivery.getQueryStatus(),
                waybillPrivacy.mask(delivery.getWaybillNo()), trackingReason(delivery),
                outboxService.listPendingEventTypes(delivery.getId()));
    }

    private String trackingReason(RentalDeliveryDO delivery) {
        if (StringUtils.hasText(delivery.getLastErrorCode())) {
            return delivery.getLastErrorCode();
        }
        if (!MAPPING_READY.equals(delivery.getMappingStatus())) {
            return delivery.getMappingStatus();
        }
        if (!RentalDeliveryQueryStatusEnum.PENDING.name().equals(delivery.getQueryStatus())
                && !RentalDeliveryQueryStatusEnum.READY.name().equals(delivery.getQueryStatus())
                && !RentalDeliveryQueryStatusEnum.QUEUED.name().equals(delivery.getQueryStatus())) {
            return delivery.getQueryStatus();
        }
        return null;
    }

    private void bindDevices(RentalDeliveryDO delivery, Iterable<RentalDeliveryDeviceCommand> devices,
                             Long tenantId) {
        List<ValidatedDeliveryDevice> validatedDevices = new ArrayList<>();
        for (RentalDeliveryDeviceCommand deviceCommand : devices) {
            validatedDevices.add(validateDeviceRelation(delivery, deviceCommand, tenantId));
        }

        Set<Long> uniqueDevices = new HashSet<>();
        for (ValidatedDeliveryDevice validatedDevice : validatedDevices) {
            RentalDeliveryDeviceCommand deviceCommand = validatedDevice.command();
            RentalDeviceAssignmentDO assignment = validatedDevice.assignment();
            RentalDeviceDO device = validatedDevice.device();
            if (!uniqueDevices.add(device.getId())) {
                continue;
            }
            if (relationMapper.selectByDeliveryAndDeviceForUpdate(tenantId, delivery.getId(), device.getId()) != null) {
                continue;
            }
            RentalDeliveryDeviceRelDO relation = RentalDeliveryDeviceRelDO.builder()
                    .deliveryId(delivery.getId())
                    .rentalOrderId(delivery.getRentalOrderId())
                    .rentalOrderItemId(deviceCommand.rentalOrderItemId())
                    .assignmentId(assignment.getId())
                    .deviceId(device.getId())
                    .build();
            relation.setCreator(SYSTEM_OPERATOR);
            relation.setUpdater(SYSTEM_OPERATOR);
            relationMapper.insert(relation);
        }
    }

    private ValidatedDeliveryDevice validateDeviceRelation(RentalDeliveryDO delivery,
                                                           RentalDeliveryDeviceCommand deviceCommand,
                                                           Long tenantId) {
        if (deviceCommand == null || deviceCommand.rentalOrderItemId() == null
                || deviceCommand.assignmentId() == null || deviceCommand.deviceId() == null) {
            throw new RentalLogisticsException("DELIVERY_DEVICE_REL_INVALID");
        }
        RentalOrderItemDO item = orderItemMapper.selectByIdForUpdate(deviceCommand.rentalOrderItemId());
        RentalDeviceAssignmentDO assignment = assignmentMapper.selectByIdForUpdate(deviceCommand.assignmentId());
        RentalDeviceDO device = deviceMapper.selectByIdForUpdate(deviceCommand.deviceId());
        requireEntity(item, tenantId, "RENTAL_ORDER_ITEM_NOT_FOUND");
        requireEntity(assignment, tenantId, "RENTAL_ASSIGNMENT_NOT_FOUND");
        requireEntity(device, tenantId, "RENTAL_DEVICE_NOT_FOUND");
        if (!Objects.equals(delivery.getRentalOrderId(), item.getRentalOrderId())
                || !Objects.equals(delivery.getRentalOrderId(), assignment.getRentalOrderId())
                || !Objects.equals(item.getId(), assignment.getRentalOrderItemId())
                || !Objects.equals(device.getId(), assignment.getDeviceId())) {
            throw new RentalLogisticsException("DELIVERY_DEVICE_REL_MISMATCH");
        }
        return new ValidatedDeliveryDevice(deviceCommand, assignment, device);
    }

    private void validateCommand(RentalDeliveryCreateCommand command) {
        if (command == null || (command.rentalOrderId() == null && command.channelOrderId() == null)
                || command.direction() == null
                || !StringUtils.hasText(command.sourceType()) || !StringUtils.hasText(command.sourceCarrierCode())
                || !StringUtils.hasText(command.waybillNo())
                || (command.rentalOrderId() != null && command.devices().isEmpty()
                    && command.channelOrderId() == null)) {
            throw new RentalLogisticsException("DELIVERY_COMMAND_INVALID");
        }
    }

    private void requireEntity(Object entity, Long tenantId, String code) {
        if (entity == null) {
            throw new RentalLogisticsException(code);
        }
        if (entity instanceof cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO tenantEntity
                && tenantEntity.getTenantId() != null && !Objects.equals(tenantId, tenantEntity.getTenantId())) {
            throw new RentalLogisticsException("TENANT_MISMATCH");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record ValidatedDeliveryDevice(
            RentalDeliveryDeviceCommand command,
            RentalDeviceAssignmentDO assignment,
            RentalDeviceDO device
    ) {
    }
}
