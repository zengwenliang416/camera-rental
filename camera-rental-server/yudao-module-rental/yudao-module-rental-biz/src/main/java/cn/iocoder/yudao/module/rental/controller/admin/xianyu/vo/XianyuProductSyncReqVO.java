package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 有界商品同步请求")
@Data
public class XianyuProductSyncReqVO {

    @NotNull
    private Long shopId;

    @NotNull
    private LocalDateTime windowStart;

    @NotNull
    private LocalDateTime windowEnd;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer pageNo;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer pageSize;

}
