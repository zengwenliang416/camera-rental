package cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 闲管家受限原始载荷 Response VO")
@Data
public class XianyuRawPayloadRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "脱敏来源标识")
    private String sourceIdentifier;

    @Schema(description = "载荷 SHA-256")
    private String payloadHash;

    @Schema(description = "结构版本")
    private String schemaVersion;

    @Schema(description = "脱敏版本")
    private String redactionVersion;

    @Schema(description = "接收时间")
    private LocalDateTime receivedAt;

    @Schema(description = "二次脱敏后的载荷，仅详情接口返回")
    private String maskedPayload;

}
