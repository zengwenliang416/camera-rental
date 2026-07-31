package cn.iocoder.yudao.module.rental.dal.dataobject.rental;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shipment evidence for one device-order dispatch through a channel write call.
 */
@TableName("rental_device_shipment")
@KeySequence("rental_device_shipment_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDeviceShipmentDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long channelOrderId;
    private Long assignmentId;
    private Long deviceId;
    private Long deliveryId;
    private String idempotencyKey;
    private String waybillNo;
    private String expressCode;
    private String expressName;
    private String shipRequestHash;
    private Integer shipResponseCode;
    private String shipResponseMsg;
    private Boolean ocrConfirmed;
    private String source;

}
