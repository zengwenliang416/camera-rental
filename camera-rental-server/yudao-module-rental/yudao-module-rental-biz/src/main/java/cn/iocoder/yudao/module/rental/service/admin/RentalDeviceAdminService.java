package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceQrRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceUpdateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentCommand;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentResult;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService.DeviceNumberSelection;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceQrCodec;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_ASSIGN_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_DISABLE_BLOCKED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NO_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_QR_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_QR_MODEL_MISMATCH;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_SERIAL_DUPLICATE;

@Service
public class RentalDeviceAdminService {

    private final RentalDeviceMapper deviceMapper;
    private final RentalDeviceAssignmentMapper assignmentMapper;
    private final RentalDeviceDeletionGuard deletionGuard;
    private final RentalDeviceAssignmentService assignmentService;
    private final RentalDeviceQrCodec qrCodec;
    private final RentalDeviceProperties deviceProperties;
    private final RentalDeviceCatalogService deviceCatalogService;

    public RentalDeviceAdminService(RentalDeviceMapper deviceMapper,
                                    RentalDeviceAssignmentMapper assignmentMapper,
                                    RentalDeviceDeletionGuard deletionGuard,
                                    RentalDeviceAssignmentService assignmentService,
                                    RentalDeviceQrCodec qrCodec,
                                    RentalDeviceProperties deviceProperties,
                                    RentalDeviceCatalogService deviceCatalogService) {
        this.deviceMapper = deviceMapper;
        this.assignmentMapper = assignmentMapper;
        this.deletionGuard = deletionGuard;
        this.assignmentService = assignmentService;
        this.qrCodec = qrCodec;
        this.deviceProperties = deviceProperties;
        this.deviceCatalogService = deviceCatalogService;
    }

