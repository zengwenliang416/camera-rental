package cn.iocoder.yudao.module.rental.service.rental;

import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualOrderCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualOrderCreateRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalCustomerDO;

/**
 * Manual entry of offline rental orders: orders land as source_type=OFFLINE,
 * PENDING_ALLOCATION + READY so the channel-neutral allocation chain takes over.
 */
public interface RentalManualOrderService {

    RentalManualOrderCreateRespVO createManualOrder(RentalManualOrderCreateReqVO reqVO);

    /**
     * Confirms hand-over for ERRAND/SELF_DELIVERY orders: every fully assigned device is
     * dispatched through the same write path as warehouse scan-out. Never writes
     * rental_delivery or rental_device_shipment.
     */
    void confirmOutbound(Long orderId);

    /**
     * Exact-match lookup by full mobile number; returns null when no customer exists.
     */
    RentalCustomerDO suggestCustomer(String mobile);

}
