package cn.iocoder.yudao.module.rental.service.configuration;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalConfigurationShopRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class RentalConfigurationShopService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final XianyuShopMapper shopMapper;

    public RentalConfigurationShopService(XianyuShopMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    public List<RentalConfigurationShopRespVO> getAvailableShops() {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        return shopMapper.selectConfigurationShops(TenantContextHolder.getRequiredTenantId())
                .stream()
                .filter(shop -> "VALID".equals(shop.getAuthorizationStatus()))
                .filter(shop -> shop.getAuthorizationExpiresAt() == null
                        || shop.getAuthorizationExpiresAt().isAfter(now))
                .map(RentalConfigurationShopService::toResponse)
                .toList();
    }

    private static RentalConfigurationShopRespVO toResponse(XianyuShopDO shop) {
        RentalConfigurationShopRespVO response = new RentalConfigurationShopRespVO();
        response.setId(shop.getId());
        response.setShopName(shop.getShopName());
        response.setAuthorizationStatus(shop.getAuthorizationStatus());
        return response;
    }

}
