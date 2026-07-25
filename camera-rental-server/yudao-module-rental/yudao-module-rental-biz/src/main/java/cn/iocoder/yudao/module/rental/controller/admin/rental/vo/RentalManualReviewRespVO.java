package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 人工复核")
@Data
public class RentalManualReviewRespVO {

    private Long id;
    private String reviewType;
    private String sourceType;
    private String sourceIdentifier;
    private String status;
    private String reasonCode;
    private String reasonMessage;
    private String resolutionNote;
    private Long resolvedBy;
    private String resolvedByName;
    private LocalDateTime resolvedAt;

}
