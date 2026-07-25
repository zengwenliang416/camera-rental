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
 * Stable source cursor for a single shop and resource type.
 */
@TableName("xianyu_sync_cursor")
@KeySequence("xianyu_sync_cursor_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuSyncCursorDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long shopId;
    private String resourceType;
    private LocalDateTime cursorUpdatedAt;
    private String cursorExternalId;
    private LocalDateTime safeUpperBound;

}
