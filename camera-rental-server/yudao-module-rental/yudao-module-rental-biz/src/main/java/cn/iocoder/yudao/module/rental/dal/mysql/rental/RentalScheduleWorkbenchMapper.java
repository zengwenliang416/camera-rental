package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalScheduleWorkbenchMetricsRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface RentalScheduleWorkbenchMapper {

    IPage<RentalDeviceDO> selectDevicePage(
            IPage<RentalDeviceDO> page,
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("equipmentModelCode") String equipmentModelCode,
            @Param("deviceStatus") String deviceStatus,
            @Param("logisticsStatus") String logisticsStatus);

    RentalScheduleWorkbenchMetricsRespVO selectDeviceMetrics(
            @Param("tenantId") Long tenantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDateExclusive") LocalDate toDateExclusive,
            @Param("keyword") String keyword,
            @Param("equipmentModelCode") String equipmentModelCode,
            @Param("deviceStatus") String deviceStatus,
            @Param("logisticsStatus") String logisticsStatus);
}
