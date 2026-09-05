package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 设备分页查询")
@Data
@EqualsAndHashCode(callSuper = true)
public class RentalDevicePageReqVO extends PageParam {

    private String categoryCode;
    private String equipmentModelCode;

    @Schema(description = "设备编号或序列号关键词")
    @Size(max = 100)
    private String keyword;

    private Boolean enabled;
}
