package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAlertPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAlertRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuAlertDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuAlertMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuAlertAdminServiceTest {

    private XianyuAlertMapper alertMapper;
    private XianyuAlertAdminService service;

    @BeforeEach
    void setUp() {
        alertMapper = mock(XianyuAlertMapper.class);
        service = new XianyuAlertAdminService(alertMapper);
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getAlertPageShouldMaskSourceIdentifierAndMessage() {
        XianyuAlertDO alert = XianyuAlertDO.builder()
                .id(1L)
                .shopId(2L)
                .alertType(XianyuAlertAdminService.TYPE_SHOP_AUTH_INVALID)
                .severity(XianyuAlertAdminService.SEVERITY_WARNING)
                .status(XianyuAlertAdminService.STATUS_OPEN)
                .sourceIdentifier("922158952480837")
                .message("授权失效，联系人:张三 手机13800138000 订单922158952480837")
                .firstSeenAt(LocalDateTime.of(2026, 7, 25, 1, 0))
                .lastSeenAt(LocalDateTime.of(2026, 7, 25, 2, 0))
                .build();
        when(alertMapper.selectPage(any(XianyuAlertPageReqVO.class), any(LambdaQueryWrapperX.class)))
                .thenReturn(new PageResult<>(List.of(alert), 1L));

        PageResult<XianyuAlertRespVO> page = service.getAlertPage(new XianyuAlertPageReqVO());
        String json = JsonUtils.toJsonString(page.getList().get(0));

        assertEquals(1L, page.getTotal());
        assertFalse(json.contains("922158952480837"));
        assertFalse(json.contains("13800138000"));
        assertFalse(json.contains("张三"));
    }

    @Test
    void recordShopAuthorizationInvalidShouldUseDedupeKey() {
        service.recordShopAuthorizationInvalid(3L, "auth-1234567890", "invalid");

        ArgumentCaptor<XianyuAlertDO> captor = ArgumentCaptor.forClass(XianyuAlertDO.class);
        verify(alertMapper).insertOrRefresh(eq(9L), captor.capture());
        XianyuAlertDO alert = captor.getValue();
        assertEquals(3L, alert.getShopId());
        assertEquals(XianyuAlertAdminService.TYPE_SHOP_AUTH_INVALID, alert.getAlertType());
        assertEquals("SHOP_AUTH_INVALID:3", alert.getDedupeKey());
        assertEquals(XianyuAlertAdminService.STATUS_OPEN, alert.getStatus());
    }

    @Test
    void recordSyncFailedShouldUseResourceShopAndSafeCodeDedupeKey() {
        service.recordSyncFailed(3L, "ORDER", "PAGE_METADATA_MISMATCH");

        ArgumentCaptor<XianyuAlertDO> captor = ArgumentCaptor.forClass(XianyuAlertDO.class);
        verify(alertMapper).insertOrRefresh(eq(9L), captor.capture());
        XianyuAlertDO alert = captor.getValue();
        assertEquals(3L, alert.getShopId());
        assertEquals(XianyuAlertAdminService.TYPE_SYNC_FAILED, alert.getAlertType());
        assertEquals("SYNC_FAILED:ORDER:3:PAGE_METADATA_MISMATCH", alert.getDedupeKey());
        assertEquals("ORDER", alert.getSourceIdentifier());
        assertEquals("ORDER synchronization failed: PAGE_METADATA_MISMATCH", alert.getMessage());
    }

    @Test
    void recordGuaranteeHealthShouldUseShopScopedDedupeKey() {
        service.recordGuaranteeHealth(3L, "auth-1234567890", "DEPOSIT_INSUFFICIENT");

        ArgumentCaptor<XianyuAlertDO> captor = ArgumentCaptor.forClass(XianyuAlertDO.class);
        verify(alertMapper).insertOrRefresh(eq(9L), captor.capture());
        XianyuAlertDO alert = captor.getValue();
        assertEquals(3L, alert.getShopId());
        assertEquals(XianyuAlertAdminService.TYPE_GUARANTEE_HEALTH, alert.getAlertType());
        assertEquals("GUARANTEE_HEALTH:3", alert.getDedupeKey());
        assertEquals("auth-1234567890", alert.getSourceIdentifier());
        assertEquals("Shop guarantee health requires attention: DEPOSIT_INSUFFICIENT", alert.getMessage());
    }

    @Test
    void recordAfterSaleTimeoutShouldUseAfterSaleScopedDedupeKey() {
        LocalDateTime timeoutAt = LocalDateTime.of(2026, 7, 24, 12, 0);

        service.recordAfterSaleTimeout(3L, "refund-1234567890", timeoutAt);

        ArgumentCaptor<XianyuAlertDO> captor = ArgumentCaptor.forClass(XianyuAlertDO.class);
        verify(alertMapper).insertOrRefresh(eq(9L), captor.capture());
        XianyuAlertDO alert = captor.getValue();
        assertEquals(3L, alert.getShopId());
        assertEquals(XianyuAlertAdminService.TYPE_AFTER_SALE_TIMEOUT, alert.getAlertType());
        assertEquals("AFTER_SALE_TIMEOUT:3:refund-1234567890", alert.getDedupeKey());
        assertEquals("refund-1234567890", alert.getSourceIdentifier());
        assertEquals("After-sale timeout reached at 2026-07-24T12:00", alert.getMessage());
    }

    @Test
    void resolveAlertShouldMarkResolved() {
        XianyuAlertDO alert = XianyuAlertDO.builder()
                .id(5L)
                .status(XianyuAlertAdminService.STATUS_OPEN)
                .build();
        when(alertMapper.selectByTenantIdAndId(9L, 5L)).thenReturn(alert);

        service.resolveAlert(5L, 7L);

        assertEquals(XianyuAlertAdminService.STATUS_RESOLVED, alert.getStatus());
        assertEquals("7", alert.getUpdater());
        verify(alertMapper).updateById(alert);
    }

    @Test
    void resolveAlertShouldRejectCrossTenantId() {
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.resolveAlert(6L, 7L));

        verify(alertMapper).selectByTenantIdAndId(9L, 6L);
    }

}
