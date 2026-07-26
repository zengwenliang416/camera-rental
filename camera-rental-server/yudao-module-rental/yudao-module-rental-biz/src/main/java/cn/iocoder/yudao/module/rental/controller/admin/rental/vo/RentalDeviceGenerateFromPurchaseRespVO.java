package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 采购入库生成设备结果")
@Data
public class RentalDeviceGenerateFromPurchaseRespVO {

    private Long purchaseInId;
    private String purchaseInNo;
    private int requestedCount;
    private int alreadyExistedCount;
    private int createdCount;
    private List<Long> createdDeviceIds = new ArrayList<>();
    private List<String> createdDeviceNos = new ArrayList<>();

}
