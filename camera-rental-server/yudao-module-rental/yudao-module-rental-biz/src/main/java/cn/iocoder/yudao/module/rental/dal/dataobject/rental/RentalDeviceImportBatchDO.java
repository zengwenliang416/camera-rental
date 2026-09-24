package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rental_device_import_batch")
public class RentalDeviceImportBatchDO extends TenantBaseDO {
    @TableId(type = IdType.INPUT) private String id;
    private Long userId;
    private String requestJson;
    private String previewJson;
    private String resultJson;
    private LocalDateTime expiresAt;
}
