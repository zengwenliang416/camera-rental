package cn.iocoder.yudao.module.rental.dal.dataobject.xianyu;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Normalized XianGuanJia after-sale summary. Full source JSON stays in xianyu_raw_payload.
 */
@TableName("xianyu_after_sale")
@KeySequence("xianyu_after_sale_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuAfterSaleDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long shopId;
    private String externalAfterSaleId;
    private String externalOrderId;
    private String afterSaleStatus;
    private Long refundAmount;
    private String amountUnitStatus;
    private LocalDateTime timeoutAt;
    private LocalDateTime sourceUpdatedAt;
    private Long rawPayloadId;

}