    public PageResult<RentalDeviceRespVO> getDevicePage(String categoryCode, String equipmentModelCode,
                                                        PageParam pageParam) {
        PageResult<RentalDeviceDO> page = deviceMapper.selectPage(pageParam,
                new LambdaQueryWrapperX<RentalDeviceDO>()
                        .eqIfPresent(RentalDeviceDO::getCategoryCode, categoryCode)
                        .eqIfPresent(RentalDeviceDO::getEquipmentModelCode, equipmentModelCode)
                        .orderByDesc(RentalDeviceDO::getId));
        List<RentalDeviceRespVO> list = page.getList().stream().map(this::toVo).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(RentalDeviceCreateReqVO reqVO) {
        DeviceNumberSelection selection = deviceCatalogService.composeDeviceNumber(
                reqVO.getCategoryCode(), reqVO.getEquipmentModelCode(), reqVO.getDeviceNoSuffix());
        String deviceNo = selection.deviceNo();
        if (deviceMapper.selectByDeviceNo(deviceNo) != null) {
            throw exception(RENTAL_DEVICE_NO_DUPLICATE, deviceNo);
        }
        RentalDeviceDO device = RentalDeviceDO.builder()
                .deviceNo(deviceNo)
                .serialNumber(reqVO.getSerialNumber())
                .categoryCode(selection.model().categoryCode())
                .equipmentModelCode(selection.model().modelCode())
                .status(reqVO.getStatus() == null ? "AVAILABLE" : reqVO.getStatus())
                .warehouseCode(reqVO.getWarehouseCode())
                .purchaseAmount(reqVO.getPurchaseAmount())
                .enabled(reqVO.getEnabled() == null || reqVO.getEnabled())
                .build();
        try {
            deviceMapper.insert(device);
        } catch (DuplicateKeyException ex) {
            if (deviceMapper.selectByDeviceNo(deviceNo) != null) {
                throw exception(RENTAL_DEVICE_NO_DUPLICATE, deviceNo);
            }
            throw ex;
        }
        return device.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(RentalDeviceUpdateReqVO reqVO) {
        RentalDeviceDO device = requireDeviceForUpdate(reqVO.getId());
        String serialNumber = normalizeOptional(reqVO.getSerialNumber());
        String warehouseCode = normalizeOptional(reqVO.getWarehouseCode());
        validateSerialNumberUnique(serialNumber, device);
        if (Boolean.FALSE.equals(reqVO.getEnabled())) {
            if (!"AVAILABLE".equals(device.getStatus())) {
                throw exception(RENTAL_DEVICE_DISABLE_BLOCKED, "设备状态不是 AVAILABLE");
            }
            if (assignmentMapper.selectActiveByDeviceIdForUpdate(device.getId()) != null) {
                throw exception(RENTAL_DEVICE_DISABLE_BLOCKED, "设备存在活动分配");
            }
        }
        try {
            deviceMapper.updateMutableFields(device.getId(), serialNumber, warehouseCode,
                    reqVO.getPurchaseAmount(), reqVO.getEnabled());
        } catch (DuplicateKeyException ex) {
            if (serialNumber != null && isSerialNumberOwnedByOtherDevice(serialNumber, device)) {
                throw exception(RENTAL_DEVICE_SERIAL_DUPLICATE, serialNumber);
            }
            throw ex;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(Long id) {
        RentalDeviceDO device = requireDeviceForUpdate(id);
        deletionGuard.validateDeletable(device);
        deviceMapper.deleteById(id);
    }

    public RentalDeviceAssignmentResult assign(RentalDeviceAssignmentCommand command) {
        try {
            return assignmentService.assign(command);
        } catch (RentalDeviceAssignmentException ex) {
            throw exception(RENTAL_DEVICE_ASSIGN_FAILED, ex.getCode().name());
        }
    }

    public RentalDeviceQrRespVO getDeviceQr(Long id) {
        RentalDeviceDO device = requireDevice(id);
        RentalDeviceQrRespVO vo = new RentalDeviceQrRespVO();
        vo.setDeviceId(device.getId());
        vo.setDeviceNo(device.getDeviceNo());
        vo.setEquipmentModelCode(device.getEquipmentModelCode());
        vo.setPayload(qrCodec.encode(device.getDeviceNo(), device.getEquipmentModelCode()));
        vo.setPayloadVersion(RentalDeviceQrCodec.VERSION);
        vo.setSigned(deviceProperties.isQrSigned());
        return vo;
    }

    public RentalDeviceRespVO resolveDeviceQr(String payload) {
        RentalDeviceQrCodec.ParsedPayload parsed;
        try {
            parsed = qrCodec.decode(payload);
        } catch (IllegalArgumentException ex) {
            throw exception(RENTAL_DEVICE_QR_INVALID, ex.getMessage());
        }
        RentalDeviceDO device = deviceMapper.selectByDeviceNo(parsed.deviceNo());
        if (device == null) {
            device = deviceMapper.selectBySerialNumber(parsed.deviceNo());
        }
        if (device == null) {
            device = deviceMapper.selectByLegacyDeviceNo(parsed.deviceNo());
        }
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        if (!Objects.equals(device.getEquipmentModelCode(), parsed.equipmentModelCode())) {
            throw exception(RENTAL_DEVICE_QR_MODEL_MISMATCH);
        }
        return toVo(device);
    }

    private RentalDeviceDO requireDevice(Long id) {
        RentalDeviceDO device = deviceMapper.selectById(id);
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        return device;
    }

    private RentalDeviceDO requireDeviceForUpdate(Long id) {
        RentalDeviceDO device = deviceMapper.selectByIdForUpdate(id);
        if (device == null) {
            throw exception(RENTAL_DEVICE_NOT_EXISTS);
        }
        return device;
    }

    private void validateSerialNumberUnique(String serialNumber, RentalDeviceDO device) {
        if (serialNumber != null && isSerialNumberOwnedByOtherDevice(serialNumber, device)) {
            throw exception(RENTAL_DEVICE_SERIAL_DUPLICATE, serialNumber);
        }
    }

    private boolean isSerialNumberOwnedByOtherDevice(String serialNumber, RentalDeviceDO device) {
        return deviceMapper.countAllBySerialNumberExcludingId(
                device.getTenantId(), serialNumber, device.getId()) > 0;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private RentalDeviceRespVO toVo(RentalDeviceDO device) {
        RentalDeviceRespVO vo = new RentalDeviceRespVO();
        vo.setId(device.getId());
        vo.setDeviceNo(device.getDeviceNo());
        vo.setSerialNumber(device.getSerialNumber());
        vo.setCategoryCode(device.getCategoryCode());
        vo.setEquipmentModelCode(device.getEquipmentModelCode());
        vo.setStatus(device.getStatus());
        vo.setWarehouseCode(device.getWarehouseCode());
        vo.setPurchaseAmount(device.getPurchaseAmount());
        vo.setEnabled(device.getEnabled());
        return vo;
    }

}
