package cn.iocoder.yudao.module.rental.dal.dataobject.rental;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
@TableName("rental_shipment_attempt")
@Data
public class RentalShipmentAttemptDO extends TenantBaseDO {
    @TableId private Long id;
    private Long channelOrderId;
    private String idempotencyKey;
    private String requestHash;
    private String status;
}
