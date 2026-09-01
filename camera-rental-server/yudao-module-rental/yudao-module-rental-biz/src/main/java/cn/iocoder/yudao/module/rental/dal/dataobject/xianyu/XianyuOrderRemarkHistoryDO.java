package cn.iocoder.yudao.module.rental.dal.dataobject.xianyu;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Immutable audit snapshot for one seller-remark parse attempt.
 */
@TableName("xianyu_order_remark_history")
@KeySequence("xianyu_order_remark_history_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuOrderRemarkHistoryDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long xianyuOrderId;
    private Long rawPayloadId;
    private String sellerRemark;
    private String parseVersion;
    private String parseStatus;
    private String parseReasonCode;
    private LocalDate shipDate;
    private LocalDate receiveDate;
    private LocalDate billableStartDate;
    private LocalDate billableEndDate;
    private LocalDate sendBackDate;
    private Boolean effectivePlan;
    private String changeType;
    private LocalDateTime sourceUpdatedAt;

}
