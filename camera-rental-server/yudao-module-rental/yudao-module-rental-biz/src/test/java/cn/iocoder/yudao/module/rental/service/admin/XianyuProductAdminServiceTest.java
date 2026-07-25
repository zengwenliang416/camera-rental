package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuProductSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuProductSyncRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductSyncWindow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_PRODUCT_SYNC_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuProductAdminServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T04:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void manualSyncShouldUseExistingProductSyncServiceWithoutSellerId() {
        XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
        XianyuProductSyncService syncService = mock(XianyuProductSyncService.class);
        when(shopMapper.selectByTenantIdAndId(9L, 7L)).thenReturn(validShop());
        when(syncService.syncPage(eq(7L), org.mockito.Mockito.isNull(), org.mockito.Mockito.any(XianyuProductSyncWindow.class),
                eq(XianyuProductSyncService.TRIGGER_MANUAL)))
                .thenReturn(new XianyuProductPageSyncResult(99L, 2, 2, 1, 3));
        XianyuProductAdminService service = new XianyuProductAdminService(shopMapper, syncService, CLOCK);

        XianyuProductSyncRespVO resp = service.syncPage(req());

        assertEquals(99L, resp.getSyncRunId());
        assertEquals(2, resp.getReceivedCount());
        assertEquals(2, resp.getSucceededCount());
        assertEquals(1, resp.getDeduplicatedCount());
        assertEquals(3, resp.getSkuCount());
        ArgumentCaptor<XianyuProductSyncWindow> windowCaptor = ArgumentCaptor.forClass(XianyuProductSyncWindow.class);
        verify(syncService).syncPage(eq(7L), org.mockito.Mockito.isNull(), windowCaptor.capture(),
                eq(XianyuProductSyncService.TRIGGER_MANUAL));
        assertEquals(LocalDateTime.of(2026, 7, 23, 0, 0), windowCaptor.getValue().start());
        assertEquals(1, windowCaptor.getValue().pageNo());
    }

    @Test
    void manualSyncShouldRejectExpiredShopBeforeRemoteAccess() {
        XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
        XianyuProductSyncService syncService = mock(XianyuProductSyncService.class);
        when(shopMapper.selectByTenantIdAndId(9L, 7L)).thenReturn(XianyuShopDO.builder()
                .id(7L)
                .externalShopId("123456")
                .authorizationStatus("VALID")
                .authorizationExpiresAt(LocalDateTime.of(2026, 7, 23, 23, 59))
                .build());
        XianyuProductAdminService service = new XianyuProductAdminService(shopMapper, syncService, CLOCK);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.syncPage(req()));

        assertEquals(XIANYU_SHOP_AUTHORIZATION_INVALID.getCode(), exception.getCode());
        verify(syncService, never()).syncPage(org.mockito.Mockito.any(), org.mockito.Mockito.any(),
                org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void manualSyncShouldMapRuntimeFailureToSafeErrorCode() {
        XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
        XianyuProductSyncService syncService = mock(XianyuProductSyncService.class);
        when(shopMapper.selectByTenantIdAndId(9L, 7L)).thenReturn(validShop());
        when(syncService.syncPage(eq(7L), org.mockito.Mockito.isNull(), org.mockito.Mockito.any(XianyuProductSyncWindow.class),
                eq(XianyuProductSyncService.TRIGGER_MANUAL)))
                .thenThrow(new IllegalStateException("secret transport detail"));
        XianyuProductAdminService service = new XianyuProductAdminService(shopMapper, syncService, CLOCK);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.syncPage(req()));

        assertEquals(XIANYU_PRODUCT_SYNC_FAILED.getCode(), exception.getCode());
        assertEquals("闲鱼商品同步失败：IllegalStateException", exception.getMessage());
    }

    private XianyuShopDO validShop() {
        return XianyuShopDO.builder()
                .id(7L)
                .externalShopId("123456")
                .authorizationStatus("VALID")
                .authorizationExpiresAt(LocalDateTime.of(2026, 8, 1, 0, 0))
                .build();
    }

    private XianyuProductSyncReqVO req() {
        XianyuProductSyncReqVO reqVO = new XianyuProductSyncReqVO();
        reqVO.setShopId(7L);
        reqVO.setWindowStart(LocalDateTime.of(2026, 7, 23, 0, 0));
        reqVO.setWindowEnd(LocalDateTime.of(2026, 7, 24, 0, 0));
        reqVO.setPageNo(1);
        reqVO.setPageSize(50);
        return reqVO;
    }

}
