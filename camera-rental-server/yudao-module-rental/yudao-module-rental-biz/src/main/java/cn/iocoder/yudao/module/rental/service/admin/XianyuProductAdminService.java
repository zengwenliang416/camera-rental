package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuProductSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuProductSyncRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductSyncWindow;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_PRODUCT_SYNC_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_NOT_EXISTS;

@Service
public class XianyuProductAdminService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final XianyuShopMapper shopMapper;
    private final XianyuProductSyncService productSyncService;
    private final Clock clock;

    public XianyuProductAdminService(XianyuShopMapper shopMapper,
                                     XianyuProductSyncService productSyncService,
                                     @Qualifier("xianyuClock") Clock clock) {
        this.shopMapper = shopMapper;
        this.productSyncService = productSyncService;
        this.clock = clock;
    }

    public XianyuProductSyncRespVO syncPage(XianyuProductSyncReqVO reqVO) {
        validateTimeRange(reqVO);
        XianyuShopDO shop = validateShop(reqVO.getShopId());
        try {
            XianyuProductSyncWindow window = new XianyuProductSyncWindow(
                    reqVO.getWindowStart(), reqVO.getWindowEnd(), reqVO.getPageNo(), reqVO.getPageSize());
            XianyuProductPageSyncResult result = productSyncService.syncPage(
                    shop.getId(), null, window, XianyuProductSyncService.TRIGGER_MANUAL);
            XianyuProductSyncRespVO resp = new XianyuProductSyncRespVO();
            resp.setSyncRunId(result.syncRunId());
            resp.setReceivedCount(result.receivedCount());
            resp.setSucceededCount(result.succeededCount());
            resp.setDeduplicatedCount(result.deduplicatedCount());
            resp.setSkuCount(result.skuCount());
            return resp;
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw exception(XIANYU_PRODUCT_SYNC_FAILED, XianyuSafeErrorCode.from(ex));
        }
    }

    private void validateTimeRange(XianyuProductSyncReqVO reqVO) {
        if (reqVO.getWindowStart() == null || reqVO.getWindowEnd() == null
                || !reqVO.getWindowStart().isBefore(reqVO.getWindowEnd())) {
            throw exception(XIANYU_PRODUCT_SYNC_FAILED, "INVALID_TIME_RANGE");
        }
    }

    private XianyuShopDO validateShop(Long shopId) {
        XianyuShopDO shop = shopMapper.selectByTenantIdAndId(TenantContextHolder.getRequiredTenantId(), shopId);
        if (shop == null) {
            throw exception(XIANYU_SHOP_NOT_EXISTS);
        }
        if (!"VALID".equals(shop.getAuthorizationStatus())
                || shop.getAuthorizationExpiresAt() != null
                && !shop.getAuthorizationExpiresAt().isAfter(LocalDateTime.now(clock.withZone(BUSINESS_ZONE)))) {
            throw exception(XIANYU_SHOP_AUTHORIZATION_INVALID);
        }
        return shop;
    }

}
