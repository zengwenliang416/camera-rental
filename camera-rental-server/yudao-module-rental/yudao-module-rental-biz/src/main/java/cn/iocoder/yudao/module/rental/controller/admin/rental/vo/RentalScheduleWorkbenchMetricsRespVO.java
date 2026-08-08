package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 设备排期工作台指标")
@Data
public class RentalScheduleWorkbenchMetricsRespVO {

    private Long totalDevices = 0L;
    private Long availableDevices = 0L;
    private Long occupiedDevices = 0L;
    private Long inTransitDevices = 0L;
    private Long pendingAllocationCount = 0L;
    private Long pendingAllocationOrders = 0L;
    private Long pendingAllocationItems = 0L;
    private Long exceptionCount = 0L;
    private Long occupiedDeviceDays = 0L;
    private Long totalDeviceDays = 0L;
    private BigDecimal utilizationRate = BigDecimal.ZERO;
}
