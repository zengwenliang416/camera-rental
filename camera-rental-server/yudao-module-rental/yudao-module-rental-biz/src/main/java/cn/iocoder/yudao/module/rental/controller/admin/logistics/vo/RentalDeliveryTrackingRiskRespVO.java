package cn.iocoder.yudao.module.rental.controller.admin.logistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 稳定物流风险")
@Data
public class RentalDeliveryTrackingRiskRespVO {

    private String code;
    private String severity;
    private String safeMessage;
    private String nextAction;
    private List<Long> deviceIds = new ArrayList<>();
}
