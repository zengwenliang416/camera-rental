package cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@TableName("rental_return_registration_device")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class RentalReturnRegistrationDeviceDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long registrationId;
    private Long deviceId;
    private Long assignmentId;
    private String submittedSerial;
    private String normalizedSerial;
    private String matchStatus;
    private String matchMessage;
    private Integer sortNo;
}
