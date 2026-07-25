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

@TableName("xianyu_shop")
@KeySequence("xianyu_shop_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XianyuShopDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long applicationId;
    private String externalShopId;
    private String authorizeId;
    private String shopName;
    private String authorizationStatus;
    private LocalDateTime authorizationExpiresAt;
    private String guaranteeStatus;
    private LocalDateTime sourceUpdatedAt;

}
