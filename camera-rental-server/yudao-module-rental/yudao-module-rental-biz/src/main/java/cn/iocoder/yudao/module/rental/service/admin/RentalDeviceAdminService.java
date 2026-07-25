package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentCommand;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentException;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentResult;
import cn.iocoder.yudao.module.rental.service.RentalDeviceAssignmentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_ASSIGN_FAILED;

@Service
public class RentalDeviceAdminService {

    private final RentalDeviceMapper deviceMapper;
    private final RentalDeviceAssignmentService assignmentService;

    public RentalDeviceAdminService(RentalDeviceMapper deviceMapper,
                                    RentalDeviceAssignmentService assignmentService) {
        this.deviceMapper = deviceMapper;
        this.assignmentService = assignmentService;
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
