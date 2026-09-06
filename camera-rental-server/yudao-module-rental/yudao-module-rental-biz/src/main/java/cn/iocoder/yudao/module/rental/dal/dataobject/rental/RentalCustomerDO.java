package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Offline customer master record; mobile is encrypted and only exact-match lookup is supported.
 */
@TableName(value = "rental_customer", autoResultMap = true)
@KeySequence("rental_customer_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalCustomerDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    @TableField(typeHandler = EncryptTypeHandler.class)
    @ToString.Exclude
    private String mobile;
    private String wechatId;
    private String remark;

}
