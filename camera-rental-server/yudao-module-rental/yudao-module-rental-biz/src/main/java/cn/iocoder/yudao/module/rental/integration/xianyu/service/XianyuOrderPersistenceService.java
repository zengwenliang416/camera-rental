package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;

import java.time.LocalDateTime;

/**
 * Local persistence boundary for successful XianGuanJia order-detail responses.
 */
public interface XianyuOrderPersistenceService {

    XianyuOrderDO persistOrderDetail(Long shopId, String rawPayload);

    boolean advanceOrderCursor(Long shopId, LocalDateTime sourceUpdatedAt, String externalOrderId,
                               LocalDateTime safeUpperBound);

}
