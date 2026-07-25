package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Actionable conversion exception that preserves the original channel order.
 */
@TableName("rental_manual_review")
@KeySequence("rental_manual_review_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalManualReviewDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String reviewType;
    private String sourceType;
    private String sourceIdentifier;
    private String status;
    private String reasonCode;
    private String reasonMessage;
    private String resolutionNote;
    private Long resolvedBy;
    private java.time.LocalDateTime resolvedAt;

}
