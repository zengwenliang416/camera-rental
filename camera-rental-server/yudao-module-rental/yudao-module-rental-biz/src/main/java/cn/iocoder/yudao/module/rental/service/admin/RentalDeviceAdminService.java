package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceQrRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentCommand;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentResult;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceQrCodec;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_ASSIGN_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_QR_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_QR_MODEL_MISMATCH;

@Service
public class RentalDeviceAdminService {

    private final RentalDeviceMapper deviceMapper;
    private final RentalDeviceAssignmentService assignmentService;
    private final RentalDeviceQrCodec qrCodec;
    private final RentalDeviceProperties deviceProperties;

    public RentalDeviceAdminService(RentalDeviceMapper deviceMapper,
                                    RentalDeviceAssignmentService assignmentService,
                                    RentalDeviceQrCodec qrCodec,
                                    RentalDeviceProperties deviceProperties) {
        this.deviceMapper = deviceMapper;
        this.assignmentService = assignmentService;
        this.qrCodec = qrCodec;
        this.deviceProperties = deviceProperties;
    }

    public PageResult<RentalDeviceRespVO> getDevicePage(String equipmentModelCode, PageParam pageParam) {
        PageResult<RentalDeviceDO> page = deviceMapper.selectPage(pageParam,
                new LambdaQueryWrapperX<RentalDeviceDO>()
                        .eqIfPresent(RentalDeviceDO::getEquipmentModelCode, equipmentModelCode)
                        .orderByDesc(RentalDeviceDO::getId));
        List<RentalDeviceRespVO> list = page.getList().stream().map(this::toVo).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    public Long createDevice(RentalDeviceCreateReqVO reqVO) {
        RentalDeviceDO device = RentalDeviceDO.builder()
                .deviceNo(reqVO.getDeviceNo())
                .serialNumber(reqVO.getSerialNumber())
                .equipmentModelCode(reqVO.getEquipmentModelCode())
                .status(reqVO.getStatus() == null ? "AVAILABLE" : reqVO.getStatus())
                .warehouseCode(reqVO.getWarehouseCode())
                .purchaseAmount(reqVO.getPurchaseAmount())
                .enabled(reqVO.getEnabled() == null || reqVO.getEnabled())
                .build();
        deviceMapper.insert(device);
        return device.getId();
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

    private RentalDeviceRespVO toVo(RentalDeviceDO device) {
        RentalDeviceRespVO vo = new RentalDeviceRespVO();
        vo.setId(device.getId());
        vo.setDeviceNo(device.getDeviceNo());
        vo.setSerialNumber(device.getSerialNumber());
        vo.setEquipmentModelCode(device.getEquipmentModelCode());
        vo.setStatus(device.getStatus());
        vo.setWarehouseCode(device.getWarehouseCode());
        vo.setPurchaseAmount(device.getPurchaseAmount());
        vo.setEnabled(device.getEnabled());
        return vo;
    }

}
