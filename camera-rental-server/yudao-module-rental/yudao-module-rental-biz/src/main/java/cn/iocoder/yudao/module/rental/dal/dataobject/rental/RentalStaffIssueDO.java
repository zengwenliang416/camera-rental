package cn.iocoder.yudao.module.rental.dal.dataobject.rental;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
@TableName("rental_staff_issue")
@Data
public class RentalStaffIssueDO extends TenantBaseDO {
    @TableId private Long id;
    private String requestKey;
    private Long rentalOrderId;
    private Long deviceId;
    private String title;
    private String note;
    private String status;
    private Long ownerId;
    private Integer revision;
}
