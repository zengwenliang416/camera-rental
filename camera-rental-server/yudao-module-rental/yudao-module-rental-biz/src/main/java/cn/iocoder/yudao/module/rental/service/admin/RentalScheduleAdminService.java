package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalSchedulePageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalScheduleDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalScheduleMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RentalScheduleAdminService {

    private final RentalScheduleMapper scheduleMapper;
    private final RentalDeviceMapper deviceMapper;
    private final RentalOrderItemMapper orderItemMapper;

    public RentalScheduleAdminService(RentalScheduleMapper scheduleMapper,
                                      RentalDeviceMapper deviceMapper,
                                      RentalOrderItemMapper orderItemMapper) {
        this.scheduleMapper = scheduleMapper;
        this.deviceMapper = deviceMapper;
        this.orderItemMapper = orderItemMapper;
    }

    public PageResult<RentalScheduleRespVO> getSchedulePage(RentalSchedulePageReqVO pageReqVO) {
        PageResult<RentalScheduleDO> page = scheduleMapper.selectPage(pageReqVO,
                new LambdaQueryWrapperX<RentalScheduleDO>()
                        .eqIfPresent(RentalScheduleDO::getDeviceId, pageReqVO.getDeviceId())
                        .eqIfPresent(RentalScheduleDO::getRentalOrderId, pageReqVO.getRentalOrderId())
                        .eqIfPresent(RentalScheduleDO::getStatus, pageReqVO.getStatus())
                        .gtIfPresent(RentalScheduleDO::getOccupyEndDateExclusive,
                                pageReqVO.getOccupyStartDate())
                        .ltIfPresent(RentalScheduleDO::getOccupyStartDate,
                                pageReqVO.getOccupyEndDateExclusive())
                        .orderByDesc(RentalScheduleDO::getOccupyStartDate)
                        .orderByDesc(RentalScheduleDO::getId));
        Map<Long, RentalDeviceDO> deviceMap = getDeviceMap(page.getList());
        Map<Long, RentalOrderItemDO> orderItemMap = getOrderItemMap(page.getList());
        List<RentalScheduleRespVO> list = page.getList().stream()
                .map(schedule -> toVo(schedule, deviceMap, orderItemMap))
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    private Map<Long, RentalDeviceDO> getDeviceMap(List<RentalScheduleDO> schedules) {
        Set<Long> ids = schedules.stream().map(RentalScheduleDO::getDeviceId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Map.of() : deviceMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(RentalDeviceDO::getId, Function.identity()));
    }

    private Map<Long, RentalOrderItemDO> getOrderItemMap(List<RentalScheduleDO> schedules) {
        Set<Long> ids = schedules.stream().map(RentalScheduleDO::getRentalOrderItemId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Map.of() : orderItemMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(RentalOrderItemDO::getId, Function.identity()));
    }

    private RentalScheduleRespVO toVo(RentalScheduleDO schedule,
                                      Map<Long, RentalDeviceDO> deviceMap,
                                      Map<Long, RentalOrderItemDO> orderItemMap) {
        RentalScheduleRespVO vo = new RentalScheduleRespVO();
        vo.setId(schedule.getId());
        vo.setDeviceId(schedule.getDeviceId());
        RentalDeviceDO device = deviceMap.get(schedule.getDeviceId());
        if (device != null) {
            vo.setDeviceNo(device.getDeviceNo());
            vo.setEquipmentModelCode(device.getEquipmentModelCode());
        }
        vo.setRentalOrderId(schedule.getRentalOrderId());
        vo.setRentalOrderItemId(schedule.getRentalOrderItemId());
        vo.setScheduleType(schedule.getScheduleType());
        vo.setStatus(schedule.getStatus());
        RentalOrderItemDO orderItem = orderItemMap.get(schedule.getRentalOrderItemId());
        if (orderItem != null) {
            vo.setBillableStartDate(orderItem.getBillableStartDate());
            vo.setBillableEndDate(orderItem.getBillableEndDate());
        }
        vo.setOccupyStartDate(schedule.getOccupyStartDate());
        vo.setOccupyEndDateExclusive(schedule.getOccupyEndDateExclusive());
        return vo;
    }

}
