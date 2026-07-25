package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 有界商品同步结果")
@Data
public class XianyuProductSyncRespVO {

    private Long syncRunId;
    private Integer receivedCount;
    private Integer succeededCount;
    private Integer deduplicatedCount;
    private Integer skuCount;

}
