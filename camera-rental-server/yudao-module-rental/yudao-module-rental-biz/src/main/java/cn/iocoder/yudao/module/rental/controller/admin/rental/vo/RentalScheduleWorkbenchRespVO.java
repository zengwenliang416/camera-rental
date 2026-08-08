package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 设备排期工作台读模型")
@Data
public class RentalScheduleWorkbenchRespVO {

    private RentalScheduleWorkbenchWindowRespVO window;
    private RentalScheduleWorkbenchMetricsRespVO metrics;
    private PageResult<RentalScheduleWorkbenchDeviceLaneRespVO> devicePage;
    private List<RentalScheduleWorkbenchPendingAllocationRespVO> pendingAllocations = List.of();
    private List<RentalScheduleWorkbenchExceptionRespVO> exceptions = List.of();
}
