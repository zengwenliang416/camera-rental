package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolves a push to one authorized shop without guessing when seller identities are shared.
 */
@Service
public class XianyuOrderPushShopResolver {

    private final XianyuShopMapper shopMapper;
    private final XianyuOrderMapper orderMapper;

    public XianyuOrderPushShopResolver(XianyuShopMapper shopMapper, XianyuOrderMapper orderMapper) {
        this.shopMapper = shopMapper;
        this.orderMapper = orderMapper;
    }

    public Long resolveShopId(String sellerId, String externalOrderId) {
        List<XianyuShopDO> shops = shopMapper.selectValidListByExternalShopId(sellerId);
        if (shops.size() == 1) {
            return shops.get(0).getId();
        }
        if (shops.size() < 2) {
            return null;
        }
        List<Long> candidateShopIds = shops.stream().map(XianyuShopDO::getId).toList();
        List<XianyuOrderDO> existingOrders =
                orderMapper.selectListByShopIdsAndExternalOrderId(candidateShopIds, externalOrderId);
        return existingOrders.size() == 1 ? existingOrders.get(0).getShopId() : null;
    }

}
