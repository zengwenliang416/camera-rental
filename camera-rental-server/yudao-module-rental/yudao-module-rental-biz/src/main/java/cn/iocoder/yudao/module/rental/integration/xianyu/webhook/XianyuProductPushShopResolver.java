package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class XianyuProductPushShopResolver {

    private final XianyuShopMapper shopMapper;
    private final XianyuProductMapper productMapper;

    public XianyuProductPushShopResolver(XianyuShopMapper shopMapper, XianyuProductMapper productMapper) {
        this.shopMapper = shopMapper;
        this.productMapper = productMapper;
    }

    public Long resolveShopId(String sellerId, String xgjProductId) {
        if (StringUtils.hasText(sellerId)) {
            List<XianyuShopDO> shops = shopMapper.selectValidListByExternalShopId(sellerId);
            if (shops.size() == 1) {
                return shops.get(0).getId();
            }
        }
        if (!StringUtils.hasText(xgjProductId)) {
            return null;
        }
        List<Long> shopIds = productMapper.selectListByXgjProductId(xgjProductId)
                .stream()
                .map(XianyuProductDO::getShopId)
                .filter(shopId -> shopId != null)
                .distinct()
                .toList();
        if (shopIds.size() == 1) {
            XianyuShopDO shop = shopMapper.selectByTenantIdAndId(
                    TenantContextHolder.getRequiredTenantId(), shopIds.get(0));
            if (shop != null && "VALID".equals(shop.getAuthorizationStatus())) {
                return shop.getId();
            }
        }
        return null;
    }

}
