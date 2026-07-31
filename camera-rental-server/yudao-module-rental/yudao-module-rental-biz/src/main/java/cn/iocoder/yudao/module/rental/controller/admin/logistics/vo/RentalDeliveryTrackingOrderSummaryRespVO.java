package cn.iocoder.yudao.module.rental.controller.admin.logistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 订单本地物流摘要")
@Data
public class RentalDeliveryTrackingOrderSummaryRespVO {

    private Long orderId;
    private Long rentalOrderId;
    private Integer packageCount;
    private Map<String, Integer> statusCounts = new LinkedHashMap<>();
    private List<RentalDeliveryTrackingPackageSummaryRespVO> packages = new ArrayList<>();
    private List<RentalDeliveryTrackingRiskRespVO> risks = new ArrayList<>();
}
