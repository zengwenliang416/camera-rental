package cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@TableName("rental_return_registration_attachment")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class RentalReturnRegistrationAttachmentDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long registrationId;
    private Long infraFileId;
    private Long fileConfigId;
    private String category;
    private String objectPath;
    private String objectPathHash;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String contentSha256;
    private Integer sortNo;
    private Boolean confirmed;
}
